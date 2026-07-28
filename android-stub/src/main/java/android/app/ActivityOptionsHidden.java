package android.app;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityOptions.class)
public interface ActivityOptionsHidden {
    void setCallerDisplayId(int displayId);
}
