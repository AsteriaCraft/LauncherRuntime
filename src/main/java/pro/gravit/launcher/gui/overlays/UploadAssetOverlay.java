package pro.gravit.launcher.gui.overlays;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.FileChooser;
import pro.gravit.launcher.base.events.request.AssetUploadInfoRequestEvent;
import pro.gravit.launcher.base.profiles.Texture;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.api.features.TextureUploadFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPIHolder;
import pro.gravit.launcher.core.backend.extensions.TextureUploadExtension;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.scenes.interfaces.SceneSupportUserBlock;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class UploadAssetOverlay extends CenterOverlay {
    private static final HttpClient client = HttpClient.newBuilder()
                                                       .build();
    private Button uploadSkin;
    private Button uploadCape;
    private CheckBox useSlim;
    private final AtomicBoolean requireManualSlimSkinSelection = new AtomicBoolean(true);
    public UploadAssetOverlay(JavaFXApplication application) {
        super("overlay/uploadasset/uploadasset.fxml", application);
    }

    @Override
    public String getName() {
        return "uploadasset";
    }

    @Override
    protected void doInit() {
        uploadSkin = LookupHelper.lookup(layout, "#uploadskin");
        uploadCape = LookupHelper.lookup(layout, "#uploadcape");
        useSlim = LookupHelper.lookup(layout, "#useslim");
        uploadSkin.setOnAction((a) -> uploadAsset("SKIN", requireManualSlimSkinSelection.get() ?
                new AssetOptions(useSlim.isSelected()) : null));
        uploadCape.setOnAction((a) -> uploadAsset("CAPE", null));
        LookupHelper.<Button>lookupIfPossible(layout, "#close").ifPresent((b) -> b.setOnAction((e) -> hide(0, null)));
    }

    public void onAssetUploadInfo(TextureUploadFeatureAPI.TextureUploadInfo event) {
        boolean uploadSkinAvailable = event.getAvailable().contains("SKIN");
        boolean uploadCapeAvailable = event.getAvailable().contains("CAPE");
        uploadSkin.setVisible(uploadSkinAvailable);
        uploadCape.setVisible(uploadCapeAvailable);
        if(uploadSkinAvailable) {
            requireManualSlimSkinSelection.set(event.isRequireManualSlimSkinSelect());
            useSlim.setVisible(event.isRequireManualSlimSkinSelect());
        }
    }

    public void uploadAsset(String name, AssetOptions options) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = chooser.showOpenDialog(currentStage.getStage());
        if(file == null) {
            return;
        }
        try {
            byte[] skinBytes = IOHelper.read(file.toPath());
            TextureUploadExtension textureUploadExtension = LauncherBackendAPIHolder.getApi().getExtension(
                    TextureUploadExtension.class);
            textureUploadExtension.uploadTexture(name, skinBytes, options == null ? null : new TextureUploadFeatureAPI.UploadSettings(options.modelSlim())).thenAccept((texture) -> {
                URI skinUrl = URI.create(texture.getUrl());
                if("SKIN".equals(name)) {
                    application.skinManager.addOrReplaceSkin(application.authService.getUsername(), skinUrl);
                    for(var scene : application.gui.getComponents()) {
                        if(scene.isInit() && scene instanceof SceneSupportUserBlock supportUserBlock) {
                            supportUserBlock.getUserBlock().resetAvatar();
                        }
                    }
                }
                contextHelper.runInFxThread(() -> application.messageManager.createNotification(application.getTranslation("runtime.overlay.uploadasset.success.header"), application.getTranslation("runtime.overlay.uploadasset.success.description")));
            }).handle(this::errorHandle);
        } catch (IOException e) {
            errorHandle(e);
        }
    }

    public static final class AssetOptions {
        @LauncherNetworkAPI
        private final boolean modelSlim;

        public AssetOptions(boolean modelSlim) {
            this.modelSlim = modelSlim;
        }

        public boolean modelSlim() {
            return modelSlim;
        }

    }

    public record UploadError(@LauncherNetworkAPI String error) {

    }

    public record UserTexture(@LauncherNetworkAPI String url,@LauncherNetworkAPI String digest,@LauncherNetworkAPI Map<String, String> metadata) {

        Texture toLauncherTexture() {
            return new Texture(url, SecurityHelper.fromHex(digest), metadata);
        }

        }

    @Override
    public void reset() {

    }
}
