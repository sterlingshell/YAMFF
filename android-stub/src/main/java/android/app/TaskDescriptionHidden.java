package android.app;

import android.graphics.Bitmap;
import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityManager.TaskDescription.class)
public interface TaskDescriptionHidden {
    String getLabel();
    Bitmap getIcon();
    int getPrimaryColor();
    int getBackgroundColor();
    int getStatusBarColor();
    int getNavigationBarColor();
}
