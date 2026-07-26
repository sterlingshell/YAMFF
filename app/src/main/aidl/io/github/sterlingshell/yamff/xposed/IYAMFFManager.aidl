package io.github.sterlingshell.yamff.xposed;

import android.view.Surface;
import io.github.sterlingshell.yamff.xposed.IOpenCountListener;

interface IYAMFFManager {
    String getVersionName();

    int getVersionCode();

    int getUid();

    void createWindow();

    long getBuildTime();

    String getConfigJson();

    void updateConfig(String newConfig);

    void registerOpenCountListener(IOpenCountListener iOpenCountListener);

    void unregisterOpenCountListener(IOpenCountListener iOpenCountListener);

    void openAppList();

    void currentToWindow();

    void resetAllWindow();
}