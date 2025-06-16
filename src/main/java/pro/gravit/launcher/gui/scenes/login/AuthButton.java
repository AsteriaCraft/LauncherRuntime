package pro.gravit.launcher.gui.scenes.login;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import pro.gravit.launcher.gui.core.JavaFXApplication;
import pro.gravit.launcher.gui.core.impl.FxComponent;
import pro.gravit.launcher.gui.helper.LookupHelper;

public class AuthButton extends FxComponent {
    private Button button;
    private AuthButtonState state = AuthButtonState.UNACTIVE;
    private String originalText;

    @Override
    protected void doInit() {
        button = LookupHelper.lookup(layout, "#authButton");
        originalText = button.getText();
    }

    @Override
    public String getName() {
        return "authButton";
    }

    @Override
    public void reset() {

    }

    public enum AuthButtonState {
        ACTIVE("activeButton"), UNACTIVE("unactiveButton"), ERROR("errorButton");
        private final String styleClass;

        public String getStyleClass() {
            return styleClass;
        }

        AuthButtonState(String styleClass) {
            this.styleClass = styleClass;
        }
    }

    public AuthButton(Pane layout, JavaFXApplication application) {
        super(layout, application);
    }

    public void setOnAction(EventHandler<ActionEvent> eventHandler) {
        this.button.setOnAction(eventHandler);
    }

    public AuthButtonState getState() {
        return state;
    }

    public void setState(AuthButtonState state) {
        if(state == null) {
            throw new NullPointerException("State can't be null");
        }
        if(state == this.state) {
            return;
        }
        if(this.state != null) {
            button.getStyleClass().remove(this.state.getStyleClass());
        }
        button.getStyleClass().add(state.getStyleClass());
        if(state == AuthButtonState.ERROR) {
            button.setText("ERROR");
        } else if(this.state == AuthButtonState.ERROR) {
            button.setText(originalText);
        }
        this.state = state;
    }

    public String getText() {
        return button.getText();
    }

    public void setText(String text) {
        button.setText(text);
        originalText = text;
    }
}
