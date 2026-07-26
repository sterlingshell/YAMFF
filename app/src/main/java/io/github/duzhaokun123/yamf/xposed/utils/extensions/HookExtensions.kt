package io.github.duzhaokun123.yamf.xposed.utils.extensions

import io.github.duzhaokun123.yamf.xposed.compat.SystemCompat
import io.github.duzhaokun123.yamf.xposed.services.YAMFWindowManager

fun Any.getDisplayIdSafe(): Int {
    return SystemCompat.getDisplayId(this)
}

fun Any.getTaskIdSafe(): Int {
    return SystemCompat.getTaskId(this)
}

fun Any.isTaskInYAMF(): Boolean {
    val displayId = this.getDisplayIdSafe()
    val taskId = this.getTaskIdSafe()
    return (displayId != 0 && YAMFWindowManager.getWindowList().contains(displayId)) || YAMFWindowManager.isTaskInWindow(taskId)
}
