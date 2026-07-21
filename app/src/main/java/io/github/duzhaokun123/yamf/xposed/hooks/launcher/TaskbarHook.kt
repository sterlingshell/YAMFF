package io.github.duzhaokun123.yamf.xposed.hooks.launcher

import android.app.AndroidAppHelper
import android.content.Intent
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAuto
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import com.github.kyuubiran.ezxhelper.utils.loadClass
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.duzhaokun123.yamf.xposed.services.YAMFServer
import io.github.duzhaokun123.yamf.xposed.utils.extensions.log

object TaskbarHook {
    private const val TAG = "YAMF_TaskbarHook"

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log(TAG, "hooking taskbar ${lpparam.packageName}")
        loadClass("com.android.launcher3.taskbar.TaskbarActivityContext").apply {
            findMethodOrNull { name == "startItemInfoActivity" }
                ?.hookReplace {
                    val infoIntent = it.args[0].invokeMethodAutoAs<Intent>("getIntent")!!
                    val intent = Intent(YAMFServer.ACTION_OPEN_IN_YAMF).apply {
                        setPackage("android")
                        putExtra(YAMFServer.EXTRA_COMPONENT_NAME, infoIntent.component)
                        putExtra(YAMFServer.EXTRA_SOURCE, YAMFServer.SOURCE_TASKBAR)
                    }
                    AndroidAppHelper.currentApplication().sendBroadcast(intent)
                }
            val class_WorkspaceItemInfo =
                loadClass("com.android.launcher3.model.data.WorkspaceItemInfo")
            findMethod { name == "onTaskbarIconClicked" }
                .hookBefore {
                    val tag = it.args[0].invokeMethodAuto("getTag")!!
                    if (class_WorkspaceItemInfo.isInstance(tag)) {
                        val infoIntent = tag.invokeMethodAutoAs<Intent>("getIntent")!!
                        val intent = Intent(YAMFServer.ACTION_OPEN_IN_YAMF).apply {
                            setPackage("android")
                            putExtra(YAMFServer.EXTRA_COMPONENT_NAME, infoIntent.component)
                            putExtra(YAMFServer.EXTRA_SOURCE, YAMFServer.SOURCE_TASKBAR)
                        }
                        AndroidAppHelper.currentApplication().sendBroadcast(intent)
                        it.result = Unit
                    }
                }
        }
    }
}
