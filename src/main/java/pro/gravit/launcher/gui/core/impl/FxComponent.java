package pro.gravit.launcher.gui.core.impl;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.core.interfaces.ValueComponent;
import pro.gravit.launcher.gui.core.internal.FXExecutorService;
import pro.gravit.launcher.gui.core.internal.FXMLFactory;
import pro.gravit.utils.helper.LogHelper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class FxComponent extends VisualComponentBase {
    protected final JavaFXApplication application;
    protected final ContextHelper contextHelper;
    protected final FXExecutorService fxExecutor;
    protected FxStage currentStage;
    private final String sysFxmlPath;
    private Parent sysFxmlRoot;
    private CompletableFuture<Node> sysFxmlFuture;
    protected boolean isResetOnShow = false;

    protected FxComponent(String fxmlPath, JavaFXApplication application) {
        this.application = application;
        this.sysFxmlPath = fxmlPath;
        this.contextHelper = new ContextHelper(this);
        this.fxExecutor = new FXExecutorService(contextHelper);
        if (application.guiModuleConfig.lazy) {
            this.sysFxmlFuture = application.fxmlFactory.getAsync(sysFxmlPath);
        }
    }

    protected FxComponent(Pane layout, JavaFXApplication application) {
        this.application = application;
        this.sysFxmlPath = null;
        this.contextHelper = new ContextHelper(this);
        this.layout = layout;
        this.fxExecutor = new FXExecutorService(contextHelper);
    }

    protected<T> Void errorHandle(T value, Throwable e) {
        errorHandle(e);
        return null;
    }

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

    protected<T extends UIComponent> T use(Pane layout, BiFunction<Pane, JavaFXApplication, T> constructor) {
        T object = constructor.apply(layout, application);
        try {
            if(object instanceof FxComponent fxComponent) {
                fxComponent.currentStage = currentStage;
            }
            object.init();
            return object;
        } catch (Throwable e) {
            errorHandle(e);
        }
        return null;
    }

    protected<T extends ValueComponent<V>, V> T use(Pane layout, BiFunction<Pane, JavaFXApplication, T> constructor, V value) {
        T object = use(layout, constructor);
        if(object != null) {
            object.onValue(value);
        }
        return object;
    }

    @Override
    public void init() throws Exception {
        if (layout == null) {
            layout = (Pane) getFxmlRoot();
        }
        doInit();
        isInit = true;
    }

    protected void switchScene(FxScene scene) throws Exception {
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

    public void showOverlay(FxOverlay overlay, EventHandler<ActionEvent> onFinished) throws Exception {
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
