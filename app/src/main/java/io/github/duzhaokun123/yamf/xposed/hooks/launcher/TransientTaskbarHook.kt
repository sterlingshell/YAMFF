package io.github.duzhaokun123.yamf.xposed.hooks.launcher

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.github.kyuubiran.ezxhelper.utils.loadClass
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.duzhaokun123.yamf.xposed.utils.extensions.log

object TransientTaskbarHook {
    private const val TAG = "YAMF_TransientTaskbarHook"

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log(TAG, "hook transientTaskbar ${lpparam.packageName}")
        loadClass("com.android.launcher3.util.DisplayController")
            .findMethod { name == "isTransientTaskbar" }
            .hookReturnConstant(true)
    }
}
