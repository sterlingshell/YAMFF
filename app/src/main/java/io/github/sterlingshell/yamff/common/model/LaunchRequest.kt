package io.github.sterlingshell.yamff.common.model

import android.content.ComponentName
import android.graphics.Rect

data class LaunchRequest(
    val componentName: ComponentName? = null,
    val userId: Int? = null,
    val taskId: Int? = null,
    val startRect: Rect? = null
) {
    val canStartActivity
        get() = componentName != null && userId != null

    val canMoveTask
        get() = taskId != null && taskId != 0
}
