package io.github.sterlingshell.yamff.xposed.hooks

import android.content.Intent
import android.content.pm.IPackageManager
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.xposed.core.IpcService
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import io.github.sterlingshell.yamff.xposed.core.IpcEntry
import io.github.sterlingshell.yamff.xposed.util.ext.getDisplayIdSafe
import io.github.sterlingshell.yamff.xposed.util.ext.isTaskInFreeform
import io.github.sterlingshell.yamff.xposed.util.ext.log
import io.github.qauxv.util.Initiator
import kotlin.concurrent.thread

class SystemUiHook : IXposedHookZygoteInit, IXposedHookLoadPackage {
    companion object {
        private const val TAG = "SystemUiHook"
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
             runCatching {
                 if (param.args[0] == "package") {
                     serviceManagerHook?.unhook()
                     val pms = param.args[1] as? IPackageManager ?: return@runCatching
                     log(TAG, "Got pms: $pms")
                     thread {
                         runCatching {
                             IpcEntry.register(pms)
                             log(TAG, "IpcEntry started")
                         }.onFailure {
                             log(TAG, "IpcEntry failed to start", it)
                         }
                     }
                 }
             }.onFailure { log(TAG, "Error in addService hook", it) }
         }

         var activityManagerServiceSystemReadyHook: XC_MethodHook.Unhook? = null
         activityManagerServiceSystemReadyHook = findMethodOrNull("com.android.server.am.ActivityManagerService") {
             name == "systemReady"
         }?.hookAfter {
             runCatching {
                 activityManagerServiceSystemReadyHook?.unhook()
                 IpcService.activityManagerService = it.thisObject
                 IpcService.systemReady()
                 log(TAG, "system ready")
             }.onFailure { log(TAG, "Error in systemReady hook", it) }
         }

        findMethodOrNull("com.android.server.am.ActivityManagerService") {
            name == "checkBroadcastFromSystem"
        }?.hookBefore {
            runCatching {
                val intent = it.args[0] as? Intent ?: return@runCatching
                if (intent.action == HookLauncher.ACTION_RECEIVE_LAUNCHER_CONFIG)
                    it.result = Unit // bypass check
            }.onFailure { log(TAG, "Error in checkBroadcastFromSystem hook", it) }
        }
        
        hookWindowLogic()
    }
    
    private fun hookWindowLogic() {
        val classRecentTasks = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.RecentTasks") ?: return
        val classActivityRecord = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.ActivityRecord") ?: return
        val classTask = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.Task") ?: return

        // Hide from Recents (Low level)
        classTask.findMethodOrNull { name == "isIncludedInRecents" }?.hookBefore { param ->
            if (param.thisObject.isTaskInFreeform()) {
                param.result = false
            }
        }

        classTask.findMethodOrNull { name == "shouldBeVisible" }?.hookBefore { param ->
            if (param.thisObject.isTaskInFreeform()) {
                param.result = false
            }
        }

        classActivityRecord.findMethodOrNull { name == "isIncludedInRecents" }?.hookBefore { param ->
            val displayId = param.thisObject.getDisplayIdSafe()
            if (displayId != 0 && FreeformManager.getWindowList().contains(displayId)) {
                param.result = false
            }
        }
        
        classRecentTasks.findMethodOrNull { name == "isVisibleRecentTask" }?.hookBefore { param ->
            if (param.args[0].isTaskInFreeform()) {
                param.result = false
            }
        }

        // Hide from Recents (List level)
        classRecentTasks.findAllMethods { name == "getRecentTasks" }.hookAfter { param ->
            runCatching {
                val result = param.result ?: return@hookAfter
                val list = io.github.sterlingshell.yamff.xposed.compat.SystemCompat.getRecentTasksList(result) ?: return@hookAfter
                val filteredList = list.filter { info ->
                    val displayId = io.github.sterlingshell.yamff.xposed.compat.SystemCompat.getDisplayId(info)
                    val inWindow = FreeformManager.isTaskInWindow(info.taskId) || FreeformManager.getWindowList().contains(displayId)
                    !inWindow
                }
                if (filteredList.size != list.size) {
                    param.result = io.github.sterlingshell.yamff.xposed.compat.SystemCompat.createParceledListSlice(filteredList)
                }
            }.onFailure { log(TAG, "Error in getRecentTasks hook", it) }
        }

        // Multi-Resume
        runCatching {
            classActivityRecord.findMethodOrNull { name == "shouldBeResumed" }?.hookBefore { param ->
                val displayId = param.thisObject.getDisplayIdSafe()
                if (FreeformManager.getWindowList().contains(displayId)) {
                    param.result = true
                }
            }
        }.onFailure { e ->
            log(TAG, "hook shouldBeResumed failed", e)
        }
    }
}
