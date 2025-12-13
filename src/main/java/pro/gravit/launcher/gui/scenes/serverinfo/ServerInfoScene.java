package pro.gravit.launcher.gui.scenes.serverinfo;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.components.ServerButton;
import pro.gravit.launcher.gui.components.UserBlock;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.core.impl.FxScene;
import pro.gravit.launcher.gui.scenes.interfaces.SceneSupportUserBlock;

public class ServerInfoScene extends FxScene implements SceneSupportUserBlock {
    private ServerButton serverButton;
    private UserBlock userBlock;

    public ServerInfoScene(JavaFXApplication application) {
        super("scenes/serverinfo/serverinfo.fxml", application);
    }

    @Override
    protected void doInit() {
        this.userBlock = use(layout, UserBlock::new);
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> {
            e.setText(application.profileService.getCurrentProfile().getName());
        });
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#backToServers").ifPresent(b -> b.setOnAction((e) -> {
            try {
                switchToBackScene();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        }));

        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#clientSettings").ifPresent(b -> b.setOnAction((e) -> {
            try {
                switchScene(application.gui.settingsScene);
                application.gui.settingsScene.reset();
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
    }

    @Override
    public void reset() {
        ProfileFeatureAPI.ClientProfile profile = application.profileService.getCurrentProfile();
        LookupHelper.<Label>lookupIfPossible(layout, "#serverName").ifPresent((e) -> e.setText(profile.getName()));
        LookupHelper.<ScrollPane>lookupIfPossible(layout, "#serverDescriptionPane").ifPresent((e) -> {
            var label = (Label) e.getContent();
            label.setText(profile.getDescription());
        });
        Pane serverButtonContainer = LookupHelper.lookup(layout, "#serverButton");
        serverButtonContainer.getChildren().clear();
        serverButton = ServerButton.createServerButton(application, profile);
        serverButton.addTo(serverButtonContainer);
        serverButton.enableSaveButton(application.getTranslation("runtime.scenes.serverinfo.serverButton.game"),
                (e) -> runClient());
        this.userBlock.reset();
    }

    private void runClient() {
        var profile = application.profileService.getCurrentProfile();
        contextHelper.runInFxThread(() -> {
            switchScene(application.gui.updateScene);
            var downloadProfile = LauncherBackendAPIHolder.getApi().downloadProfile(profile,
                    LauncherBackendAPIHolder.getApi().makeClientProfileSettings(profile),
                    application.gui.updateScene.makeDownloadCallback());
            downloadProfile.thenAccept((readyProfile) -> {
                contextHelper.runInFxThread(() -> {
                    switchScene(application.gui.debugScene);
                    application.gui.debugScene.run(readyProfile);
                }).handle((success, error) -> {
                    if (error != null) {
                        errorHandle(error);
                    }
                    return null;
                });
            }).exceptionally(e -> {
                contextHelper.runInFxThread(() -> {
                    errorHandle(e);
                });
                return null;
            });

        });
        /*
         * application.launchService.launchClient().thenAccept((clientInstance -> {
         * if (application.runtimeSettings.globalSettings.debugAllClients ||
         * clientInstance.getSettings().debug) {
         * contextHelper.runInFxThread(() -> {
         * try {
         * switchScene(application.gui.debugScene);
         * application.gui.debugScene.onClientInstance(clientInstance);
         * } catch (Exception ex) {
         * errorHandle(ex);
         * }
         * });
         * } else {
         * clientInstance.start();
         * clientInstance.getOnWriteParamsFuture().thenAccept((ok) -> {
         * LogHelper.info("Params write successful. Exit...");
         * Platform.exit();
         * }).exceptionally((ex) -> {
         * contextHelper.runInFxThread(() -> errorHandle(ex));
         * return null;
         * });
         * }
         * })).exceptionally((ex) -> {
         * contextHelper.runInFxThread(() -> errorHandle(ex));
         * return null;
         * });
         */
    }

    @Override
    public String getName() {
        return "serverinfo";
    }

    @Override
    public UserBlock getUserBlock() {
        return userBlock;
    }
}
