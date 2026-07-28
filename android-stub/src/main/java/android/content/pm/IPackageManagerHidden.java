package android.content.pm;

import android.content.ComponentName;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(IPackageManager.class)
public interface IPackageManagerHidden {
    @RequiresApi(33)
    ActivityInfo getActivityInfo(ComponentName className, long flags, int userId)
            throws RemoteException;

    ActivityInfo getActivityInfo(ComponentName className, int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    PackageInfo getPackageInfo(String packageName, long flags, int userId)
            throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException;

    int checkPermission(String permName, String pkgName, int userId)
            throws RemoteException;

    int getPackageUid(String packageName, int flags, int userId)
            throws RemoteException;

    @RequiresApi(33)
    int getPackageUid(String packageName, long flags, int userId)
            throws RemoteException;

    String[] getPackagesForUid(int uid)
            throws RemoteException;
}
