package io.github.sterlingshell.yamff.xposed.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.extensions.gson
import io.github.sterlingshell.yamff.common.model.StartCommand
import io.github.sterlingshell.yamff.common.extensions.runMain
import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import io.github.sterlingshell.yamff.xposed.IYAMFFManager
import io.github.sterlingshell.yamff.xposed.hooks.HookLauncher
import io.github.sterlingshell.yamff.xposed.window.AppListWindow
import io.github.sterlingshell.yamff.xposed.window.AppWindow
import io.github.sterlingshell.yamff.xposed.utils.SystemServices
import io.github.sterlingshell.yamff.xposed.utils.SystemServices.systemContext
import io.github.sterlingshell.yamff.xposed.utils.SystemServices.systemUiContext
import io.github.sterlingshell.yamff.xposed.utils.extensions.createContext
import io.github.sterlingshell.yamff.xposed.utils.extensions.getTopRootTask
import io.github.sterlingshell.yamff.xposed.utils.extensions.log
import io.github.sterlingshell.yamff.xposed.utils.extensions.registerReceiver
import io.github.sterlingshell.yamff.xposed.utils.extensions.startAuto
import io.github.qauxv.ui.CommonContextWrapper
import rikka.hidden.compat.ActivityManagerApis

object YAMFFServer : IYAMFFManager.Stub() {
    const val TAG = "YAMFFManager"

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
    fun systemReady() {
        SystemServices.init(activityManagerService)
        systemContext.registerReceiver(ACTION_OPEN_IN_YAMFF, OpenInYAMFFBroadcastReceiver)
        systemContext.registerReceiver(ACTION_CURRENT_TO_WINDOW) { _, _ ->
            currentToWindow()
        }
        systemContext.registerReceiver(ACTION_OPEN_APP_LIST) { _, _ ->
            AppListWindow(
                CommonContextWrapper.createAppCompatContext(systemUiContext.createContext()),
                null
            )
        }
        systemContext.registerReceiver(ACTION_OPEN_APP) { _, intent ->
            val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_COMPONENT_NAME)
                ?: return@registerReceiver
            val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
            createWindow(StartCommand(componentName = componentName, userId = userId))
        }
        systemContext.registerReceiver(ACTION_GET_LAUNCHER_CONFIG) { _, intent ->
            ActivityManagerApis.broadcastIntent(Intent(HookLauncher.ACTION_RECEIVE_LAUNCHER_CONFIG).apply {
                val config = ConfigManager.config
                log(TAG, "send config: ${config.hookLauncher}")
                putExtra(HookLauncher.EXTRA_HOOK_RECENTS, config.hookLauncher.hookRecents)
                putExtra(HookLauncher.EXTRA_HOOK_TASKBAR, config.hookLauncher.hookTaskbar)
                putExtra(HookLauncher.EXTRA_HOOK_POPUP, config.hookLauncher.hookPopup)
                putExtra(HookLauncher.EXTRA_HOOK_TRANSIENT_TASKBAR, config.hookLauncher.hookTransientTaskbar)
                `package` = intent.getStringExtra("sender")
            }, 0)
        }
        ConfigManager.loadConfig()
    }

    fun createWindow(startCmd: StartCommand?) {
        SystemServices.iStatusBarService.collapsePanels()
        runCatching {
            val moduleContext = systemContext.createPackageContext(
                BuildConfig.APPLICATION_ID,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            AppWindow(
                CommonContextWrapper.createAppCompatContext(moduleContext),
                ConfigManager.config.densityDpi,
                ConfigManager.config.flags,
                startCmd?.startRect
            ) { displayId ->
                YAMFFWindowManager.addWindow(displayId)
                startCmd?.taskId?.let { YAMFFWindowManager.associateTaskWithDisplay(it, displayId) }
                startCmd?.startAuto(displayId)
            }
        }.onFailure { e ->
            log(TAG, "Failed to create window due to context error", e)
        }
    }

    init {
        log(TAG, "YAMFF service initialized")
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
        return gson.toJson(ConfigManager.config)
    }

    override fun updateConfig(newConfig: String) {
        runMain {
            ConfigManager.updateConfig(newConfig)
        }
    }

    override fun registerOpenCountListener(iOpenCountListener: IOpenCountListener) {
        YAMFFWindowManager.registerOpenCountListener(iOpenCountListener)
    }

    override fun unregisterOpenCountListener(iOpenCountListener: IOpenCountListener?) {
        YAMFFWindowManager.unregisterOpenCountListener(iOpenCountListener)
    }

    override fun openAppList() {
        runMain {
            SystemServices.iStatusBarService.collapsePanels()
            AppListWindow(
                CommonContextWrapper.createAppCompatContext(systemUiContext.createContext()),
                null
            )
        }
    }

    override fun currentToWindow() {
        runMain {
            val task = getTopRootTask(0) ?: return@runMain
            createWindow(StartCommand(taskId = task.taskId))
        }
    }

    override fun resetAllWindow() {
        runMain {
            SystemServices.iStatusBarService.collapsePanels()
            systemContext.sendBroadcast(Intent(AppWindow.ACTION_RESET_ALL_WINDOW))
        }
    }

    private val OpenInYAMFFBroadcastReceiver: BroadcastReceiver.(Context, Intent) -> Unit =
        { _: Context, intent: Intent ->
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
            val componentName = intent.getParcelableExtra<ComponentName>(EXTRA_COMPONENT_NAME)
            val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
            val source = intent.getIntExtra(EXTRA_SOURCE, SOURCE_UNSPECIFIED)
            createWindow(StartCommand(componentName, userId, taskId))

            if (source == SOURCE_RECENTS && ConfigManager.config.recentsBackHome) {
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
