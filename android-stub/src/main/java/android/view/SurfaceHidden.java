package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(Surface.class)
public interface SurfaceHidden {
    void setFrameRate(float frameRate, int compatibility);
}
