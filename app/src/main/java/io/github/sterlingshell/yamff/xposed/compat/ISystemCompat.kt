package io.github.sterlingshell.yamff.xposed.compat

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.view.InputEvent
import android.view.IWindowManager
import android.view.Surface

interface ISystemCompat {
    fun setDisplayId(event: InputEvent, displayId: Int)
    fun getDisplayId(obj: Any): Int
    fun getTaskId(obj: Any): Int
    fun setFrameRate(surface: Surface, fps: Float)
    fun setFocusedDisplay(iwm: IWindowManager, displayId: Int)
    fun createParceledListSlice(list: List<ActivityManager.RecentTaskInfo>): Any?
    fun getRecentTasksList(parceledListSlice: Any): List<ActivityManager.RecentTaskInfo>?
    fun setCallerDisplayId(options: ActivityOptions, displayId: Int)
    fun getTargetComponent(itemInfo: Any): ComponentName?
    fun getUserHandle(itemInfo: Any): Any? // Returns UserHandle
}
