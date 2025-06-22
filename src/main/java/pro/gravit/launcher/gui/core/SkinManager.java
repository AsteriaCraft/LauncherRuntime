package pro.gravit.launcher.gui.core;

import javafx.scene.image.Image;
import pro.gravit.launcher.gui.core.utils.SkinUtils;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {

    private final JavaFXApplication application;
    private final Map<String, SkinUtils.SkinEntry> map = new ConcurrentHashMap<>();

    public SkinManager(JavaFXApplication application) {
        this.application = application;
    }

    public void addSkin(String username, URI url) {
        map.put(username, new SkinUtils.SkinEntry(url));
    }

    public void addOrReplaceSkin(String username, URI url) {
        SkinUtils.SkinEntry entry = map.get(username);
        if(entry == null) {
            map.put(username, new SkinUtils.SkinEntry(url));
        } else {
            map.put(username, new SkinUtils.SkinEntry(url, entry.getAvatarUrl()));
        }
    }

    public void addSkinWithAvatar(String username, URI url, URI avatarUrl) {
        map.put(username, new SkinUtils.SkinEntry(url, avatarUrl));
    }

    public BufferedImage getSkin(String username) {
        SkinUtils.SkinEntry entry = map.get(username);
        if (entry == null) return null;
        return entry.getFullImage();
    }

    public BufferedImage getSkinHead(String username) {
        SkinUtils.SkinEntry entry = map.get(username);
        if (entry == null) return null;
        return entry.getHeadImage();
    }

    public Image getFxSkin(String username) {
        SkinUtils.SkinEntry entry = map.get(username);
        if (entry == null) return null;
        return entry.getFullFxImage();
    }

    public Image getFxSkinHead(String username) {
        SkinUtils.SkinEntry entry = map.get(username);
        if (entry == null) return null;
        return entry.getHeadFxImage();
    }

    public BufferedImage getScaledSkin(String username, int width, int height) {
        BufferedImage image = getSkin(username);
        return SkinUtils.scaleImage(image, width, height);
    }

    public BufferedImage getScaledSkinHead(String username, int width, int height) {
        BufferedImage image = getSkinHead(username);
        return SkinUtils.scaleImage(image, width, height);
    }

    public Image getScaledFxSkin(String username, int width, int height) {
        BufferedImage image = getSkin(username);
        return SkinUtils.convertToFxImage(SkinUtils.scaleImage(image, width, height));
    }

    public Image getScaledFxSkinHead(String username, int width, int height) {
        BufferedImage image = getSkinHead(username);
        if (image == null) return null;
        return SkinUtils.convertToFxImage(SkinUtils.scaleImage(image, width, height));
    }

}
