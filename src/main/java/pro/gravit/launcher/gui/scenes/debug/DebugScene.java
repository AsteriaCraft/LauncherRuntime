package pro.gravit.launcher.gui.scenes.debug;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.JavaRuntimeModule;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.core.impl.FxScene;
import pro.gravit.utils.helper.LogHelper;

import java.io.EOFException;

public class DebugScene extends FxScene {
    private volatile ProcessLogOutput processLogOutput;
    private TextArea output;

    public DebugScene(JavaFXApplication application) {
        super("scenes/debug/debug.fxml", application);
        this.isResetOnShow = true;
    }

    @Override
    protected void doInit() {
        output = LookupHelper.lookup(layout, "#output");
        processLogOutput = new ProcessLogOutput(output);
        LookupHelper.<ButtonBase>lookupIfPossible(header, "#controls", "#kill").ifPresent((x) -> x.setOnAction((e) -> {
            processLogOutput.terminate();
        }));

        LookupHelper.<Label>lookupIfPossible(layout, "#version")
                    .ifPresent((v) -> v.setText(JavaRuntimeModule.getMiniLauncherInfo()));
        LookupHelper.<ButtonBase>lookupIfPossible(header, "#controls", "#copy").ifPresent((x) -> x.setOnAction((e) -> processLogOutput.copyToClipboard()));
        LookupHelper.<ButtonBase>lookup(header, "#back").setOnAction((e) -> {
            processLogOutput.detach();
            processLogOutput = new ProcessLogOutput(output);
            try {
                switchToBackScene();
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });
    }


    @Override
    public void reset() {
        processLogOutput.clear();
    }

    public void run(LauncherBackendAPI.ReadyProfile readyProfile) {
        try {
            readyProfile.run(processLogOutput);
        } catch (Throwable e) {
            errorHandle(e);
        }
    }

    public void append(String text) {
        processLogOutput.append(text);
    }

    @Override
    public void errorHandle(Throwable e) {
        if (!(e instanceof EOFException)) {
            processLogOutput.append(e.toString());
        }
    }

    @Override
    public String getName() {
        return "debug";
    }
}
