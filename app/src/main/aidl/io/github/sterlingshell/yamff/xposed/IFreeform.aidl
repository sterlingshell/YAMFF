package io.github.sterlingshell.yamff.xposed;

import android.view.Surface;
import io.github.sterlingshell.yamff.xposed.IOpenCountListener;
import io.github.sterlingshell.yamff.xposed.IConfigChangeListener;
import io.github.sterlingshell.yamff.xposed.IExtensionsChangeListener;

interface IFreeform {
    String getVersionName();
    int getVersionCode();
    int getUid();
    void createWindow();
    long getBuildTime();
    String getConfigJson();
    void updateConfig(String newConfig);
    void registerOpenCountListener(IOpenCountListener iOpenCountListener);
    void unregisterOpenCountListener(IOpenCountListener iOpenCountListener);
    void registerConfigChangeListener(IConfigChangeListener listener);
    void unregisterConfigChangeListener(IConfigChangeListener listener);
    void registerExtensionsChangeListener(IExtensionsChangeListener listener);
    void unregisterExtensionsChangeListener(IExtensionsChangeListener listener);
    void openAppList();
    void currentToWindow();
    void resetAllWindow();
    String getExtensionsJson();
    void updateExtensions(String newConfig);
}
