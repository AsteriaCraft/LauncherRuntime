package pro.gravit.launcher.gui.impl;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.gui.JavaFXApplication;
import pro.gravit.launcher.gui.helper.LookupHelper;
import pro.gravit.launcher.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.gui.scenes.AbstractScene;
import pro.gravit.utils.helper.LogHelper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class AbstractVisualComponent {
    protected final JavaFXApplication application;
    protected final ContextHelper contextHelper;
    protected final FXExecutorService fxExecutor;
    protected AbstractStage currentStage;
    protected Pane layout;
    private final String sysFxmlPath;
    private Parent sysFxmlRoot;
    private CompletableFuture<Node> sysFxmlFuture;
    boolean isInit;
    boolean isPostInit;
    protected boolean isResetOnShow = false;

    protected AbstractVisualComponent(String fxmlPath, JavaFXApplication application) {
        this.application = application;
        this.sysFxmlPath = fxmlPath;
        this.contextHelper = new ContextHelper(this);
        this.fxExecutor = new FXExecutorService(contextHelper);
        if (application.guiModuleConfig.lazy) {
            this.sysFxmlFuture = application.fxmlFactory.getAsync(sysFxmlPath);
        }
    }

    protected AbstractVisualComponent(Pane layout, JavaFXApplication application) {
        this.application = application;
        this.sysFxmlPath = null;
        this.contextHelper = new ContextHelper(this);
        this.layout = layout;
        this.fxExecutor = new FXExecutorService(contextHelper);
    }

    public static FadeTransition fade(Node region, double delay, double from, double to,
            EventHandler<ActionEvent> onFinished) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(100), region);
        if (onFinished != null) fadeTransition.setOnFinished(onFinished);
        fadeTransition.setDelay(Duration.millis(delay));
        fadeTransition.setFromValue(from);
        fadeTransition.setToValue(to);
        fadeTransition.play();
        return fadeTransition;
    }

    protected<T> Void errorHandle(T value, Throwable e) {
        errorHandle(e);
        return null;
    }

    public Pane getLayout() {
        return layout;
    }

    public boolean isInit() {
        return isInit;
    }

    public abstract String getName();

    protected synchronized Parent getFxmlRoot() {
        try {
            if (sysFxmlRoot == null) {
                if (sysFxmlFuture == null) {
                    this.sysFxmlFuture = application.fxmlFactory.getAsync(sysFxmlPath);
                }
                sysFxmlRoot = (Parent) sysFxmlFuture.get();
            }
            return sysFxmlRoot;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CompletionException) {
                cause = cause.getCause();
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new FXMLFactory.FXMLLoadException(cause);
        }
    }

    protected<T extends AbstractVisualComponent> T use(Pane layout, BiFunction<Pane, JavaFXApplication, T> constructor) {
        T object = constructor.apply(layout, application);
        try {
            object.currentStage = currentStage;
            object.init();
            return object;
        } catch (Throwable e) {
            errorHandle(e);
        }
        return null;
    }

    public void init() throws Exception {
        if (layout == null) {
            layout = (Pane) getFxmlRoot();
        }
        doInit();
        isInit = true;
    }

    public void postInit() throws Exception {
        if (!isPostInit) {
            doPostInit();
            isPostInit = true;
        }
    }

    protected abstract void doInit();

    protected void doPostInit() {

    }

    public abstract void reset();

    public void disable() {

    }

    public void enable() {

    }

    protected void onShow() {

    }

    protected void onHide() {

    }

    protected void switchScene(AbstractScene scene) throws Exception {
        currentStage.setScene(scene, true);
        if(currentStage.getVisualComponent() != null) {
            currentStage.getVisualComponent().onHide();
            scene.onShow();
        }
    }

    protected void switchToBackScene() throws Exception {
        currentStage.back();
    }

    public void errorHandle(Throwable e) {
        String message = null;
        if (e instanceof CompletionException) {
            e = e.getCause();
        }
        if (e instanceof ExecutionException) {
            e = e.getCause();
        }
        if (e instanceof RequestException) {
            message = e.getMessage();
        }
        if (message == null) {
            message = "%s: %s".formatted(e.getClass().getName(), e.getMessage());
        } else {
            message = application.getTranslation("runtime.request.".concat(message), message);
        }
        LogHelper.error(e);
        application.messageManager.createNotification("Error", message);
    }

    protected Parent getFxmlRootPrivate() {
        return getFxmlRoot();
    }

    public boolean isDisableReturnBack() {
        return false;
    }

    public void showOverlay(AbstractOverlay overlay, EventHandler<ActionEvent> onFinished) throws Exception {
        overlay.show(currentStage, onFinished);
    }

    protected final <T> void processRequest(String message, CompletableFuture<T> request,
            Consumer<T> onSuccess, EventHandler<ActionEvent> onError) {
        application.gui.processingOverlay.processRequest(currentStage, message, request, onSuccess, onError);
    }

    protected final <T> void processRequest(String message, CompletableFuture<T> request,
            Consumer<T> onSuccess, Consumer<Throwable> onException, EventHandler<ActionEvent> onError) {
        application.gui.processingOverlay.processRequest(currentStage, message, request, onSuccess, onException, onError);
    }
}
