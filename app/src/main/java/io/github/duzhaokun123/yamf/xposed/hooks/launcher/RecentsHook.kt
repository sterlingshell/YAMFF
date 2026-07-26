package io.github.duzhaokun123.yamf.xposed.hooks.launcher

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.github.kyuubiran.ezxhelper.init.InitFields.moduleRes
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.duzhaokun123.yamf.BuildConfig
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.xposed.services.YAMFServer
import io.github.duzhaokun123.yamf.xposed.utils.extensions.log

object RecentsHook {
    private const val TAG = "YAMF_RecentsHook"

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log(TAG, "hooking recents ${lpparam.packageName}")
        XposedBridge.hookAllMethods(
            XposedHelpers.findClass(
                "com.android.quickstep.TaskOverlayFactory",
                lpparam.classLoader
            ), "getEnabledShortcuts", object : XC_MethodHook() {
                @SuppressLint("UseCompatLoadingForDrawables")
                override fun afterHookedMethod(param: MethodHookParam) {
                    val taskView = param.args[0] as View
                    val shortcuts = param.result as MutableList<Any>
                    if (shortcuts.isEmpty()) return
                    var itemInfo = XposedHelpers.getObjectField(shortcuts[0], "mItemInfo")
                    itemInfo =
                        itemInfo.javaClass.getConstructor(itemInfo.javaClass).newInstance(itemInfo)
                    val activity = taskView.context
                    val task = XposedHelpers.callMethod(taskView, "getTask")
                    val key = XposedHelpers.getObjectField(task, "key")
                    val taskId = XposedHelpers.getIntField(key, "id")
                    val topComponent =
                        XposedHelpers.callMethod(itemInfo, "getTargetComponent") as ComponentName
                    val userId = XposedHelpers.getIntField(key, "userId")

                    val class_RemoteActionShortcut = XposedHelpers.findClass(
                        "com.android.launcher3.popup.RemoteActionShortcut",
                        lpparam.classLoader
                    )
                    val intent = Intent(YAMFServer.ACTION_OPEN_IN_YAMF).apply {
                        setPackage("android")
                        putExtra(YAMFServer.EXTRA_TASK_ID, taskId)
                        putExtra(YAMFServer.EXTRA_COMPONENT_NAME, topComponent)
                        putExtra(YAMFServer.EXTRA_USER_ID, userId)
                        putExtra(YAMFServer.EXTRA_SOURCE, YAMFServer.SOURCE_RECENTS)
                    }
                    val action = RemoteAction(
                        Icon.createWithBitmap(
                            moduleRes.getDrawable(R.drawable.ic_picture_in_picture_alt_24, null)
                                .toBitmap()
                        ),
                        moduleRes.getString(R.string.open_with_yamf) + if (BuildConfig.DEBUG) " ($taskId)" else "",
                        "",
                        PendingIntent.getBroadcast(
                            AndroidAppHelper.currentApplication(),
                            1345,
                            intent,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    val c = class_RemoteActionShortcut.constructors[0]
                    val shortcut = when (c.parameterCount) {
                        4 -> c.newInstance(action, activity, itemInfo, null)
                        3 -> c.newInstance(action, activity, itemInfo)
                        else -> {
                            log(
                                TAG,
                                "unknown RemoteActionShortcut constructor: ${c.toGenericString()}"
                            )
                            null
                        }
                    }

                    if (shortcut != null) {
                        shortcuts.add(shortcut)
                    }
                }
            })
    }
}
