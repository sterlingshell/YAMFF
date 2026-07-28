package io.github.sterlingshell.yamff.xposed.core

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcel
import android.os.Process
import android.os.RemoteCallbackList
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.common.ext.runMain
import io.github.sterlingshell.yamff.common.model.LaunchRequest
import io.github.sterlingshell.yamff.xposed.IFreeform
import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import io.github.sterlingshell.yamff.xposed.IConfigChangeListener
import io.github.sterlingshell.yamff.xposed.IExtensionsChangeListener
import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.xposed.hooks.HookLauncher
import io.github.sterlingshell.yamff.xposed.sys.SystemServices
import io.github.sterlingshell.yamff.xposed.sys.SystemServices.systemContext
import io.github.sterlingshell.yamff.xposed.sys.SystemServices.systemUiContext
import io.github.sterlingshell.yamff.xposed.util.ext.createContext
import io.github.sterlingshell.yamff.xposed.util.ext.getTopRootTask
import io.github.sterlingshell.yamff.xposed.util.ext.log
import io.github.sterlingshell.yamff.xposed.util.ext.registerReceiver
import io.github.sterlingshell.yamff.xposed.util.ext.launch
import io.github.sterlingshell.yamff.xposed.window.AppPicker
import io.github.sterlingshell.yamff.xposed.window.Window
import io.github.qauxv.ui.CommonContextWrapper
import kotlin.concurrent.thread
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.hidden.compat.ActivityManagerApis

object IpcService : IFreeform.Stub(), KoinComponent {
    const val TAG = "IpcService"
    
    private val configManager: ConfigManager by inject()
    private val extensionRegistry: ExtensionRegistry by inject()

    @Volatile
    var managerUid: Int = -1

    private val configListeners = RemoteCallbackList<IConfigChangeListener>()
    private val extensionsListeners = RemoteCallbackList<IExtensionsChangeListener>()

    const val ACTION_GET_LAUNCHER_CONFIG = "io.github.sterlingshell.yamff.ACTION_GET_LAUNCHER_CONFIG"
    const val ACTION_OPEN_APP = "io.github.sterlingshell.yamff.action.OPEN_APP"
    const val ACTION_CURRENT_TO_WINDOW = "io.github.sterlingshell.yamff.action.CURRENT_TO_WINDOW"
    const val ACTION_OPEN_APP_LIST = "io.github.sterlingshell.yamff.action.OPEN_APP_LIST"
    const val ACTION_OPEN_IN_YAMFF = "io.github.sterlingshell.yamff.action.ACTION_OPEN_IN_YAMFF"

    const val EXTRA_COMPONENT_NAME = "componentName"
    const val EXTRA_USER_ID = "userId"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_SOURCE = "source"

    const val SOURCE_UNSPECIFIED = 0
    const val SOURCE_RECENTS = 1
    const val SOURCE_TASKBAR = 2
    const val SOURCE_POPUP = 3

    lateinit var activityManagerService: Any

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Suppress("DEPRECATION")
    fun systemReady() {
        SystemServices.init(activityManagerService)
        log(TAG, "systemReady: initializing fallbacks")
        
        // Ensure IpcEntry is registered even if addService hook was missed
        thread {
            runCatching {
                val pms = SystemServices.iPackageManager
                IpcEntry.register(pms)
            }.onFailure { log(TAG, "Fallback IpcEntry registration failed", it) }
        }

        systemContext.registerReceiver(ACTION_OPEN_IN_YAMFF, OpenInYAMFFBroadcastReceiver)
        systemContext.registerReceiver(ACTION_CURRENT_TO_WINDOW) { _, _ ->
            currentToWindow()
        }
        systemContext.registerReceiver(ACTION_OPEN_APP_LIST) { _, _ ->
            AppPicker(
                CommonContextWrapper.createAppCompatContext(systemUiContext.createContext()),
                null
            )
        }
        systemContext.registerReceiver(ACTION_OPEN_APP) { _, intent ->
            val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_COMPONENT_NAME)
                ?: return@registerReceiver
            val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
            createWindow(LaunchRequest(componentName = componentName, userId = userId))
        }
        systemContext.registerReceiver(ACTION_GET_LAUNCHER_CONFIG) { _, intent ->
            ActivityManagerApis.broadcastIntent(Intent(HookLauncher.ACTION_RECEIVE_LAUNCHER_CONFIG).apply {
                val config = configManager.config
                log(TAG, "send config: ${config.hookLauncher}")
                putExtra(HookLauncher.EXTRA_HOOK_RECENTS, config.hookLauncher.hookRecents)
                putExtra(HookLauncher.EXTRA_HOOK_TASKBAR, config.hookLauncher.hookTaskbar)
                putExtra(HookLauncher.EXTRA_HOOK_POPUP, config.hookLauncher.hookPopup)
                putExtra(HookLauncher.EXTRA_HOOK_TRANSIENT_TASKBAR, config.hookLauncher.hookTransientTaskbar)
                `package` = intent.getStringExtra("sender")
            }, 0)
        }
        
        // Listen for package changes to refresh UID cache
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        systemContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                log(TAG, "Package change detected, refreshing UID cache")
                extensionRegistry.refreshUidCache()
            }
        }, packageFilter)
    }

    fun createWindow(request: LaunchRequest?) {
        SystemServices.iStatusBarService.collapsePanels()
        runCatching {
            // Force refresh package info to avoid stale ApplicationInfo and Resource ID mismatches

            val moduleContext = systemContext.createPackageContext(
                BuildConfig.APPLICATION_ID,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            
            // Log for debugging ID drift
            log(TAG, "Creating window with module context: ${moduleContext.packageCodePath}")
            
            Window(
                CommonContextWrapper.createAppCompatContext(moduleContext),
                configManager.config.densityDpi,
                configManager.config.flags,
                request?.startRect
            ) { displayId ->
                FreeformManager.addWindow(displayId)
                request?.taskId?.let { FreeformManager.associateTaskWithDisplay(it, displayId) }
                request?.launch(displayId)
            }
        }.onFailure { e ->
            log(TAG, "Failed to create window due to context error", e)
        }
    }

    init {
        log(TAG, "IPC service initialized")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        val callingUid = getCallingUid()
        
        // 1. Authorization Check
        // code >= 1 && code <= 0x00ffffff are AIDL-generated method calls
        if (code in FIRST_CALL_TRANSACTION..LAST_CALL_TRANSACTION) {
             val isAuthorized = callingUid == managerUid || 
                               callingUid == Process.SYSTEM_UID || 
                               callingUid == Process.ROOT_UID ||
                               extensionRegistry.authorizedUids.contains(callingUid)

             if (!isAuthorized) {
                 if (BuildConfig.DEBUG) {
                     log(TAG, "Denied unauthorized IPC call (code: $code) from UID: $callingUid")
                 }
                 throw SecurityException("Calling UID $callingUid is not authorized by YAMFF Manager.")
             }
        }
        
        return try {
            super.onTransact(code, data, reply, flags)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) {
                log(TAG, "Error in onTransact for code $code from UID $callingUid", t)
            }
            throw t
        }
    }

    override fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    override fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    override fun getUid(): Int {
        return Process.myUid()
    }

    override fun createWindow() {
        runMain {
            createWindow(null)
        }
    }

    override fun getBuildTime(): Long {
        return BuildConfig.BUILD_TIME
    }

    override fun getConfigJson(): String {
        return gson.toJson(configManager.config)
    }

    override fun updateConfig(newConfig: String) {
        runMain {
            configManager.updateConfig(newConfig)
            notifyConfigChanged(newConfig)
        }
    }

    private fun notifyConfigChanged(configJson: String) {
        val n = configListeners.beginBroadcast()
        for (i in 0 until n) {
            runCatching {
                configListeners.getBroadcastItem(i).onConfigChanged(configJson)
            }
        }
        configListeners.finishBroadcast()
    }

    override fun registerOpenCountListener(iOpenCountListener: IOpenCountListener) {
        FreeformManager.registerOpenCountListener(iOpenCountListener)
    }

    override fun unregisterOpenCountListener(iOpenCountListener: IOpenCountListener?) {
        FreeformManager.unregisterOpenCountListener(iOpenCountListener)
    }

    override fun registerConfigChangeListener(listener: IConfigChangeListener) {
        configListeners.register(listener)
    }

    override fun unregisterConfigChangeListener(listener: IConfigChangeListener) {
        configListeners.unregister(listener)
    }

    override fun registerExtensionsChangeListener(listener: IExtensionsChangeListener) {
        extensionsListeners.register(listener)
    }

    override fun unregisterExtensionsChangeListener(listener: IExtensionsChangeListener) {
        extensionsListeners.unregister(listener)
    }

    override fun openAppList() {
        runMain {
            SystemServices.iStatusBarService.collapsePanels()
            AppPicker(
                CommonContextWrapper.createAppCompatContext(systemUiContext.createContext()),
                null
            )
        }
    }

    override fun currentToWindow() {
        runMain {
            val task = getTopRootTask(0) ?: return@runMain
            createWindow(LaunchRequest(taskId = task.taskId))
        }
    }

    override fun resetAllWindow() {
        runMain {
            SystemServices.iStatusBarService.collapsePanels()
            systemContext.sendBroadcast(Intent(Window.ACTION_RESET_ALL_WINDOW))
        }
    }

    override fun getExtensionsJson(): String {
        return gson.toJson(extensionRegistry.getAllAuthorizedPackages())
    }

    override fun updateExtensions(newConfig: String) {
        runMain {
            val authorized: Set<String> = gson.fromJson(newConfig, object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type)
            val current = extensionRegistry.getAllAuthorizedPackages()
            (current - authorized).forEach { extensionRegistry.setAuthorized(it, false) }
            (authorized - current).forEach { extensionRegistry.setAuthorized(it, true) }
            // Force refresh UID cache in memory
            extensionRegistry.refreshUidCache()
            notifyExtensionsChanged(newConfig)
        }
    }

    private fun notifyExtensionsChanged(extensionsJson: String) {
        val n = extensionsListeners.beginBroadcast()
        for (i in 0 until n) {
            runCatching {
                extensionsListeners.getBroadcastItem(i).onExtensionsChanged(extensionsJson)
            }
        }
        extensionsListeners.finishBroadcast()
    }

    @Suppress("DEPRECATION")
    private val OpenInYAMFFBroadcastReceiver: BroadcastReceiver.(Context, Intent) -> Unit =
        { _: Context, intent: Intent ->
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
            val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_COMPONENT_NAME)
            val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
            val source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_UNSPECIFIED)
            createWindow(LaunchRequest(componentName, userId, taskId))

            if (source == SOURCE_RECENTS && configManager.config.recentsBackHome) {
                runCatching {
                    val down = KeyEvent(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_HOME,
                        0
                    ).apply {
                        this.source = InputDevice.SOURCE_KEYBOARD
                        SystemCompat.setDisplayId(this, 0)
                    }
                    SystemServices.inputManager.injectInputEvent(down, 0)
                    val up = KeyEvent(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_HOME,
                        0
                    ).apply {
                        this.source = InputDevice.SOURCE_KEYBOARD
                        SystemCompat.setDisplayId(this, 0)
                    }
                    SystemServices.inputManager.injectInputEvent(up, 0)
                }.onFailure { log(TAG, "Failed to inject HOME key", it) }
            }
        }
}
