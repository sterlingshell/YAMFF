package io.github.sterlingshell.yamff.xposed.compat

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.os.Build
import android.view.InputEvent
import android.view.IWindowManager
import android.view.Surface
import com.github.kyuubiran.ezxhelper.utils.*

class SystemCompatImpl : ISystemCompat {
    override fun setDisplayId(event: InputEvent, displayId: Int) {
        runCatching {
            event.invokeMethod("setDisplayId", args(displayId), argTypes(Int::class.java))
        }
    }

    override fun getDisplayId(obj: Any): Int {
        return runCatching { obj.invokeMethodAutoAs<Int>("getDisplayId") }.getOrNull()
            ?: runCatching { obj.getObjectAs<Int>("mDisplayId") }.getOrNull()
            ?: runCatching { obj.getObjectAs<Int>("displayId") }.getOrNull()
            ?: 0
    }

    override fun getTaskId(obj: Any): Int {
        return runCatching { obj.getObjectAs<Int>("mTaskId") }.getOrNull()
            ?: runCatching { obj.invokeMethodAutoAs<Int>("getTaskId") }.getOrNull()
            ?: runCatching { obj.getObjectAs<Int>("taskId") }.getOrNull()
            ?: -1
    }

    override fun setFrameRate(surface: Surface, fps: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                surface.invokeMethod("setFrameRate", args(fps, 0), argTypes(Float::class.java, Int::class.java))
            }
        }
    }

    override fun setFocusedDisplay(iwm: IWindowManager, displayId: Int) {
        runCatching {
            iwm.invokeMethod("setFocusedDisplay", args(displayId), argTypes(Int::class.java))
        }
    }

    override fun createParceledListSlice(list: List<ActivityManager.RecentTaskInfo>): Any? {
        return runCatching {
            val clazz = loadClass("android.content.pm.ParceledListSlice")
            clazz.findConstructor { paramCount == 1 }.newInstance(list)
        }.getOrNull()
    }

    override fun getRecentTasksList(parceledListSlice: Any): List<ActivityManager.RecentTaskInfo>? {
        return runCatching {
            parceledListSlice.invokeMethodAutoAs<List<ActivityManager.RecentTaskInfo>>("getList")
        }.getOrNull()
    }

    override fun setCallerDisplayId(options: ActivityOptions, displayId: Int) {
        runCatching {
            options.invokeMethod("setCallerDisplayId", args(displayId), argTypes(Int::class.java))
        }
    }

    override fun getTargetComponent(itemInfo: Any): ComponentName? {
        return runCatching {
            itemInfo.invokeMethod("getTargetComponent") as? ComponentName
        }.getOrNull()
    }

    override fun getUserHandle(itemInfo: Any): Any? {
        return runCatching {
            itemInfo.getObject("user") // Typically Launcher's ItemInfo has 'user' field
        }.getOrNull()
    }
}
