package android.app;

import android.content.ComponentName;
import android.os.UserHandle;
import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityManager.RecentTaskInfo.class)
public interface RecentTaskInfoHidden {
    int getDisplayId();
    int getTaskId();
    UserHandle getUser();
    ComponentName getTargetComponent();
}
