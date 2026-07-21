package io.github.duzhaokun123.yamf.xposed.hooks

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.IPackageManager
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.github.kyuubiran.ezxhelper.utils.paramCount
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.duzhaokun123.yamf.BuildConfig
import io.github.duzhaokun123.yamf.xposed.services.YAMFServer
import io.github.duzhaokun123.yamf.xposed.services.YAMFIpcService
import io.github.duzhaokun123.yamf.xposed.services.YAMFWindowManager
import io.github.duzhaokun123.yamf.xposed.utils.extensions.getDisplayIdSafe
import io.github.duzhaokun123.yamf.xposed.utils.extensions.isTaskInYAMF
import io.github.duzhaokun123.yamf.xposed.utils.extensions.log
import io.github.qauxv.util.Initiator
import kotlin.concurrent.thread

class HookSystem : IXposedHookZygoteInit, IXposedHookLoadPackage {
    companion object {
        private const val TAG = "YAMF_HookSystem"
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return
        log(TAG, "xposed init")
        log(TAG, "buildtype: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}")
        EzXHelperInit.initHandleLoadPackage(lpparam)
        Initiator.init(lpparam.classLoader)

         var serviceManagerHook: XC_MethodHook.Unhook? = null
         serviceManagerHook = findMethodOrNull("android.os.ServiceManager") {
             name == "addService"
         }?.hookBefore { param ->
             if (param.args[0] == "package") {
                 serviceManagerHook?.unhook()
                 val pms = param.args[1] as IPackageManager
                 log(TAG, "Got pms: $pms")
                 thread {
                     runCatching {
                         YAMFIpcService.register(pms)
                         log(TAG, "YAMFIpcService started")
                     }.onFailure {
                         log(TAG, "YAMFIpcService failed to start", it)
                     }
                 }
             }
         }

         var activityManagerServiceSystemReadyHook: XC_MethodHook.Unhook? = null
         activityManagerServiceSystemReadyHook = findMethodOrNull("com.android.server.am.ActivityManagerService") {
             name == "systemReady"
         }?.hookAfter {
             activityManagerServiceSystemReadyHook?.unhook()
             YAMFServer.activityManagerService = it.thisObject
             YAMFServer.systemReady()
             log(TAG, "system ready")
         }

        findMethodOrNull("com.android.server.am.ActivityManagerService") {
            name == "checkBroadcastFromSystem"
        }?.hookBefore {
            val intent = it.args[0] as Intent
            if (intent.action == HookLauncher.ACTION_RECEIVE_LAUNCHER_CONFIG)
                it.result = Unit // bypass check
        }
        
        hookWindowLogic(lpparam)
    }
    
    private fun hookWindowLogic(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classRecentTasks = loadClass("com.android.server.wm.RecentTasks")
        val classActivityRecord = loadClass("com.android.server.wm.ActivityRecord")
        val classTask = loadClass("com.android.server.wm.Task")

        // Hide from Recents (Low level)
        classTask.findMethodOrNull { name == "isIncludedInRecents" }?.hookBefore { param ->
            if (param.thisObject.isTaskInYAMF()) {
                param.result = false
            }
        }

        classTask.findMethodOrNull { name == "shouldBeVisible" }?.hookBefore { param ->
            if (param.thisObject.isTaskInYAMF()) {
                param.result = false
            }
        }

        classActivityRecord.findMethodOrNull { name == "isIncludedInRecents" }?.hookBefore { param ->
            val displayId = param.thisObject.getDisplayIdSafe()
            if (displayId != 0 && YAMFWindowManager.getWindowList().contains(displayId)) {
                param.result = false
            }
        }
        
        classRecentTasks.findMethodOrNull { name == "isVisibleRecentTask" }?.hookBefore { param ->
            if (param.args[0].isTaskInYAMF()) {
                param.result = false
            }
        }

        // Hide from Recents (List level)
        classRecentTasks.findAllMethods { name == "getRecentTasks" }.hookAfter { param ->
            val result = param.result ?: return@hookAfter
            val list = result.invokeMethodAutoAs<List<ActivityManager.RecentTaskInfo>>("getList") ?: return@hookAfter
            val filteredList = list.filter { info ->
                val displayId = runCatching { info.getObjectAs<Int>("displayId") }.getOrDefault(0)
                val inWindow = YAMFWindowManager.isTaskInWindow(info.taskId) || YAMFWindowManager.getWindowList().contains(displayId)
                !inWindow
            }
            if (filteredList.size != list.size) {
                val classParceledListSlice = loadClass("android.content.pm.ParceledListSlice")
                param.result = classParceledListSlice.findConstructor { paramCount == 1 }.newInstance(filteredList)
            }
        }

        // Multi-Resume
        runCatching {
            classActivityRecord.findMethodOrNull { name == "shouldBeResumed" }?.hookBefore { param ->
                val displayId = param.thisObject.getDisplayIdSafe()
                if (YAMFWindowManager.getWindowList().contains(displayId)) {
                    param.result = true
                }
            }
        }.onFailure { e ->
            log(TAG, "hook shouldBeResumed failed", e)
        }
    }
}
