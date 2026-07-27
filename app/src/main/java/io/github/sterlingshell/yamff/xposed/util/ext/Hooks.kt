package io.github.sterlingshell.yamff.xposed.util.ext

import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.xposed.core.FreeformManager

fun Any.getDisplayIdSafe(): Int {
    return SystemCompat.getDisplayId(this)
}

fun Any.getTaskIdSafe(): Int {
    return SystemCompat.getTaskId(this)
}

fun Any.isTaskInFreeform(): Boolean {
    val displayId = this.getDisplayIdSafe()
    val taskId = this.getTaskIdSafe()
    return (displayId != 0 && FreeformManager.getWindowList().contains(displayId)) || FreeformManager.isTaskInWindow(taskId)
}
