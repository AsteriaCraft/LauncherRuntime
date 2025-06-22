package pro.gravit.launcher.gui.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.core.backend.extensions.TextureUploadExtension;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.DesignConstants;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.core.impl.FxComponent;
import pro.gravit.launcher.gui.core.utils.JavaFxUtils;
import pro.gravit.utils.helper.LogHelper;

public class UserBlock extends FxComponent {
    private ImageView avatar;
    private Image originalAvatarImage;

    public UserBlock(Pane layout, JavaFXApplication application) {
        super(layout, application);
    }

    @Override
    public String getName() {
        return "userBlock";
    }

    @Override
    protected void doInit() {
        avatar = LookupHelper.lookup(layout, "#avatar");
        originalAvatarImage = avatar.getImage();
        LookupHelper.<ImageView>lookupIfPossible(layout, "#avatar").ifPresent((h) -> {
            try {
                JavaFxUtils.setStaticRadius(h, DesignConstants.AVATAR_IMAGE_RADIUS);
                h.setImage(originalAvatarImage);
            } catch (Throwable e) {
                LogHelper.warning("Skin head error");
            }
        });
        reset();
    }

    public void reset() {
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname")
                    .ifPresent((e) -> e.textProperty().bind(application.authService.username));
        LookupHelper.<Label>lookupIfPossible(layout, "#role")
                    .ifPresent((e) -> e.setText(application.authService.getMainRole()));
        avatar.setImage(originalAvatarImage);
        resetAvatar();
        TextureUploadExtension extension = LauncherBackendAPIHolder.getApi().getExtension(TextureUploadExtension.class);
        if(extension != null) {
            LookupHelper.<Button>lookupIfPossible(layout, "#customization").ifPresent((h) -> {
                h.setVisible(true);
                h.setOnAction((a) -> application.gui.processingOverlay.processRequest(currentStage,
                        application.getTranslation("runtime.overlay.processing.text.uploadassetinfo"),
                        extension.fetchTextureUploadInfo(), (info) ->
                                contextHelper.runInFxThread(() ->
                                                                    application.gui.uploadAssetOverlay.show(currentStage, (f) -> application.gui.uploadAssetOverlay.onAssetUploadInfo(info))), this::errorHandle, (e) -> {}));
            });
        }
    }

    public void resetAvatar() {
        if (avatar == null) {
            return;
        }
        JavaFxUtils.putAvatarToImageView(application, application.authService.getUsername(), avatar);
    }
}
