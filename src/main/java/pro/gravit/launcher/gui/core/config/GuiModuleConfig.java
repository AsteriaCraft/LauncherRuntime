package pro.gravit.launcher.gui.core.config;

import pro.gravit.launcher.core.LauncherInject;

import java.util.HashMap;
import java.util.Map;

public class GuiModuleConfig {
    @LauncherInject(value = "modules.javaruntime.createaccounturl")
    public String createAccountURL;
    @LauncherInject(value = "modules.javaruntime.forgotpassurl")
    public String forgotPassURL;
    @LauncherInject(value = "modules.javaruntime.lazy")
    public boolean lazy;
    @LauncherInject(value = "modules.javaruntime.disableofflinemode")
    public boolean disableOfflineMode;
    @LauncherInject(value = "modules.javaruntime.disabledebugpermissions")
    public boolean disableDebugPermissions;

    @LauncherInject(value = "modules.javaruntime.autoauth")
    public boolean autoAuth;

    @LauncherInject(value = "modules.javaruntime.locale")
    public String locale;

    @LauncherInject(value = "modules.javaruntime.downloadthreads")
    public int downloadThreads = 4;

    public static Object getDefault() {
        GuiModuleConfig config = new GuiModuleConfig();
        config.createAccountURL = "https://gravitlauncher.com/createAccount.php";
        config.forgotPassURL = "https://gravitlauncher.com/fogotPass.php";
        config.lazy = false;
        config.disableOfflineMode = false;
        config.autoAuth = false;
        config.locale = "RUSSIAN";
        config.downloadThreads = 4;
        return config;
    }
}
