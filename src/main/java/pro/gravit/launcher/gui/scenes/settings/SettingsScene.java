package pro.gravit.launcher.gui.scenes.settings;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import javafx.util.StringConverter;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.gui.core.JavaFXApplication;

import pro.gravit.launcher.gui.components.UserBlock;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.scenes.interfaces.SceneSupportUserBlock;
import pro.gravit.launcher.gui.scenes.settings.components.JavaSelector;

import java.text.MessageFormat;

public class SettingsScene extends BaseSettingsScene implements SceneSupportUserBlock {

    private final static long MAX_JAVA_MEMORY_X64 = 32 * 1024;
    private final static long MAX_JAVA_MEMORY_X32 = 1536;
    private Label ramLabel;
    private Slider ramSlider;
    private LauncherBackendAPI.ClientProfileSettings profileSettings;
    private JavaSelector javaSelector;
    private UserBlock userBlock;

    public SettingsScene(JavaFXApplication application) {
        super("scenes/settings/settings.fxml", application);
    }

    @Override
    protected void doInit() {
        super.doInit();
        this.userBlock = use(layout, UserBlock::new);

        ramSlider = LookupHelper.lookup(componentList, "#ramSlider");
        ramLabel = LookupHelper.lookup(componentList, "#ramLabel");

        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramSlider.setMinorTickCount(1);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setBlockIncrement(1024);
        ramSlider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double object) {
                return "%.0fG".formatted(object / 1024);
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> {
            var profile = application.profileService.getCurrentProfile();
            if (profile != null)
                e.setText(profile.getName());
        });

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#backToServers").ifPresent(b -> b.setOnAction((e) -> {
            try {
                switchToBackScene();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#navInfo").ifPresent(b -> b.setOnAction((e) -> {
            try {
                // Return to Server Info (which is likely the back scene if we came from there)
                // Or explicitly use switchScene if we have the reference.
                // For safety, let's treat it as Back if we assume flow ServerInfo -> Settings.
                // But better: try to find serverInfoScene.
                // IF we are in context, application.gui.serverInfoScene should be available.
                // Ideally switchToBackScene() works if we pushed history.
                // But user might want to jump straight to Info.
                // Let's assume Back logic for now as it's safer.
                switchToBackScene();
            } catch (Exception ex) {
                errorHandle(ex);
            }
        }));

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#optionalMods").ifPresent(b -> b.setOnAction((e) -> {
            try {
                if (application.profileService.getCurrentProfile() == null)
                    return;
                switchScene(application.gui.optionsScene);
                application.gui.optionsScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        reset();
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#savesettings").ifPresent(a -> a.setOnAction((e) -> {
            try {
                LauncherBackendAPIHolder.getApi().saveClientProfileSettings(profileSettings);
                switchToBackScene();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#reset").ifPresent(a -> a.setOnAction((e) -> reset()));
    }

    @Override
    public void reset() {
        super.reset();
        var profile = application.profileService.getCurrentProfile();
        profileSettings = LauncherBackendAPIHolder.getApi().makeClientProfileSettings(profile);
        javaSelector = new JavaSelector(componentList, profileSettings, profile);
        ramSlider.setValue(getReservedMemoryMbs());
        ramSlider.setMax(
                profileSettings.getMaxMemoryBytes(LauncherBackendAPI.ClientProfileSettings.MemoryClass.TOTAL) >> 20);
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            profileSettings.setReservedMemoryBytes(LauncherBackendAPI.ClientProfileSettings.MemoryClass.TOTAL,
                    (long) newValue.intValue() << 20);
            updateRamLabel();
        });
        updateRamLabel();

        for (var flag : profileSettings.getAvailableFlags()) {
            add(flag.name(), profileSettings.hasFlag(flag), (value) -> {
                if (value) {
                    profileSettings.addFlag(flag);
                } else {
                    profileSettings.removeFlag(flag);
                }
            }, false);
        }
        userBlock.reset();
    }

    private long getReservedMemoryMbs() {
        return profileSettings.getReservedMemoryBytes(LauncherBackendAPI.ClientProfileSettings.MemoryClass.TOTAL) >> 20;
    }

    @Override
    public UserBlock getUserBlock() {
        return userBlock;
    }

    @Override
    public String getName() {
        return "settings";
    }

    public void updateRamLabel() {
        ramLabel.setText(getReservedMemoryMbs() == 0
                ? application.getTranslation("runtime.scenes.settings.ramAuto")
                : MessageFormat.format(application.getTranslation("runtime.scenes.settings.ram"),
                        getReservedMemoryMbs()));
    }
}
