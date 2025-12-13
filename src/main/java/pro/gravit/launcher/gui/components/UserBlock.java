package pro.gravit.launcher.gui.components;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.core.backend.extensions.TextureUploadExtension;
import pro.gravit.launcher.gui.core.JavaFXApplication;

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
        LogHelper.info("UserBlock doInit");
        LookupHelper.<ImageView>lookupIfPossible(layout, "#avatar")
                .or(() -> LookupHelper.<Labeled>lookupIfPossible(layout, "#profile")
                        .map(Labeled::getGraphic)
                        .filter(node -> node instanceof Parent)
                        .map(node -> (ImageView) node.lookup("#avatar")))
                .ifPresentOrElse((h) -> {
                    avatar = h;
                    originalAvatarImage = h.getImage();
                    LogHelper.info("Found avatar ImageView using lookup strategies: " + h);
                    try {
                        h.setImage(originalAvatarImage);
                    } catch (Throwable e) {
                        LogHelper.warning("Skin head error");
                    }
                }, () -> LogHelper.warning("Avatar ImageView not found in UserBlock"));
        reset();
    }

    public void reset() {
        LookupHelper.<Label>lookupIfPossible(layout, "#nickname")
                .ifPresent((e) -> e.textProperty().bind(application.authService.username));
        LookupHelper.<Label>lookupIfPossible(layout, "#role")
                .ifPresent((e) -> e.setText(application.authService.getMainRole()));
        if (avatar != null) {
            avatar.setImage(originalAvatarImage);
        }
        resetAvatar();
        TextureUploadExtension extension = LauncherBackendAPIHolder.getApi().getExtension(TextureUploadExtension.class);
        if (extension != null) {
            LookupHelper.<Button>lookupIfPossible(layout, "#customization").ifPresent((h) -> {
                h.setVisible(true);
                h.setOnAction((a) -> application.gui.processingOverlay.processRequest(currentStage,
                        application.getTranslation("runtime.overlay.processing.text.uploadassetinfo"),
                        extension.fetchTextureUploadInfo(),
                        (info) -> contextHelper.runInFxThread(() -> application.gui.uploadAssetOverlay
                                .show(currentStage, (f) -> application.gui.uploadAssetOverlay.onAssetUploadInfo(info))),
                        this::errorHandle, (e) -> {
                        }));
            });
        }
    }

    public void resetAvatar() {
        if (avatar == null) {
            return;
        }
        String username = application.authService.getUsername();
        int width = (int) avatar.getFitWidth();
        int height = (int) avatar.getFitHeight();
        application.workers.submit(() -> {
            Image head = application.skinManager.getScaledFxSkinHead(username, width, height);
            if (head != null) {
                contextHelper.runInFxThread(() -> avatar.setImage(head));
            }
        });
    }
}
