package io.github.sterlingshell.yamff.manager.ui.extensions

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.manager.service.IpcProxy
import io.github.sterlingshell.yamff.xposed.IExtensionsChangeListener
import io.github.sterlingshell.yamff.xposed.sys.ExtensionDiscovery
import io.github.sterlingshell.yamff.xposed.sys.ExtensionMetadata

class ViewModel(application: Application) : AndroidViewModel(application) {
    var extensions by mutableStateOf(loadExtensions())
        private set

    private val extensionsChangeListener = object : IExtensionsChangeListener.Stub() {
        override fun onExtensionsChanged(extensionsJson: String) {
            val authorizedPackages: Set<String> = gson.fromJson(
                extensionsJson,
                object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
            )
            extensions = ExtensionDiscovery.discover(getApplication(), authorizedPackages)
        }
    }

    init {
        IpcProxy.registerExtensionsChangeListener(extensionsChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        IpcProxy.unregisterExtensionsChangeListener(extensionsChangeListener)
    }

    private fun loadExtensions(): List<ExtensionMetadata> {
        val authorizedJson = IpcProxy.getExtensionsJson()
        val authorizedPackages: Set<String> = gson.fromJson(
            authorizedJson,
            object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
        )
        return ExtensionDiscovery.discover(getApplication(), authorizedPackages)
    }

    fun refresh() {
        extensions = loadExtensions()
    }

    fun toggleAuthorization(packageName: String, authorized: Boolean) {
        val currentAuthorized: MutableSet<String> = gson.fromJson(
            IpcProxy.getExtensionsJson(),
            object : com.google.gson.reflect.TypeToken<MutableSet<String>>() {}.type
        )
        if (authorized) {
            currentAuthorized.add(packageName)
        } else {
            currentAuthorized.remove(packageName)
        }
        IpcProxy.updateExtensions(gson.toJson(currentAuthorized))
        refresh()
    }
}
