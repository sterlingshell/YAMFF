package android.app;

import java.util.List;
import dev.rikka.tools.refine.RefineAs;

@RefineAs(ActivityManager.class)
public interface ActivityManagerHidden {
    int UID_OBSERVER_ACTIVE = 1 << 3;
    int PROCESS_STATE_UNKNOWN = -1;

    List<ActivityManager.RunningTaskInfo> getRunningTasks(int maxNum);
}
