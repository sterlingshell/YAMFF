package io.github.sterlingshell.yamff.xposed.hooks

import android.content.Intent
import android.content.pm.IPackageManager
import android.hardware.HardwareBuffer
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
import io.github.sterlingshell.yamff.di.coreModule
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.core.ExtensionRegistry
import io.github.sterlingshell.yamff.xposed.core.IpcService
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import io.github.sterlingshell.yamff.xposed.core.IpcEntry
import io.github.sterlingshell.yamff.xposed.sys.SystemServices
import io.github.sterlingshell.yamff.xposed.util.ext.getDisplayIdSafe
import io.github.sterlingshell.yamff.xposed.util.ext.isTaskInFreeform
import io.github.sterlingshell.yamff.xposed.util.ext.log
import io.github.sterlingshell.yamff.xposed.sys.graphics.processForRecentTask
import io.github.sterlingshell.yamff.common.model.RecentTaskMode
import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.qauxv.util.Initiator
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.concurrent.thread

class SystemUiHook : IXposedHookZygoteInit, IXposedHookLoadPackage, KoinComponent {
    companion object {
        private const val TAG = "SystemUiHook"
    }

    private val configManager: ConfigManager by inject()

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android") return
        log(TAG, "YAMFF SystemUiHook: android loaded")
        log(TAG, "Build: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}")
        
        EzXHelperInit.initHandleLoadPackage(lpparam)
        Initiator.init(lpparam.classLoader)

        runCatching {
            org.koin.core.context.GlobalContext.getOrNull() ?: startKoin {
                modules(coreModule)
            }
            // Force eager initialization of singletons
            get<ConfigManager>()
            get<ExtensionRegistry>()
            log(TAG, "Koin and core components initialized in system_server")
        }.onFailure { log(TAG, "Koin init failed (might be already started)", it) }

         var serviceManagerHook: XC_MethodHook.Unhook? = null
         serviceManagerHook = findMethodOrNull("android.os.ServiceManager") {
             name == "addService"
         }?.hookBefore { param ->
             runCatching {
                 if (param.args[0] == "package") {
                     serviceManagerHook?.unhook()
                     val pms = (param.args[1] as? IPackageManager) ?: return@runCatching
                     log(TAG, "Got pms: $pms")
                     thread {
                         runCatching {
                             IpcEntry.register(pms)
                             log(TAG, "IpcEntry started")
                         }.onFailure { e ->
                             log(TAG, "IpcEntry failed to start", e)
                         }
                     }
                 }
             }.onFailure { e -> log(TAG, "Error in addService hook", e) }
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
             }.onFailure { e -> log(TAG, "Error in systemReady hook", e) }
         }

        findMethodOrNull("com.android.server.am.ActivityManagerService") {
            name == "checkBroadcastFromSystem"
        }?.hookBefore {
            runCatching {
                val intent = it.args[0] as? Intent ?: return@runCatching
                if (intent.action == HookLauncher.ACTION_RECEIVE_LAUNCHER_CONFIG)
                    it.result = Unit // bypass check
            }.onFailure { e -> log(TAG, "Error in checkBroadcastFromSystem hook", e) }
        }

        // BroadcastController is introduced in Android 15 (API 35)
        runCatching {
            com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.am.BroadcastController")?.findMethodOrNull {
                name == "checkBroadcastFromSystem"
            }?.hookBefore {
                runCatching {
                    val intent = it.args[0] as? Intent ?: return@runCatching
                    if (intent.action == HookLauncher.ACTION_RECEIVE_LAUNCHER_CONFIG)
                        it.result = Unit // bypass check
                }.onFailure { e -> log(TAG, "Error in checkBroadcastFromSystem (BroadcastController) hook", e) }
            }
        }.onFailure { e -> log(TAG, "BroadcastController not found or hook failed", e) }
        
        hookWindowLogic()
    }
    
    private fun hookWindowLogic() {
        val classRecentTasks = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.RecentTasks") ?: return
        val classActivityRecord = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.ActivityRecord") ?: return
        val classTask = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.Task") ?: return
        val classATM = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.ActivityTaskManagerService") ?: return

        // Hide from Recents (Low level)
        classTask.findAllMethods { name == "isIncludedInRecents" }.hookBefore { param ->
            if ((configManager.config.recentTaskMode == RecentTaskMode.HIDDEN) && param.thisObject.isTaskInFreeform()) {
                param.result = false
            }
        }

        classTask.findAllMethods { name == "shouldBeVisible" }.hookBefore { param ->
            if ((configManager.config.recentTaskMode == RecentTaskMode.HIDDEN) && param.thisObject.isTaskInFreeform()) {
                param.result = false
            }
        }

        classActivityRecord.findAllMethods { name == "isIncludedInRecents" }.hookBefore { param ->
            if (configManager.config.recentTaskMode == RecentTaskMode.HIDDEN) {
                val displayId = param.thisObject.getDisplayIdSafe()
                if (displayId != 0 && FreeformManager.getWindowList().contains(displayId)) {
                    param.result = false
                }
            }
        }
        
        classRecentTasks.findAllMethods { name == "isVisibleRecentTask" }.hookBefore { param ->
            if ((configManager.config.recentTaskMode == RecentTaskMode.HIDDEN) && param.args[0].isTaskInFreeform()) {
                param.result = false
            }
        }

        // Hide from Recents (List level)
        classRecentTasks.findAllMethods { name == "getRecentTasks" }.hookAfter { param ->
            runCatching {
                if (configManager.config.recentTaskMode != RecentTaskMode.HIDDEN) return@runCatching
                
                val result = param.result ?: return@hookAfter
                val list = SystemCompat.getRecentTasksList(result) ?: return@hookAfter
                val filteredList = list.filter { info ->
                    val displayId = SystemCompat.getDisplayId(info)
                    val inWindow = FreeformManager.isTaskInWindow(info.taskId) || FreeformManager.getWindowList().contains(displayId)
                    !inWindow
                }
                if (filteredList.size != list.size) {
                    param.result = SystemCompat.createParceledListSlice(filteredList)
                }
            }.onFailure { e -> log(TAG, "Error in getRecentTasks hook", e) }
        }

        // Snapshot Optimization
        val classSnapshotController = com.github.kyuubiran.ezxhelper.utils.loadClassOrNull("com.android.server.wm.TaskSnapshotController")
        val snapshotAction: (XC_MethodHook.MethodHookParam) -> Unit = { param ->
            runCatching {
                if (configManager.config.recentTaskMode != RecentTaskMode.DECORATED) return@runCatching
                
                val snapshot = param.result ?: return@runCatching
                val task = if (param.thisObject.javaClass.simpleName == "Task") param.thisObject else param.args[0]
                val taskId = SystemCompat.getTaskId(task)
                
                val isFreeform = FreeformManager.isTaskInWindow(taskId) || FreeformManager.isTaskFormerFreeform(taskId)
                
                if (isFreeform) {
                    val buffer = (de.robv.android.xposed.XposedHelpers.callMethod(snapshot, "getHardwareBuffer") as? HardwareBuffer) ?: return@runCatching
                    
                    val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(buffer, null) ?: return@runCatching
                    val softwareBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return@runCatching
                    
                    val window = FreeformManager.getDisplayIdForTask(taskId)?.let { FreeformManager.getWindow(it) }
                    
                    val optimizedBitmap = window?.renderer?.decorateSnapshot(softwareBitmap)
                        ?: softwareBitmap.processForRecentTask(
                            softwareBitmap.width,
                            softwareBitmap.height,
                            configManager.config.snapshotBackground,
                            addDecoration = false,
                        )
                    
                    val newBuffer = optimizedBitmap.copy(android.graphics.Bitmap.Config.HARDWARE, false)?.hardwareBuffer ?: return@runCatching
                    
                    val field = snapshot.javaClass.getDeclaredField("mSnapshot")
                    field.isAccessible = true
                    field[snapshot] = newBuffer
                }
            }.onFailure { e -> log(TAG, "Error in snapshot hook", e) }
        }

        classTask.findMethodOrNull { name == "getSnapshot" }?.hookAfter { snapshotAction(it) }
        classSnapshotController?.findMethodOrNull { name == "getSnapshot" }?.hookAfter { snapshotAction(it) }
        classSnapshotController?.findMethodOrNull { name == "snapshotTask" }?.hookAfter { snapshotAction(it) }

        // Intercept Recents Click
        classATM.findMethodOrNull { name == "startActivityFromRecents" }?.hookBefore { param ->
            runCatching {
                if (configManager.config.recentTaskMode != RecentTaskMode.DECORATED) return@runCatching
                
                val taskId = param.args[0] as Int
                val displayId = FreeformManager.getDisplayIdForTask(taskId)
                
                if (displayId != null) {
                    val window = FreeformManager.getWindow(displayId)
                    if (window != null) {
                        FreeformManager.moveToTop(displayId)
                        SystemCompat.setFocusedDisplay(SystemServices.iWindowManager, displayId)
                        param.result = 0 // START_SUCCESS
                    }
                }
            }.onFailure { e -> log(TAG, "Error in startActivityFromRecents hook", e) }
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
