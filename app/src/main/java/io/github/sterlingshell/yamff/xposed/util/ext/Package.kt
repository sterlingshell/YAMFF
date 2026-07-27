package io.github.sterlingshell.yamff.xposed.util.ext

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.IPackageManagerHidden
import android.os.Build

val ActivityInfo.componentName: ComponentName
    get() = ComponentName(packageName, name)

fun IPackageManagerHidden.getActivityInfoCompat(className: ComponentName, flags: Int, userId: Int): ActivityInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getActivityInfo(className, flags.toLong(), userId)
    } else {
        getActivityInfo(className, flags, userId)
    }
