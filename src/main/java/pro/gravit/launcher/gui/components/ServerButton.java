package pro.gravit.launcher.gui.components;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.core.impl.FxComponent;
import pro.gravit.launcher.gui.core.utils.JavaFxUtils;
import pro.gravit.utils.helper.LogHelper;

import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

public class ServerButton extends FxComponent {
    private static final String SERVER_BUTTON_FXML = "components/serverButton.fxml";
    private static final String SERVER_BUTTON_CUSTOM_FXML = "components/serverButton/%s.fxml";
    private static final String SERVER_BUTTON_DEFAULT_IMAGE = "images/servers/example.png";
    private static final String SERVER_BUTTON_CUSTOM_IMAGE = "images/servers/%s.png";
    public ProfileFeatureAPI.ClientProfile profile;
    private Button saveButton;
    private Button resetButton;
    private ProgressBar progressBar;

    protected ServerButton(JavaFXApplication application, ProfileFeatureAPI.ClientProfile profile) {
        super(getServerButtonFxml(application, profile), application);
        this.profile = profile;
    }

    public static ServerButton createServerButton(JavaFXApplication application,
            ProfileFeatureAPI.ClientProfile profile) {
        return new ServerButton(application, profile);
    }

    private static String getServerButtonFxml(JavaFXApplication application, ProfileFeatureAPI.ClientProfile profile) {
        String customFxml = String.format(SERVER_BUTTON_CUSTOM_FXML, profile.getUUID().toString());
        URL fxml = application.tryResource(customFxml);
        if (fxml != null) {
            return customFxml;
        }
        return SERVER_BUTTON_FXML;
    }

    @Override
    public String getName() {
        return "serverButton";
    }

    @Override
    protected void doInit() {
        LookupHelper.<Labeled>lookup(layout, "#nameServer").setText(profile.getName());
        LookupHelper.<Labeled>lookup(layout, "#version").setText(profile.getMinecraftVersion());
        LookupHelper.<ProgressBar>lookupIfPossible(layout, "#playersTrack").ifPresent(p -> progressBar = p);
        ImageView serverLogo = LookupHelper.lookup(layout, "#serverButtonImage");
        URL logo = application.tryResource(String.format(SERVER_BUTTON_CUSTOM_IMAGE, profile.getUUID().toString()));
        if (logo == null) {
            logo = application.tryResource(SERVER_BUTTON_DEFAULT_IMAGE);
        }
        if (logo != null) {
            serverLogo.setImage(new Image(logo.toString()));
            JavaFxUtils.setStaticRadius(serverLogo, 10.0);
        }
        AtomicLong currentOnline = new AtomicLong(0);
        AtomicLong maxOnline = new AtomicLong(0);
        Runnable update = () -> contextHelper.runInFxThread(() -> {
            if (currentOnline.get() == 0 && maxOnline.get() == 0) {
                LookupHelper.<Labeled>lookupIfPossible(layout, "#serverOnline").ifPresent(l -> l.setText("?"));
                // Hide online info, show offline label
                LookupHelper.lookupIfPossible(layout, "#serverInfoPane").ifPresent(p -> p.setVisible(false));
                LookupHelper.lookupIfPossible(layout, "#serverOfflineLabel").ifPresent(p -> p.setVisible(true));
                LookupHelper.lookupIfPossible(layout, "#serverOfflineLabel").ifPresent(p -> p.setVisible(true));
                if (progressBar != null)
                    progressBar.setProgress(0);
            } else {
                LookupHelper.<Labeled>lookupIfPossible(layout, "#serverOnline")
                        .ifPresent(l -> l.setText(String.format("%d / %d", currentOnline.get(), maxOnline.get())));
                if (progressBar != null && maxOnline.get() > 0) {
                    progressBar.setProgress((double) currentOnline.get() / maxOnline.get());
                }
                // Show online info, hide offline label
                LookupHelper.lookupIfPossible(layout, "#serverInfoPane").ifPresent(p -> p.setVisible(true));
                LookupHelper.lookupIfPossible(layout, "#serverOfflineLabel").ifPresent(p -> p.setVisible(false));
            }
        });
        application.pingService.getPingReport(profile.getUUID()).thenAccept((report) -> {
            if (report != null) {
                currentOnline.addAndGet(report.playersOnline);
                maxOnline.addAndGet(report.maxPlayers);
            }
            update.run();
        });
        LookupHelper.<Button>lookupIfPossible(layout, "#save").ifPresent(b -> saveButton = b);
        LookupHelper.<Button>lookupIfPossible(layout, "#reset").ifPresent(b -> resetButton = b);
    }

    @Override
    protected void doPostInit() {

    }

    public void setOnMouseClicked(EventHandler<? super MouseEvent> eventHandler) {
        layout.setOnMouseClicked(eventHandler);
    }

    public void enableSaveButton(String text, EventHandler<ActionEvent> eventHandler) {
        if (saveButton != null) {
            saveButton.setVisible(true);
            if (text != null)
                saveButton.setText(text);
            saveButton.setOnAction(eventHandler);
        }
    }

    public void enableResetButton(String text, EventHandler<ActionEvent> eventHandler) {
        if (resetButton != null) {
            resetButton.setVisible(true);
            if (text != null)
                resetButton.setText(text);
            resetButton.setOnAction(eventHandler);
        }
    }

    public void addTo(Pane pane) {
        if (!isInit()) {
            try {
                init();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        pane.getChildren().add(layout);
    }

    public void addTo(Pane pane, int position) {
        if (!isInit()) {
            try {
                init();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        pane.getChildren().add(position, layout);
    }

    @Override
    public void reset() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void enable() {

    }
}
