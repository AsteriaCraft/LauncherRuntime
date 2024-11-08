package pro.gravit.launcher.gui.service;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.core.api.features.AuthFeatureAPI;
import pro.gravit.launcher.core.api.method.AuthMethod;
import pro.gravit.launcher.core.api.method.AuthMethodPassword;
import pro.gravit.launcher.core.api.model.User;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.password.*;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.ArrayList;
import java.util.List;

public class AuthService {
    private final LauncherConfig config = Launcher.getConfig();
    private final JavaFXApplication application;
    private AuthFeatureAPI.AuthResponse rawAuthResult;
    private AuthMethod authAvailability;

    public AuthService(JavaFXApplication application) {
        this.application = application;
    }

    public AuthMethodPassword makePassword(String plainPassword) {
        if (config.passwordEncryptKey != null) {
            try {
                return new AuthAESPassword(encryptAESPassword(plainPassword));
            } catch (Exception ignored) {
            }
        }
        return new AuthPlainPassword(plainPassword);
    }

    private byte[] encryptAESPassword(String password) throws Exception {
        return SecurityHelper.encrypt(Launcher.getConfig().passwordEncryptKey, password);
    }

    public void setAuthResult(String authId, AuthFeatureAPI.AuthResponse rawAuthResult) {
        this.rawAuthResult = rawAuthResult;
    }

    public void setAuthAvailability(AuthMethod info) {
        this.authAvailability = info;
    }

    public AuthMethod getAuthAvailability() {
        return authAvailability;
    }

    public boolean isFeatureAvailable(String name) {
        return authAvailability.getFeatures() != null && authAvailability.getFeatures().contains(name);
    }

    public String getUsername() {
        if (rawAuthResult == null || rawAuthResult.user() == null) return "Player";
        return rawAuthResult.user().getUsername();
    }

    public String getMainRole() {
        return "";
    }

    public boolean checkPermission(String name) {
        if (rawAuthResult == null || rawAuthResult.user().getPermissions() == null) {
            return false;
        }
        return rawAuthResult.user().getPermissions().hasPerm(name);
    }

    public boolean checkDebugPermission(String name) {
        return application.isDebugMode() || (!application.guiModuleConfig.disableDebugPermissions &&
                checkPermission("launcher.debug."+name));
    }

    public User getPlayerProfile() {
        if (rawAuthResult == null) return null;
        return rawAuthResult.user();
    }

    public String getAccessToken() {
        if (rawAuthResult == null) return null;
        return rawAuthResult.authToken().getAccessToken();
    }

    public void exit() {
        rawAuthResult = null;
        //.profile = null;
    }
}
