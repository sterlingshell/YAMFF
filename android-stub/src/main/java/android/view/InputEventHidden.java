package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(InputEvent.class)
public interface InputEventHidden {
    void setDisplayId(int displayId);
    int getDisplayId();
}
