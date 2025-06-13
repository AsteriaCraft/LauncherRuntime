package pro.gravit.launcher.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.client.api.DialogService;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.core.backend.UserSettings;
import pro.gravit.launcher.gui.commands.RuntimeCommand;
import pro.gravit.launcher.gui.commands.VersionCommand;
import pro.gravit.launcher.gui.config.GuiModuleConfig;
import pro.gravit.launcher.gui.config.RuntimeSettings;
import pro.gravit.launcher.gui.helper.EnFSHelper;
import pro.gravit.launcher.gui.impl.FXMLFactory;
import pro.gravit.launcher.gui.impl.GuiObjectsContainer;
import pro.gravit.launcher.gui.impl.MessageManager;
import pro.gravit.launcher.gui.scenes.AbstractScene;
import pro.gravit.launcher.gui.service.*;
import pro.gravit.launcher.gui.stage.PrimaryStage;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.runtime.client.DirBridge;
import pro.gravit.launcher.runtime.client.events.ClientGuiPhase;
import pro.gravit.launcher.runtime.debug.DebugMain;
import pro.gravit.launcher.runtime.managers.ConsoleManager;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.CommandCategory;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class JavaFXApplication extends Application {
    private static final AtomicReference<JavaFXApplication> INSTANCE = new AtomicReference<>();
    private static Path runtimeDirectory = null;
    public final LauncherConfig config = Launcher.getConfig();
    public final ExecutorService workers = Executors.newWorkStealingPool(4);
    public RuntimeSettings runtimeSettings;
    public GuiObjectsContainer gui;
    public AuthService authService;
    public ProfileService profileService;
    public GuiModuleConfig guiModuleConfig;
    public MessageManager messageManager;
    public SkinManager skinManager;
    public FXMLFactory fxmlFactory;
    public PingService pingService;
    public BackendCallbackService backendCallbackService;
    private PrimaryStage mainStage;
    private boolean debugMode;
    private ResourceBundle resources;
    private static Path enfsDirectory;

    public JavaFXApplication() {
        INSTANCE.set(this);
    }

    public static JavaFXApplication getInstance() {
        return INSTANCE.get();
    }

    public AbstractScene getCurrentScene() {
        return (AbstractScene) mainStage.getVisualComponent();
    }

    public PrimaryStage getMainStage() {
        return mainStage;
    }

    @Override
    public void init() throws Exception {
        UserSettings.providers.register(JavaRuntimeModule.RUNTIME_NAME, RuntimeSettings.class);
        backendCallbackService = new BackendCallbackService();
        backendCallbackService.initDataCallback = LauncherBackendAPIHolder.getApi().init();
        guiModuleConfig = new GuiModuleConfig();
        runtimeSettings = (RuntimeSettings) LauncherBackendAPIHolder.getApi().getUserSettings("stdruntime", (a) -> RuntimeSettings.getDefault(guiModuleConfig));
        runtimeSettings.apply();
        System.setProperty("prism.vsync", String.valueOf(runtimeSettings.globalSettings.prismVSync));
        DirBridge.dirUpdates = runtimeSettings.updatesDir == null
                ? DirBridge.defaultUpdatesDir
                : runtimeSettings.updatesDir;
        authService = new AuthService(this);
        profileService = new ProfileService(this);
        messageManager = new MessageManager(this);
        skinManager = new SkinManager(this);
        pingService = new PingService();
        LauncherBackendAPIHolder.getApi().setCallback(backendCallbackService);
        registerCommands();
    }

    @Override
    public void start(Stage stage) {
        // If debugging
        try {
            Class.forName("pro.gravit.launcher.runtime.debug.DebugMain", false, JavaFXApplication.class.getClassLoader());
            if (DebugMain.IS_DEBUG.get()) {
                runtimeDirectory = IOHelper.WORKING_DIR.resolve("runtime");
                debugMode = true;
            }
        } catch (Throwable e) {
            if (!(e instanceof ClassNotFoundException) && !(e instanceof NoClassDefFoundError)) {
                LogHelper.error(e);
            }
        }
        try {
            Class.forName("pro.gravit.utils.enfs.EnFS", false, JavaFXApplication.class.getClassLoader());
            EnFSHelper.initEnFS();
            String themeDir = runtimeSettings.theme == null ? RuntimeSettings.LAUNCHER_THEME.COMMON.name :
                    runtimeSettings.theme.name;
            enfsDirectory = EnFSHelper.initEnFSDirectory(config, themeDir, runtimeDirectory);
        } catch (Throwable e) {
            if (!(e instanceof ClassNotFoundException)) {
                LogHelper.error(e);
            }
            if(config.runtimeEncryptKey != null) {
                JavaRuntimeModule.noEnFSAlert();
            }
        }
        // System loading
        if (runtimeSettings.locale == null) runtimeSettings.locale = RuntimeSettings.DEFAULT_LOCALE;
        try {
            updateLocaleResources(runtimeSettings.locale.name);
        } catch (Throwable e) {
            JavaRuntimeModule.noLocaleAlert(runtimeSettings.locale.name);
            if (!(e instanceof FileNotFoundException)) {
                LogHelper.error(e);
            }
            Platform.exit();
        }
        {
            RuntimeDialogService dialogService = new RuntimeDialogService(messageManager);
            DialogService.setDialogImpl(dialogService);
            DialogService.setNotificationImpl(dialogService);
        }
        /*if (offlineService.isOfflineMode()) {
            if (!offlineService.isAvailableOfflineMode() && !debugMode) {
                messageManager.showDialog(getTranslation("runtime.offline.dialog.header"),
                                          getTranslation("runtime.offline.dialog.text"),
                                          Platform::exit, Platform::exit, false);
                return;
            }
        }*/
        try {
            mainStage = new PrimaryStage(this, stage, "%s Launcher".formatted(config.projectName));
            // Overlay loading
            gui = new GuiObjectsContainer(this);
            gui.init();
            //
            mainStage.setScene(gui.loginScene, true);
            gui.background.init();
            mainStage.pushBackground(gui.background);
            mainStage.show();
            /*if (offlineService.isOfflineMode()) {
                messageManager.createNotification(getTranslation("runtime.offline.notification.header"),
                                                  getTranslation("runtime.offline.notification.text"));
            }*/
            //
            LauncherEngine.modulesManager.invokeEvent(new ClientGuiPhase(StdJavaRuntimeProvider.getInstance()));
            AuthRequest.registerProviders();
        } catch (Throwable e) {
            LogHelper.error(e);
            JavaRuntimeModule.errorHandleAlert(e);
            Platform.exit();
        }
    }

    public void updateLocaleResources(String locale) throws IOException {
        try (InputStream input = getResource("runtime_%s.properties".formatted(locale))) {
            resources = new PropertyResourceBundle(input);
        }
        fxmlFactory = new FXMLFactory(resources, workers);
    }

    public void resetDirectory() throws IOException {
        if (enfsDirectory != null) {
            String themeDir = runtimeSettings.theme == null ? RuntimeSettings.LAUNCHER_THEME.COMMON.name :
                    runtimeSettings.theme.name;
            enfsDirectory = EnFSHelper.initEnFSDirectory(config, themeDir, runtimeDirectory);
        }
    }

    private CommandCategory runtimeCategory;

    private void registerCommands() {
        runtimeCategory = new BaseCommandCategory();
        runtimeCategory.registerCommand("version", new VersionCommand());
        if (ConsoleManager.isConsoleUnlock) {
            registerPrivateCommands();
        }
        ConsoleManager.handler.registerCategory(new CommandHandler.Category(runtimeCategory, "runtime"));
    }

    public void registerPrivateCommands() {
        if (runtimeCategory == null) return;
        runtimeCategory.registerCommand("runtime", new RuntimeCommand(this));
    }

    public boolean isThemeSupport() {
        return enfsDirectory != null;
    }


    @Override
    public void stop() {
        LogHelper.debug("JavaFX method stop invoked");
        LauncherEngine.modulesManager.invokeEvent(new ClientExitPhase(0));
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    private InputStream getResource(String name) throws IOException {
        return IOHelper.newInput(getResourceURL(name));
    }

    public static URL getResourceURL(String name) throws IOException {
        if (enfsDirectory != null) {
            return getResourceEnFs(name);
        } else if (runtimeDirectory != null) {
            Path target = runtimeDirectory.resolve(name);
            if (!Files.exists(target)) throw new FileNotFoundException("File runtime/%s not found".formatted(name));
            return target.toUri().toURL();
        } else  {
            return Launcher.getResourceURL(name);
        }
    }

    private static URL getResourceEnFs(String name) throws IOException {
        return EnFSHelper.getURL(enfsDirectory.resolve(name).toString().replaceAll("\\\\", "/"));
        //return EnFS.main.getURL(enfsDirectory.resolve(name));
    }

    public URL tryResource(String name) {
        try {
            return getResourceURL(name);
        } catch (IOException e) {
            return null;
        }

    }

    public void setMainScene(AbstractScene scene) throws Exception {
        mainStage.setScene(scene, true);
    }

    public Stage newStage() {
        return newStage(StageStyle.TRANSPARENT);
    }

    public Stage newStage(StageStyle style) {
        Stage ret = new Stage();
        ret.initStyle(style);
        ret.setResizable(false);
        return ret;
    }

    public final String getTranslation(String name) {
        return getTranslation(name, "'%s'".formatted(name));
    }

    public final String getTranslation(String key, String defaultValue) {
        try {
            return resources.getString(key);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public boolean openURL(String url) {
        try {
            getHostServices().showDocument(url);
            return true;
        } catch (Throwable e) {
            LogHelper.error(e);
            return false;
        }
    }
}