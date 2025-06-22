package pro.gravit.launcher.gui.overlays;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.DesignConstants;
import pro.gravit.launcher.gui.core.impl.FxOverlay;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.core.utils.JavaFxUtils;
import pro.gravit.utils.helper.LogHelper;

public class WelcomeOverlay extends FxOverlay {
    private Image originalImage;
    public WelcomeOverlay(JavaFXApplication application) {
        super("overlay/welcome/welcome.fxml", application);
    }

    @Override
    public String getName() {
        return "welcome";
    }

    @Override
    protected void doInit() {
        reset();
    }

    @Override
    public void reset() {
        LookupHelper.<Label>lookupIfPossible(layout, "#playerName")
                    .ifPresent((e) -> e.textProperty().bind(application.authService.username));
        LookupHelper.<ImageView>lookupIfPossible(layout, "#playerHead").ifPresent((h) -> {
            try {
                JavaFxUtils.setStaticRadius(h, DesignConstants.AVATAR_IMAGE_RADIUS);
                Image image = application.skinManager.getScaledFxSkinHead(
                        application.authService.getUsername(), (int) h.getFitWidth(), (int) h.getFitHeight());
                if (image != null) {
                    if(originalImage == null) {
                        originalImage = h.getImage();
                    }
                    h.setImage(image);
                } else if(originalImage != null) {
                    h.setImage(originalImage);
                }
            } catch (Throwable e) {
                LogHelper.warning("Skin head error");
            }
        });
    }
}
