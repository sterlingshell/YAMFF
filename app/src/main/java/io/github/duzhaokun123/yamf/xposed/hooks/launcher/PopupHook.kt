package io.github.duzhaokun123.yamf.xposed.hooks.launcher

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Intent
import android.os.UserHandle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.InitFields.moduleRes
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAuto
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.github.kyuubiran.ezxhelper.utils.paramCount
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.xposed.compat.SystemCompat
import io.github.duzhaokun123.yamf.xposed.services.YAMFServer
import io.github.duzhaokun123.yamf.xposed.utils.extensions.log
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.WeakHashMap

object PopupHook {
    private const val TAG = "YAMF_PopupHook"
    private val proxyInstances = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())

    fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        log(TAG, "hooking popup ${lpparam.packageName}")
        
        loadClass("com.android.launcher3.popup.SystemShortcut")
            .findField { name == "INSTALL" }
            .set(null, getOpenInYAMFSystemShortcutFactory(lpparam.classLoader))
            
        val shortcutClass = loadClass("com.android.launcher3.popup.SystemShortcut")
        val installClass = loadClass("com.android.launcher3.popup.SystemShortcut\$Install")
        
        val methodsToHook = listOf("onClick", "setIconAndContentDescriptionFor", "setIconAndLabelFor")
        
        shortcutClass.findAllMethods { methodsToHook.contains(name) }.hookBefore { param ->
            if (proxyInstances.contains(param.thisObject)) {
                handleProxyMethod(param)
            }
        }
        
        installClass.findAllMethods { methodsToHook.contains(name) }.hookBefore { param ->
            if (proxyInstances.contains(param.thisObject)) {
                handleProxyMethod(param)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun handleProxyMethod(param: XC_MethodHook.MethodHookParam) {
        val methodName = param.method.name
        val thiz = param.thisObject
        when (methodName) {
            "onClick" -> {
                val mItemInfo = thiz.getObject("mItemInfo")
                val componentName = SystemCompat.getTargetComponent(mItemInfo)
                val userHandle = SystemCompat.getUserHandle(mItemInfo)
                AndroidAppHelper.currentApplication()
                    .sendBroadcast(Intent(YAMFServer.ACTION_OPEN_APP).apply {
                        setPackage("android")
                        putExtra(YAMFServer.EXTRA_COMPONENT_NAME, componentName)
                        putExtra(YAMFServer.EXTRA_USER_ID, (userHandle as? UserHandle)?.hashCode() ?: 0)
                        putExtra(YAMFServer.EXTRA_SOURCE, YAMFServer.SOURCE_POPUP)
                    })
                thiz.invokeMethodAuto("dismissTaskMenuView", thiz.getObject("mTarget"))
                param.result = Unit
            }

            "setIconAndContentDescriptionFor" -> {
                val view = param.args[0] as ImageView
                view.setImageDrawable(
                    moduleRes.getDrawable(R.drawable.ic_picture_in_picture_alt_24, null)
                )
                view.contentDescription = moduleRes.getString(R.string.open_with_yamf)
                param.result = Unit
            }

            "setIconAndLabelFor" -> {
                val iconView = param.args[0] as View
                val labelView = param.args[1] as TextView
                iconView.background =
                    moduleRes.getDrawable(R.drawable.ic_picture_in_picture_alt_24, null)
                labelView.text = moduleRes.getString(R.string.open_with_yamf)
                param.result = Unit
            }
        }
    }

    private fun getOpenInYAMFSystemShortcutFactory(classLoader: ClassLoader): Any {
        return Proxy.newProxyInstance(
            classLoader, arrayOf(loadClass("com.android.launcher3.popup.SystemShortcut\$Factory"))
        ) { _, method, args ->
            if (method.name != "getShortcut") return@newProxyInstance Unit
            return@newProxyInstance loadClass("com.android.launcher3.popup.SystemShortcut\$Install")
                .findConstructor { paramCount == 3 }
                .newInstance(args[0], args[1], args[2])
                .also { proxyInstances.add(it) }
        }
    }
}
