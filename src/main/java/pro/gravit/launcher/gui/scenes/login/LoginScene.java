package pro.gravit.launcher.gui.scenes.login;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.model.Texture;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.gui.StdJavaRuntimeProvider;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.gui.scenes.AbstractScene;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.runtime.utils.LauncherUpdater;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.WebSocketEvent;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.base.request.auth.details.AuthPasswordDetails;
import pro.gravit.launcher.base.request.auth.password.*;
import pro.gravit.launcher.base.request.update.LauncherRequest;
import pro.gravit.launcher.base.request.update.ProfilesRequest;
import pro.gravit.utils.helper.LogHelper;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LoginScene extends AbstractScene {
    private List<AuthMethod> auth; //TODO: FIX? Field is assigned but never accessed.
    private CheckBox savePasswordCheckBox;
    private CheckBox autoenter;
    private Pane content;
    private AbstractVisualComponent contentComponent;
    private LoginAuthButtonComponent authButton;
    private ComboBox<AuthMethod> authList;
    private AuthMethod authAvailability;
    private final AuthFlow authFlow;

    public LoginScene(JavaFXApplication application) {
        super("scenes/login/login.fxml", application);
        LoginSceneAccessor accessor = new LoginSceneAccessor();
        this.authFlow = new AuthFlow(accessor, this::onSuccessLogin);
    }

    @Override
    public void doInit() {
        LookupHelper.<ButtonBase>lookup(header, "#controls", "#settings").setOnAction((e) -> {
            try {
                switchScene(application.gui.globalSettingsScene);
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        authButton = new LoginAuthButtonComponent(LookupHelper.lookup(layout, "#authButton"), application,
                                                  (e) -> contextHelper.runCallback(authFlow::loginWithGui));
        savePasswordCheckBox = LookupHelper.lookup(layout, "#savePassword");
        autoenter = LookupHelper.lookup(layout, "#autoenter");
        autoenter.setSelected(application.runtimeSettings.autoAuth);
        autoenter.setOnAction((event) -> application.runtimeSettings.autoAuth = autoenter.isSelected());
        content = LookupHelper.lookup(layout, "#content");
        if (application.guiModuleConfig.createAccountURL != null) {
            LookupHelper.<Text>lookup(header, "#createAccount")
                        .setOnMouseClicked((e) -> application.openURL(application.guiModuleConfig.createAccountURL));
        }

        if (application.guiModuleConfig.forgotPassURL != null) {
            LookupHelper.<Text>lookup(header, "#forgotPass")
                        .setOnMouseClicked((e) -> application.openURL(application.guiModuleConfig.forgotPassURL));
        }
        authList = LookupHelper.lookup(layout, "#authList");
        authList.setConverter(new AuthAvailabilityStringConverter());
        authList.setOnAction((e) -> changeAuthAvailability(authList.getSelectionModel().getSelectedItem()));
        authFlow.prepare();
        // Verify Launcher
    }

    @Override
    protected void doPostInit() {
        getAvailabilityAuth();
    }

//    private void launcherRequest() {
//        processRequest(application.getTranslation("runtime.overlay.processing.text.launcher"), LauncherAPIHolder.core().checkUpdates(),
//                       (result) -> {
//                           if (result.required()) {
//                               try {
//                                   LogHelper.debug("Start update processing");
//                                   disable();
//                                   StdJavaRuntimeProvider.updatePath = LauncherUpdater.prepareUpdate(
//                                           new URI(result.url()).toURL());
//                                   LogHelper.debug("Exit with Platform.exit");
//                                   Platform.exit();
//                                   return;
//                               } catch (Throwable e) {
//                                   contextHelper.runInFxThread(() -> errorHandle(e));
//                                   try {
//                                       Thread.sleep(1500);
//                                       LauncherEngine.modulesManager.invokeEvent(new ClientExitPhase(0));
//                                       Platform.exit();
//                                   } catch (Throwable ex) {
//                                       LauncherEngine.exitLauncher(0);
//                                   }
//                               }
//                           }
//                           LogHelper.dev("Launcher update processed");
//                           getAvailabilityAuth();
//                       }, (event) -> LauncherEngine.exitLauncher(0));
//    }

    private void getAvailabilityAuth() {
        processing(LauncherBackendAPIHolder.getApi().init(),
                   application.getTranslation("runtime.overlay.processing.text.launcher"),
                   (initData) -> contextHelper.runInFxThread(() -> {
                       this.auth = initData.methods();
                       authList.setVisible(auth.size() != 1);
                       authList.setManaged(auth.size() != 1);
                       for (var authAvailability : auth) {
                           if (!authAvailability.isVisible()) {
                               continue;
                           }
                           if (application.runtimeSettings.lastAuth == null) {
                               if (authAvailability.getName().equals("std") || this.authAvailability == null) {
                                   changeAuthAvailability(authAvailability);
                               }
                           } else if (authAvailability.getName().equals(application.runtimeSettings.lastAuth.getName()))
                               changeAuthAvailability(authAvailability);
                           if(authAvailability.isVisible()) {
                               addAuthAvailability(authAvailability);
                           }
                       }
                       if (this.authAvailability == null && !auth.isEmpty()) {
                           changeAuthAvailability(auth.get(0));
                       }
                       runAutoAuth();
                   }), null);
    }

    private void runAutoAuth() {
        if (application.guiModuleConfig.autoAuth || application.runtimeSettings.autoAuth) {
            contextHelper.runInFxThread(authFlow::loginWithGui);
        }
    }

    public void changeAuthAvailability(AuthMethod authAvailability) {
        boolean isChanged = this.authAvailability != authAvailability; //TODO: FIX
        LauncherBackendAPIHolder.getApi().selectAuthMethod(authAvailability);
        this.authAvailability = authAvailability;
        this.application.authService.setAuthAvailability(authAvailability);
        this.authList.selectionModelProperty().get().select(authAvailability);
        authFlow.init(authAvailability);
        LogHelper.info("Selected auth: %s", authAvailability.getName());
    }

    public void addAuthAvailability(AuthMethod authAvailability) {
        authList.getItems().add(authAvailability);
        LogHelper.info("Added %s: %s", authAvailability.getName(), authAvailability.getDisplayName());
    }

    @Deprecated
    public <T extends WebSocketEvent> void processing(Request<T> request, String text, Consumer<T> onSuccess,
            Consumer<String> onError) {
        processRequest(text, request, onSuccess, (thr) -> onError.accept(thr.getCause().getMessage()), null);
    }

    public <T> void processing(CompletableFuture<T> request, String text, Consumer<T> onSuccess,
            Consumer<String> onError) {
        processRequest(text, request, onSuccess, (thr) -> onError.accept(thr.getCause().getMessage()), null);
    }


    @Override
    public void errorHandle(Throwable e) {
        super.errorHandle(e);
        contextHelper.runInFxThread(() -> authButton.setState(LoginAuthButtonComponent.AuthButtonState.ERROR));
    }

    @Override
    public void reset() {
        authFlow.reset();
    }

    @Override
    public String getName() {
        return "login";
    }

    public void onSuccessLogin(AuthFlow.SuccessAuth successAuth) {
        var user = successAuth.user();
        application.authService.setUser(user);
        boolean savePassword = savePasswordCheckBox.isSelected();
        if (savePassword) {
            application.runtimeSettings.login = successAuth.recentLogin();
            application.runtimeSettings.password = null;
            application.runtimeSettings.lastAuth = authAvailability;
        }
        if (user != null
                && user.getAssets() != null) {
            try {
                Texture skin = user.getAssets().get("SKIN");
                Texture avatar = user.getAssets().get("AVATAR");
                if(skin != null || avatar != null) {
                    application.skinManager.addSkinWithAvatar(user.getUsername(),
                                                              skin != null ? new URI(skin.getUrl()) : null,
                                                              avatar != null ? new URI(avatar.getUrl()) : null);
                    application.skinManager.getSkin(user.getUsername()); //Cache skin
                }
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        contextHelper.runInFxThread(() -> {
            if(application.gui.welcomeOverlay.isInit()) {
                application.gui.welcomeOverlay.reset();
            }
            showOverlay(application.gui.welcomeOverlay,
                                                      (e) -> application.gui.welcomeOverlay.hide(2000,
                                                                                                 (f) -> onGetProfiles()));});
    }

    public void onGetProfiles() {
        processing(new ProfilesRequest(), application.getTranslation("runtime.overlay.processing.text.profiles"),
                   (profiles) -> {
                       application.profilesService.setProfilesResult(profiles);
                       application.runtimeSettings.profiles = profiles.profiles;
                       contextHelper.runInFxThread(() -> {
                           application.securityService.startRequest();
                           if (application.gui.optionsScene != null) {
                               try {
                                   application.profilesService.loadAll();
                               } catch (Throwable ex) {
                                   errorHandle(ex);
                               }
                           }
                           if (application.getCurrentScene() instanceof LoginScene loginScene) {
                               loginScene.authFlow.isLoginStarted = false;
                           }
                           application.setMainScene(application.gui.serverMenuScene);
                       });
                   }, null);
    }

    public void clearPassword() {
        application.runtimeSettings.password = null;
        application.runtimeSettings.login = null;
    }

    public AuthFlow getAuthFlow() {
        return authFlow;
    }

    private static class AuthAvailabilityStringConverter extends StringConverter<AuthMethod> {
        @Override
        public String toString(AuthMethod object) {
            return object == null ? "null" : object.getDisplayName();
        }

        @Override
        public GetAvailabilityAuthRequestEvent.AuthAvailability fromString(String string) {
            return null;
        }
    }

    public class LoginSceneAccessor extends SceneAccessor {

        public void showContent(AbstractVisualComponent component) throws Exception {
            component.init();
            component.postInit();
            if (contentComponent != null) {
                content.getChildren().clear();
            }
            contentComponent = component;
            content.getChildren().add(component.getLayout());
        }

        public LoginAuthButtonComponent getAuthButton() {
            return authButton;
        }

        public void setState(LoginAuthButtonComponent.AuthButtonState state) {
            authButton.setState(state);
        }

        public boolean isEmptyContent() {
            return content.getChildren().isEmpty();
        }

        public void clearContent() {
            content.getChildren().clear();
        }

        @Deprecated
        public <T extends WebSocketEvent> void processing(Request<T> request, String text, Consumer<T> onSuccess,
                Consumer<String> onError) {
            LoginScene.this.processing(request, text, onSuccess, onError);
        }

        public <T> void processing(CompletableFuture<T> request, String text, Consumer<T> onSuccess,
                Consumer<String> onError) {
            LoginScene.this.processing(request, text, onSuccess, onError);
        }
    }


}
