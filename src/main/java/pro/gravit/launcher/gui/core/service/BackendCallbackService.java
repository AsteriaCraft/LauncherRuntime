package pro.gravit.launcher.gui.core.service;

import javafx.application.Platform;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.api.model.SelfUser;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.gui.JavaRuntimeModule;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.core.impl.ContextHelper;
import pro.gravit.launcher.gui.scenes.login.AuthFlow;
import pro.gravit.launcher.gui.scenes.login.LoginScene;
import pro.gravit.utils.helper.LogHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BackendCallbackService extends LauncherBackendAPI.MainCallback {
    private final JavaFXApplication application;
    public CompletableFuture<LauncherBackendAPI.LauncherInitData> initDataCallback;

    public BackendCallbackService(JavaFXApplication application) {
        this.application = application;
    }

    @Override
    public void onChangeStatus(String status) {
        super.onChangeStatus(status);
    }

    @Override
    public void onProfiles(List<ProfileFeatureAPI.ClientProfile> profiles) {
        application.profileService.setProfiles(profiles);
    }

    @Override
    public void onAuthorize(SelfUser selfUser) {
        ContextHelper.runInFxThreadStatic(() -> {
            application.authService.setUser(selfUser);
            if(application.getMainStage().getVisualComponent() instanceof LoginScene loginScene) {
                if(loginScene.getAuthFlow().isLoginStarted) {
                    return;
                }
                loginScene.onSuccessLogin(new AuthFlow.SuccessAuth(selfUser, selfUser.getUsername(), null));
            }
        });
    }

    @Override
    public void onNotify(String header, String description) {
        application.messageManager.createNotification(header, description);
    }

    @Override
    public void onShutdown() {
        boolean isAlreadyShutdown = JavaRuntimeModule.SHUTDOWN_STARTED.getAndSet(true); // It can be called by launcher itself
        if(!isAlreadyShutdown) {
            Platform.exit();
            LogHelper.info("Method after Platform.exit() called");
        }
    }
}
