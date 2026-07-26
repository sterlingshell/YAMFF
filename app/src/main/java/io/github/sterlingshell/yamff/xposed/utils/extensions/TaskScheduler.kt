package io.github.sterlingshell.yamff.xposed.utils.extensions

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.ActivityTaskManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.newInstance
import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.common.model.StartCommand
import io.github.sterlingshell.yamff.common.extensions.onException
import io.github.sterlingshell.yamff.xposed.services.ConfigManager
import io.github.sterlingshell.yamff.xposed.utils.SystemServices
import io.github.sterlingshell.yamff.xposed.utils.TipUtil

@SuppressLint("MissingPermission")
fun moveTask(taskId: Int, displayId: Int) {
    if (!SystemServices.checkInitialized()) return
    runCatching {
        SystemServices.activityTaskManager.moveRootTaskToDisplay(taskId, displayId)
        SystemServices.activityManager.moveTaskToFront(taskId, 0)
    }.onFailure { log("TaskScheduler", "moveTask failed", it) }
}

fun startActivity(context: Context, componentName: ComponentName, userId: Int, displayId: Int) {
    if (!SystemServices.checkInitialized()) return
    runCatching {
        context.invokeMethod(
            "startActivityAsUser",
            args(
                Intent().apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    component = componentName
                    `package` = component!!.packageName
                    action = Intent.ACTION_VIEW
                },
                ActivityOptions.makeBasic().apply {
                    launchDisplayId = displayId
                    SystemCompat.setCallerDisplayId(this, displayId)
                }.toBundle(),
                UserHandle::class.java.newInstance(
                    args(userId),
                    argTypes(Integer.TYPE)
                )
            ), argTypes(Intent::class.java, Bundle::class.java, UserHandle::class.java)
        )
    }.onFailure { log("TaskScheduler", "startActivity failed", it) }
}

fun moveToDisplay(context: Context, taskId: Int, componentName: ComponentName, userId: Int, displayId: Int) {
    when (ConfigManager.config.windowfy) {
        0 -> {
            runCatching {
                moveTask(taskId, displayId)
            }.onException {
                TipUtil.showToast("can't move task $taskId")
            }
        }
        1 -> {
            runCatching {
                startActivity(context, componentName, userId, displayId)
            }.onException {
                TipUtil.showToast("can't start activity $componentName")
            }
        }
        2 -> {
            runCatching {
                moveTask(taskId, displayId)
            }.onException {
                TipUtil.showToast("can't move task $taskId")
                runCatching {
                    startActivity(context, componentName, userId, displayId)
                }.onException {
                    TipUtil.showToast("can't start activity $componentName")
                }
            }
        }
    }
}

fun StartCommand.startAuto(displayId: Int) {
    when {
        canStartActivity && canMoveTask ->
            moveToDisplay(SystemServices.systemContext, taskId!!, componentName!!, userId!!, displayId)
        canMoveTask -> {
            runCatching {
                moveTask(taskId!!, displayId)
            }.onException {
                TipUtil.showToast("can't move task $taskId")
            }
        }
        canStartActivity -> {
            runCatching {
                startActivity(SystemServices.systemContext, componentName!!, userId!!, displayId)
            }.onException {
                TipUtil.showToast("can't start activity $componentName")
            }
        }
    }
}

fun getTopRootTask(displayId: Int): ActivityTaskManager.RootTaskInfo? {
    SystemServices.activityTaskManager.getAllRootTaskInfosOnDisplay(displayId).forEach { task ->
        if (task.visible)
            return task
    }
    return null
}
