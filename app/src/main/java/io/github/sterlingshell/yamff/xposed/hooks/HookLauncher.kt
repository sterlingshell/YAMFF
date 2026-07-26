package io.github.sterlingshell.yamff.xposed.hooks

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.sterlingshell.yamff.xposed.hooks.launcher.PopupHook
import io.github.sterlingshell.yamff.xposed.hooks.launcher.RecentsHook
import io.github.sterlingshell.yamff.xposed.hooks.launcher.TaskbarHook
import io.github.sterlingshell.yamff.xposed.hooks.launcher.TransientTaskbarHook
import io.github.sterlingshell.yamff.xposed.services.YAMFFServer
import io.github.sterlingshell.yamff.xposed.utils.extensions.log
import io.github.sterlingshell.yamff.xposed.utils.extensions.registerReceiver

class HookLauncher : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        const val TAG = "YAMFF_HookLauncher"
        const val ACTION_RECEIVE_LAUNCHER_CONFIG =
            "io.github.sterlingshell.yamff.ACTION_RECEIVE_LAUNCHER_CONFIG"

        const val EXTRA_HOOK_RECENTS = "hookRecents"
        const val EXTRA_HOOK_TASKBAR = "hookTaskbar"
        const val EXTRA_HOOK_POPUP = "hookPopup"
        const val EXTRA_HOOK_TRANSIENT_TASKBAR = "hookTransientTaskbar"
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelperInit.initHandleLoadPackage(lpparam)
        loadClassOrNull("com.android.launcher3.Launcher") ?: return
        Application::class.java.findMethod {
            name == "onCreate"
        }.hookAfter {
            val application = it.thisObject as? Application ?: return@hookAfter
            if (application.packageName != lpparam.packageName) return@hookAfter
            application.registerReceiver(ACTION_RECEIVE_LAUNCHER_CONFIG) { _, intent ->
                val hookRecents = intent.getBooleanExtra(EXTRA_HOOK_RECENTS, false)
                val hookTaskbar = intent.getBooleanExtra(EXTRA_HOOK_TASKBAR, false)
                val hookPopup = intent.getBooleanExtra(EXTRA_HOOK_POPUP, false)
                val hookTransientTaskbar =
                    intent.getBooleanExtra(EXTRA_HOOK_TRANSIENT_TASKBAR, false)
                log(
                    TAG,
                    "receive config hookRecents=$hookRecents hookTaskbar=$hookTaskbar hookPopup=$hookPopup hookTranslucentTaskbar=$hookTransientTaskbar"
                )
                if (hookRecents) runCatching { RecentsHook.hook(lpparam) }.onFailure { e ->
                    log(TAG, "hook recents failed", e) }
                if (hookTaskbar) runCatching { TaskbarHook.hook(lpparam) }.onFailure { e ->
                    log(TAG, "hook taskbar failed", e) }
                if (hookPopup) runCatching { PopupHook.hook(lpparam) }.onFailure { e ->
                    log(TAG, "hook popup failed", e) }
                if (hookTransientTaskbar) runCatching { TransientTaskbarHook.hook(lpparam) }.onFailure { e ->
                    log(TAG, "hook transient taskbar failed", e) }
                application.unregisterReceiver(this)
            }
            application.sendBroadcast(Intent(YAMFFServer.ACTION_GET_LAUNCHER_CONFIG).apply {
                `package` = "android"
                putExtra("sender", application.packageName)
            })
        }
    }
}
