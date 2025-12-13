package pro.gravit.launcher.gui.scenes.options;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.components.UserBlock;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.core.impl.FxScene;
import pro.gravit.launcher.gui.scenes.interfaces.SceneSupportUserBlock;

public class OptionsScene extends FxScene implements SceneSupportUserBlock {
    private OptionsTab optionsTab;
    private UserBlock userBlock;

    public OptionsScene(JavaFXApplication application) {
        super("scenes/options/options.fxml", application);
    }

    @Override
    protected void doInit() {
        this.userBlock = use(layout, UserBlock::new);
        optionsTab = new OptionsTab(application, LookupHelper.lookup(layout, "#tabPane"));

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
                switchToBackScene();
            } catch (Exception ex) {
                errorHandle(ex);
            }
        }));

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#clientSettings").ifPresent(b -> b.setOnAction((e) -> {
            try {
                switchScene(application.gui.settingsScene);
                application.gui.settingsScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));
    }

    @Override
    public void reset() {
        var profile = application.profileService.getCurrentProfile();
        var profileSettings = LauncherBackendAPIHolder.getApi().makeClientProfileSettings(profile);

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#save").ifPresent(b -> b.setOnAction((e) -> {
            try {
                LauncherBackendAPIHolder.getApi().saveClientProfileSettings(profileSettings);
                // switchScene(application.gui.serverInfoScene); // Do we want to go back?
                // usually save stays on page or goes back.
                // Settings usually stays.
                // But original logic went to serverInfoScene.
                // Let's stick to staying on page or giving feedback, but for now matching
                // original behavior might be jarring if it jumps pages.
                // But seeing as this is "Optional Mods", maybe going back to Info is fine.
                // Actually, let's just save.
                switchScene(application.gui.serverInfoScene);
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#reset").ifPresent(b -> b.setOnAction((e) -> {
            optionsTab.clear();
            optionsTab.addProfileOptionals(profileSettings);
        }));

        optionsTab.clear();
        optionsTab.addProfileOptionals(profileSettings);

        LookupHelper.<Label>lookupIfPossible(layout, "#emptyList").ifPresent((e) -> {
            e.setVisible(profileSettings.getAllOptionals().isEmpty());
            e.setManaged(profileSettings.getAllOptionals().isEmpty());
        });
        LookupHelper.<TabPane>lookupIfPossible(layout, "#tabPane").ifPresent((e) -> {
            e.setVisible(!profileSettings.getAllOptionals().isEmpty());
        });

        userBlock.reset();
    }

    @Override
    public String getName() {
        return "options";
    }

    @Override
    public UserBlock getUserBlock() {
        return userBlock;
    }
}
