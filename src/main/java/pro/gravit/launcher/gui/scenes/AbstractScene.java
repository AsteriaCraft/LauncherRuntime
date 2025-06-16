package pro.gravit.launcher.gui.scenes;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.core.api.LauncherAPIHolder;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.gui.components.BasicUserControls;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.impl.AbstractStage;
import pro.gravit.launcher.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.gui.impl.ContextHelper;
import pro.gravit.launcher.gui.overlays.AbstractOverlay;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class AbstractScene extends AbstractVisualComponent {
    protected final LauncherConfig launcherConfig;
    protected Pane header;
    protected BasicUserControls basicUserControls;

    protected AbstractScene(String fxmlPath, JavaFXApplication application) {
        super(fxmlPath, application);
        this.launcherConfig = Launcher.getConfig();
    }

    protected AbstractStage getCurrentStage() {
        return currentStage;
    }

    public void init() throws Exception {
        layout = (Pane) getFxmlRoot();
        header = (Pane) LookupHelper.lookupIfPossible(layout, "#header").orElse(null);
        sceneBaseInit();
        super.init();
    }

    protected abstract void doInit();

    @Override
    protected void doPostInit() {

    }

    protected void sceneBaseInit() {
        basicUserControls = use(header, BasicUserControls::new);
        currentStage.enableMouseDrag(layout);
    }

    public void disable() {
        currentStage.disable();
    }

    public void enable() {
        currentStage.enable();
    }

    public abstract void reset();

    public Node getHeader() {
        return header;
    }

    public static void runLater(double delay, EventHandler<ActionEvent> callback) {
        fade(null, delay, 0.0, 1.0, callback);
    }

    public class SceneAccessor {
        public SceneAccessor() {
        }


        public void showOverlay(AbstractOverlay overlay, EventHandler<ActionEvent> onFinished) throws Exception {
            AbstractScene.this.showOverlay(overlay, onFinished);
        }

        public JavaFXApplication getApplication() {
            return application;
        }

        public void errorHandle(Throwable e) {
            AbstractScene.this.errorHandle(e);
        }

        public void runInFxThread(ContextHelper.GuiExceptionRunnable runnable) {
            contextHelper.runInFxThread(runnable);
        }

        public <T> void processRequest(String message, CompletableFuture<T> request,
                Consumer<T> onSuccess, EventHandler<ActionEvent> onError) {
            AbstractScene.this.processRequest(message, request, onSuccess, onError);
        }

        public final <T> void processRequest(String message, CompletableFuture<T> request,
                Consumer<T> onSuccess, Consumer<Throwable> onException, EventHandler<ActionEvent> onError) {
            AbstractScene.this.processRequest(message, request, onSuccess, onException, onError);
        }
    }
}
