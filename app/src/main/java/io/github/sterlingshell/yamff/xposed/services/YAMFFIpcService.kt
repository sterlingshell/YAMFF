package io.github.sterlingshell.yamff.xposed.services

import android.app.ActivityManagerHidden
import android.content.AttributionSource
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Bundle
import android.os.ServiceManager
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.xposed.utils.extensions.log
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.adapter.UidObserverAdapter

object YAMFFIpcService {
    const val TAG = "YAMFFIpcService"
    const val PROVIDER_AUTHORITY = "io.github.sterlingshell.yamff.ServiceProvider"

    private var appId = -1

    private val uidObserver = object : UidObserverAdapter() {
        override fun onUidActive(uid: Int) {
            if (appId == -1 || (uid % 100000) != appId) return
            log(TAG, "Target app UID active: $uid, pushing binder...")
            sendBinderWithRetry(uid)
        }
    }

    private fun sendBinderWithRetry(uid: Int, retryCount: Int = 5) {
        kotlin.concurrent.thread {
            var currentRetry = 0
            while (currentRetry < retryCount) {
                try {
                    val userId = uid / 100000
                    val provider = ActivityManagerApis.getContentProviderExternal(
                        PROVIDER_AUTHORITY,
                        userId,
                        null,
                        null
                    )
                    if (provider != null) {
                        val extras = Bundle()
                        extras.putBinder("binder", YAMFFServer)
                        val attr = AttributionSource.Builder(1000).setPackageName("android").build()
                        val reply = provider.call(attr, PROVIDER_AUTHORITY, "", null, extras)
                        if (reply != null) {
                            log(TAG, "Successfully sent binder to app (UID: $uid)")
                            return@thread
                        }
                    }
                    log(TAG, "Provider not ready for UID $uid, retry ${currentRetry + 1}/$retryCount")
                } catch (e: Throwable) {
                    log(TAG, "Failed to send binder to app (UID: $uid), retry ${currentRetry + 1}/$retryCount", e)
                }
                currentRetry++
                Thread.sleep(1000)
            }
            log(TAG, "Failed to send binder to app after $retryCount retries (UID: $uid)")
        }
    }

    fun register(pms: IPackageManager) {
        log(TAG, "Init YAMFFService")
        val packageUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getPackageUid(BuildConfig.APPLICATION_ID, 0L, 0)
        } else {
            pms.getPackageUid(BuildConfig.APPLICATION_ID, 0, 0)
        }
        appId = packageUid % 100000
        log(TAG, "App AppID: $appId")
        log(TAG, "Register uid observer")

        waitSystemService("activity")
        ActivityManagerApis.registerUidObserver(
            uidObserver,
            ActivityManagerHidden.UID_OBSERVER_ACTIVE,
            ActivityManagerHidden.PROCESS_STATE_UNKNOWN,
            null
        )
        
        // Initial attempt for user 0
        sendBinderWithRetry(appId)
    }

    private fun waitSystemService(name: String) {
        while (ServiceManager.getService(name) == null) {
            Thread.sleep(1000)
        }
    }
}
