package io.github.sterlingshell.yamff.manager.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import io.github.sterlingshell.yamff.manager.services.YAMFFManagerProxy

class ServiceProvider: ContentProvider() {
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
        if (android.os.Binder.getCallingUid() != 1000 || extras == null) return null
        val binder = extras.getBinder("binder") ?: return null
        YAMFFManagerProxy.linkService(binder)
        return Bundle()
    }
}