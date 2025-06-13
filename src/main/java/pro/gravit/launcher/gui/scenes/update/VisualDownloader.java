package pro.gravit.launcher.gui.scenes.update;

import javafx.beans.property.DoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.gui.impl.ContextHelper;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class VisualDownloader extends LauncherBackendAPI.DownloadCallback {
    private final JavaFXApplication application;
    private final AtomicLong totalDownloaded = new AtomicLong(0);
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final AtomicLong lastDownloaded = new AtomicLong(0);

    private final AtomicLong totalSize = new AtomicLong();

    private final ProgressBar progressBar;
    private final Label speed;
    private final Label volume;

    private final Consumer<String> addLog;
    private volatile Runnable cancelCallback;

    public VisualDownloader(JavaFXApplication application, ProgressBar progressBar, Label speed, Label volume, Consumer<String> addLog) {
        this.application = application;
        this.progressBar = progressBar;
        this.speed = speed;
        this.volume = volume;
        this.addLog = addLog;
    }

    @Override
    public void onStage(String stage) {
        super.onStage(stage);
        addLog.accept(String.format("Stage %s", stage));
    }

    @Override
    public void onCanCancel(Runnable cancel) {
        super.onCanCancel(cancel);
        cancelCallback = cancel;
    }

    @Override
    public void onTotalDownload(long total) {
        super.onTotalDownload(total);
        totalSize.set(total);
    }

    @Override
    public void onCurrentDownloaded(long current) {
        super.onCurrentDownloaded(current);
        updateProgress(current);
    }

    private void resetProgress() {
        totalDownloaded.set(0);
        lastUpdateTime.set(System.currentTimeMillis());
        lastDownloaded.set(0);
        progressBar.progressProperty().setValue(0);
    }


    private void updateProgress(long newValue) {
        DoubleProperty property = progressBar.progressProperty();
        property.set((double) newValue / (double) totalSize.get());
        long lastTime = lastUpdateTime.get();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime >= 130) {
            double bytesSpeed = (double) (newValue - lastDownloaded.get()) / (double) (currentTime - lastTime) * 1000.0;
            String speedFormat = "%.1f".formatted(bytesSpeed * 8 / (1000.0 * 1000.0));
            ContextHelper.runInFxThreadStatic(() -> {
                volume.setText("%.1f/%.1f MB".formatted((double) newValue / (1024.0 * 1024.0),
                                                           (double) totalSize.get() / (1024.0 * 1024.0)));
                speed.setText(speedFormat);
            });
            lastUpdateTime.set(currentTime);
            lastDownloaded.set(newValue);
        }
    }

    public void cancel() {
        cancelCallback.run();
    }
}
