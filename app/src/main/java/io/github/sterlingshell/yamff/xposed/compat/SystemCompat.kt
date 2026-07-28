package io.github.sterlingshell.yamff.xposed.compat

import android.app.ActivityManager
import android.app.ActivityManagerHidden
import android.app.ActivityOptions
import android.app.ActivityOptionsHidden
import android.app.RecentTaskInfoHidden
import android.content.ComponentName
import android.content.pm.IPackageManagerHidden
import android.content.pm.ParceledListSliceHidden
import android.os.Build
import android.os.UserHandle
import android.view.InputEvent
import android.view.InputEventHidden
import android.view.IWindowManager
import android.view.Surface
import android.view.SurfaceHidden
import com.github.kyuubiran.ezxhelper.utils.*
import dev.rikka.tools.refine.Refine
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import io.github.sterlingshell.yamff.xposed.sys.SystemServices

object SystemCompat {
    private const val TAG = "SystemCompat"

    fun setDisplayId(event: InputEvent, displayId: Int) {
        try {
            Refine.unsafeCast<InputEventHidden>(event).setDisplayId(displayId)
        } catch (t: Throwable) {
            log(TAG, "Refine setDisplayId failed, falling back to reflection", t)
            runCatching {
                event.invokeMethod("setDisplayId", args(displayId), argTypes(Int::class.java))
            }
        }
    }

    fun getDisplayId(obj: Any): Int {
        return try {
            Refine.unsafeCast<RecentTaskInfoHidden>(obj).displayId
        } catch (t: Throwable) {
            runCatching { obj.invokeMethodAutoAs<Int>("getDisplayId") }.getOrNull()
                ?: runCatching { obj.getObjectAs<Int>("mDisplayId") }.getOrNull()
                ?: runCatching { obj.getObjectAs<Int>("displayId") }.getOrNull()
                ?: 0
        }
    }

    fun getTaskId(obj: Any): Int {
        return try {
            Refine.unsafeCast<RecentTaskInfoHidden>(obj).taskId
        } catch (t: Throwable) {
            runCatching { obj.getObjectAs<Int>("mTaskId") }.getOrNull()
                ?: runCatching { obj.invokeMethodAutoAs<Int>("getTaskId") }.getOrNull()
                ?: runCatching { obj.getObjectAs<Int>("taskId") }.getOrNull()
                ?: -1
        }
    }

    fun setFrameRate(surface: Surface, fps: Float) {
        try {
            Refine.unsafeCast<SurfaceHidden>(surface).setFrameRate(fps, 0)
        } catch (t: Throwable) {
            log(TAG, "Refine setFrameRate failed, falling back to reflection", t)
            runCatching {
                surface.invokeMethod("setFrameRate", args(fps, 0), argTypes(Float::class.java, Int::class.java))
            }
        }
    }

    fun setFocusedDisplay(iwm: IWindowManager, displayId: Int) {
        try {
            iwm.setFocusedDisplay(displayId)
        } catch (t: Throwable) {
            log(TAG, "Refine setFocusedDisplay failed, falling back to reflection", t)
            runCatching {
                iwm.invokeMethod("setFocusedDisplay", args(displayId), argTypes(Int::class.java))
            }
        }
    }

    fun createParceledListSlice(list: List<ActivityManager.RecentTaskInfo>): Any? {
        return runCatching {
            val clazz = loadClass("android.content.pm.ParceledListSlice")
            clazz.findConstructor { paramCount == 1 }.newInstance(list)
        }.getOrNull()
    }

    fun getRecentTasksList(parceledListSlice: Any): List<ActivityManager.RecentTaskInfo>? {
        return try {
            Refine.unsafeCast<ParceledListSliceHidden<ActivityManager.RecentTaskInfo>>(parceledListSlice).list
        } catch (t: Throwable) {
            runCatching {
                parceledListSlice.invokeMethodAutoAs<List<ActivityManager.RecentTaskInfo>>("getList")
            }.getOrNull()
        }
    }

    fun setCallerDisplayId(options: ActivityOptions, displayId: Int) {
        try {
            Refine.unsafeCast<ActivityOptionsHidden>(options).setCallerDisplayId(displayId)
        } catch (t: Throwable) {
            log(TAG, "Refine setCallerDisplayId failed, falling back to reflection", t)
            runCatching {
                options.invokeMethod("setCallerDisplayId", args(displayId), argTypes(Int::class.java))
            }
        }
    }

    fun getTargetComponent(itemInfo: Any): ComponentName? {
        return try {
            Refine.unsafeCast<RecentTaskInfoHidden>(itemInfo).targetComponent
        } catch (t: Throwable) {
            runCatching { itemInfo.invokeMethod("getTargetComponent") as? ComponentName }.getOrNull()
        }
    }

    fun getUserHandle(itemInfo: Any): UserHandle? {
        return try {
            Refine.unsafeCast<RecentTaskInfoHidden>(itemInfo).user
        } catch (t: Throwable) {
            runCatching { itemInfo.getObject("user") as? UserHandle }.getOrNull()
        }
    }

    fun getTaskDescription(taskId: Int): ActivityManager.TaskDescription? {
        val taskDescription = try {
            SystemServices.activityTaskManager.getTaskDescription(taskId)
        } catch (t: Throwable) {
            log(TAG, "getTaskDescription direct failed", t)
            null
        }

        if (taskDescription == null && Build.VERSION.SDK_INT >= 35) {
            // Android 15 fallback
            val runningTasks = try {
                Refine.unsafeCast<ActivityManagerHidden>(SystemServices.activityManager).getRunningTasks(10)
            } catch (t: Throwable) {
                // Final fallback to direct public method
                runCatching { SystemServices.activityManager.getRunningTasks(10) }.getOrNull()
            }

            runningTasks?.forEach { task ->
                if (task.taskId == taskId) return task.taskDescription
            }
        }
        return taskDescription
    }

    fun isTaskInFreeform(task: Any): Boolean {
        val taskId = getTaskId(task)
        val displayId = getDisplayId(task)
        return FreeformManager.isTaskInWindow(taskId) || FreeformManager.getWindowList().contains(displayId)
    }

    private fun log(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) {
            io.github.sterlingshell.yamff.xposed.util.ext.log(tag, msg, t)
        } else {
            io.github.sterlingshell.yamff.xposed.util.ext.log(tag, msg)
        }
    }
}
