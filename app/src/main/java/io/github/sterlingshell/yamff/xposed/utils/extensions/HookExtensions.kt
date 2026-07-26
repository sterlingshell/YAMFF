package io.github.sterlingshell.yamff.xposed.utils.extensions

import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.xposed.services.YAMFFWindowManager

fun Any.getDisplayIdSafe(): Int {
    return SystemCompat.getDisplayId(this)
}

fun Any.getTaskIdSafe(): Int {
    return SystemCompat.getTaskId(this)
}

fun Any.isTaskInYAMFF(): Boolean {
    val displayId = this.getDisplayIdSafe()
    val taskId = this.getTaskIdSafe()
    return (displayId != 0 && YAMFFWindowManager.getWindowList().contains(displayId)) || YAMFFWindowManager.isTaskInWindow(taskId)
}
