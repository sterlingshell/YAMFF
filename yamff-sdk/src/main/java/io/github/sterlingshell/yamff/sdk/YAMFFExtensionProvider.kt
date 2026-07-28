package io.github.sterlingshell.yamff.sdk

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Binder
import io.github.sterlingshell.yamff.xposed.IFreeform

/**
 * Base ContentProvider for receiving the YAMFF Binder.
 * Extensions should inherit this and declare it in AndroidManifest.xml
 * with authority following "[packageName].yamff.provider" format.
 */
abstract class YAMFFExtensionProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, p: Array<String>?, s: String?, sa: Array<String>?, so: String?) = null
    override fun getType(uri: Uri) = null
    override fun insert(uri: Uri, v: ContentValues?) = null
    override fun delete(uri: Uri, s: String?, sa: Array<String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, sa: Array<String>?) = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        // Safety check: Only allow system_server (UID 1000) to push the binder
        if (Binder.getCallingUid() != 1000 || extras == null) return null
        
        val binder = extras.getBinder("binder")
        if (binder != null) {
            val service = IFreeform.Stub.asInterface(binder)
            YAMFFClient.init(service)
            onYAMFFConnected(service)
        }
        return Bundle()
    }

    /**
     * Called when the connection to YAMFF is established or updated.
     */
    open fun onYAMFFConnected(service: IFreeform) {}
}
