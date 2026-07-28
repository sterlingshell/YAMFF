package io.github.sterlingshell.yamff.xposed.core

import android.app.ActivityManagerHidden
import android.content.AttributionSource
import android.content.pm.IPackageManager
import android.content.pm.IPackageManagerHidden
import android.os.Build
import android.os.Bundle
import android.os.ServiceManager
import dev.rikka.tools.refine.Refine
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.xposed.util.ext.log
import rikka.hidden.compat.ActivityManagerApis
import rikka.hidden.compat.adapter.UidObserverAdapter

object IpcEntry {
    const val TAG = "IpcEntry"

    private var earlyPms: IPackageManager? = null

    private val uidObserver = object : UidObserverAdapter() {
        override fun onUidActive(uid: Int) {
            try {
                // Use earlyPms if SystemServices is not yet initialized
                val authorizedPackages = ExtensionRegistry.instance.getAuthorizedPackages(uid, earlyPms)
                if (authorizedPackages.isNotEmpty()) {
                    log(TAG, "Authorized extension UID active: $uid, pushing binder to $authorizedPackages...")
                    authorizedPackages.forEach { pkg ->
                        sendBinderWithRetry(uid, pkg)
                    }
                }
            } catch (t: Throwable) {
                log(TAG, "uidObserver.onUidActive fatal crash suppressed", t)
            }
        }
    }

    private fun sendBinderWithRetry(uid: Int, packageName: String, retryCount: Int = 5) {
        kotlin.concurrent.thread {
            var currentRetry = 0
            val authority = packageName + io.github.sterlingshell.yamff.common.Constants.PROVIDER_SUFFIX
            while (currentRetry < retryCount) {
                try {
                    val userId = uid / 100000
                    val provider = ActivityManagerApis.getContentProviderExternal(
                        authority,
                        userId,
                        null,
                        null
                    )
                    if (provider != null) {
                        val extras = Bundle()
                        extras.putBinder("binder", IpcService)
                        val attr = AttributionSource.Builder(1000).setPackageName("android").build()
                        val reply = provider.call(attr, authority, "", null, extras)
                        if (reply != null) {
                            log(TAG, "Successfully sent binder to extension ($packageName, UID: $uid)")
                            return@thread
                        }
                    }
                    log(TAG, "Provider $authority not ready for UID $uid, retry ${currentRetry + 1}/$retryCount")
                } catch (e: Throwable) {
                    log(TAG, "Failed to send binder to $packageName (UID: $uid), retry ${currentRetry + 1}/$retryCount", e)
                }
                currentRetry++
                Thread.sleep(1000)
            }
            log(TAG, "Failed to send binder to $packageName after $retryCount retries (UID: $uid)")
        }
    }

    fun register(pms: IPackageManager) {
        try {
            log(TAG, "Init IpcEntry")
            earlyPms = pms
            
            waitSystemService()
            
            // Register observer for future processes
            ActivityManagerApis.registerUidObserver(
                uidObserver,
                ActivityManagerHidden.UID_OBSERVER_ACTIVE,
                ActivityManagerHidden.PROCESS_STATE_UNKNOWN,
                null
            )
            
            // Handle YAMFF Manager itself (self-push)
            runCatching {
                val packageUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Refine.unsafeCast<IPackageManagerHidden>(pms).getPackageUid(BuildConfig.APPLICATION_ID, 0L, 0)
                } else {
                    Refine.unsafeCast<IPackageManagerHidden>(pms).getPackageUid(BuildConfig.APPLICATION_ID, 0, 0)
                }
                if (packageUid != -1) {
                    log(TAG, "Self-pushing binder to Manager (UID: $packageUid)")
                    sendBinderWithRetry(packageUid, BuildConfig.APPLICATION_ID)
                }
            }
        } catch (t: Throwable) {
            log(TAG, "IpcEntry.register fatal crash suppressed", t)
        }
    }

    private fun waitSystemService() {
        while (ServiceManager.getService("activity") == null) {
            Thread.sleep(1000)
        }
    }
}
