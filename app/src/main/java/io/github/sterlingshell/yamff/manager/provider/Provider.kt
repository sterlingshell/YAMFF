package io.github.sterlingshell.yamff.manager.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.manager.service.IpcProxy

class Provider: ContentProvider() {
    override fun onCreate() = false

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ) = null

    override fun getType(uri: Uri) = null

    override fun insert(uri: Uri, values: ContentValues?) = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ) = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val callingUid = android.os.Binder.getCallingUid()
        if (BuildConfig.DEBUG) {
            android.util.Log.i("YAMFF.Provider", "Provider.call: method=$method, uid=$callingUid")
        }
        
        if (callingUid != 1000 || extras == null) {
             if (BuildConfig.DEBUG) {
                 android.util.Log.w("YAMFF.Provider", "Unauthorized or empty call from UID $callingUid")
             }
             return null
        }
        
        val binder = extras.getBinder("binder") ?: run {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("YAMFF.Provider", "No binder in extras")
            }
            return null
        }
        
        if (BuildConfig.DEBUG) {
            android.util.Log.i("YAMFF.Provider", "Linking service from system...")
        }
        IpcProxy.linkService(binder)
        return Bundle().apply { putBoolean("success", true) }
    }
}
