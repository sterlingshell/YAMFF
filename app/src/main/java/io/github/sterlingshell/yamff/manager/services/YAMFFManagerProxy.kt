package io.github.sterlingshell.yamff.manager.services

import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.util.Log
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import io.github.sterlingshell.yamff.xposed.IYAMFFManager
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object YAMFFManagerProxy : IYAMFFManager, DeathRecipient {
    private const val TAG = "YAMFFManagerProxy"

    private class ServiceProxy(private val obj: IYAMFFManager) : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
            try {
                val result = method.invoke(obj, *args.orEmpty())
                if (BuildConfig.DEBUG) {
                    val logMsg = if (result == null) {
                        "Call service method ${method.name}"
                    } else {
                        "Call service method ${method.name} with result " + result.toString().take(20)
                    }
                    Log.d(TAG, logMsg)
                }
                return result
            } catch (e: InvocationTargetException) {
                Log.e(TAG, "IPC call failed: ${method.name}", e.targetException)
                throw e.targetException
            } catch (e: Exception) {
                Log.e(TAG, "Proxy call failed: ${method.name}", e)
                throw e
            }
        }
    }

    @Volatile
    private var service: IYAMFFManager? = null

    val isServiceAlive: Boolean get() = service != null

    fun linkService(binder: IBinder) {
        service = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(IYAMFFManager::class.java),
            ServiceProxy(IYAMFFManager.Stub.asInterface(binder))
        ) as IYAMFFManager
        binder.linkToDeath(this, 0)
    }

    override fun binderDied() {
        service = null
        Log.e(TAG, "Binder died")
    }

    override fun asBinder() = service?.asBinder()

    override fun getVersionName(): String? {
        return service?.versionName
    }

    override fun getVersionCode() = service?.versionCode ?: 0

    override fun getUid() = service?.uid ?: -1

    override fun createWindow() {
        service?.createWindow()
    }

    override fun getBuildTime(): Long {
        return service?.buildTime ?: 0
    }

    override fun getConfigJson(): String {
        return service?.configJson ?: "{}"
    }

    override fun updateConfig(newConfig: String) {
        service?.updateConfig(newConfig)
    }

    override fun registerOpenCountListener(iOpenCountListener: IOpenCountListener) {
        service?.registerOpenCountListener(iOpenCountListener)
    }

    override fun unregisterOpenCountListener(iOpenCountListener: IOpenCountListener) {
        service?.unregisterOpenCountListener(iOpenCountListener)
    }

    override fun openAppList() {
        service?.openAppList()
    }

    override fun currentToWindow() {
        service?.currentToWindow()
    }

    override fun resetAllWindow() {
        service?.resetAllWindow()
    }
}