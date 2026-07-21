package io.github.duzhaokun123.yamf.xposed.utils.extensions

import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import io.github.duzhaokun123.yamf.xposed.services.YAMFWindowManager
import com.github.kyuubiran.ezxhelper.utils.getObjectAs

fun Any.getDisplayIdSafe(): Int {
    return runCatching { this.invokeMethodAutoAs<Int>("getDisplayId") }.getOrNull()
        ?: runCatching { this.getObjectAs<Int>("mDisplayId") }.getOrNull()
        ?: 0
}

fun Any.getTaskIdSafe(): Int {
    return runCatching { this.getObjectAs<Int>("mTaskId") }.getOrNull()
        ?: runCatching { this.invokeMethodAutoAs<Int>("getTaskId") }.getOrNull()
        ?: -1
}

fun Any.isTaskInYAMF(): Boolean {
    val displayId = this.getDisplayIdSafe()
    val taskId = this.getTaskIdSafe()
    return (displayId != 0 && YAMFWindowManager.getWindowList().contains(displayId)) || YAMFWindowManager.isTaskInWindow(taskId)
}
