package pro.gravit.launcher.gui.scenes.update;

import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.scenes.AbstractScene;
import pro.gravit.utils.helper.LogHelper;

import java.util.concurrent.CompletionException;

public class UpdateScene extends AbstractScene {
    private ProgressBar progressBar;
    private Label speed;
    private Label volume;
    private TextArea logOutput;
    private Button cancel;
    private Label speedtext;
    private Label speederr;
    private Pane speedon;

    private VisualDownloader downloader;

    public UpdateScene(JavaFXApplication application) {
        super("scenes/update/update.fxml", application);
    }

    @Override
    protected void doInit() {
        progressBar = LookupHelper.lookup(layout, "#progress");
        speed = LookupHelper.lookup(layout, "#speed");
        speederr = LookupHelper.lookup(layout, "#speedErr");
        speedon = LookupHelper.lookup(layout, "#speedOn");
        speedtext = LookupHelper.lookup(layout, "#speed-text");
        cancel = LookupHelper.lookup(layout, "#cancel");
        volume = LookupHelper.lookup(layout, "#volume");
        logOutput = LookupHelper.lookup(layout, "#outputUpdate");
        downloader = new VisualDownloader(application, progressBar, speed, volume,
                                          (log) -> contextHelper.runInFxThread(() -> addLog(log)));
        LookupHelper.<ButtonBase>lookup(layout, "#cancel").setOnAction((e) -> {
            downloader.cancel();
            try {
                switchToBackScene();
            } catch (Exception exception) {
                errorHandle(exception);
            }
            /*if (downloadStatus == DownloadStatus.DOWNLOAD && downloader.isDownload()) {
                downloader.cancel();
            } else if(downloadStatus == DownloadStatus.ERROR || downloadStatus == DownloadStatus.COMPLETE) {
                try {
                    switchToBackScene();
                } catch (Exception exception) {
                    errorHandle(exception);
                }
            }*/
        });
    }

    public LauncherBackendAPI.DownloadCallback makeDownloadCallback() {
        return downloader;
    }

    public void addLog(String string) {
        LogHelper.dev("Update event %s", string);
        logOutput.appendText(string.concat("\n"));
    }

    @Override
    public void reset() {
        progressBar.progressProperty().setValue(0);
        logOutput.setText("");
        volume.setText("");
        speed.setText("0");
        progressBar.getStyleClass().removeAll("progress");
        speederr.setVisible(false);
        speedon.setVisible(true);
    }

    @Override
    public void errorHandle(Throwable e) {
        if(e instanceof CompletionException) {
            e = e.getCause();
        }
        addLog("Exception %s: %s".formatted(e.getClass(), e.getMessage() == null ? "" : e.getMessage()));
        progressBar.getStyleClass().add("progressError");
        speederr.setVisible(true);
        speedon.setVisible(false);
        LogHelper.error(e);
    }

    @Override
    public boolean isDisableReturnBack() {
        return true;
    }

    @Override
    public String getName() {
        return "update";
    }

    public enum DownloadStatus {
        ERROR, HASHING, REQUEST, DOWNLOAD, COMPLETE, DELETE
    }
}
