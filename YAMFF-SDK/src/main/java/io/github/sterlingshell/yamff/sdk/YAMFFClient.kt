package io.github.sterlingshell.yamff.sdk

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import io.github.sterlingshell.yamff.xposed.IFreeform

object YAMFFClient : IBinder.DeathRecipient {
    private const val TAG = "YAMFFClient"
    
    @Volatile
    private var service: IFreeform? = null

    internal fun init(service: IFreeform) {
        this.service = service
        try {
            service.asBinder().linkToDeath(this, 0)
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to link to death", e)
        }
    }

    override fun binderDied() {
        Log.w(TAG, "YAMFF Binder died")
        service = null
    }

    fun getService(): IFreeform? = service

    fun isActivated(): Boolean = service != null

    /**
     * Executes a block if YAMFF is available, otherwise returns null.
     */
    fun <T> runSafe(block: (IFreeform) -> T): T? {
        return try {
            service?.let { block(it) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Not authorized to call YAMFF", e)
            throw e
        } catch (e: RemoteException) {
            Log.e(TAG, "YAMFF IPC error", e)
            null
        }
    }
}
