package pro.gravit.launcher.gui.core.interfaces;

import pro.gravit.launcher.gui.core.impl.UIComponent;

public interface ValueComponent<T> extends UIComponent {
    void onValue(T value);
}
