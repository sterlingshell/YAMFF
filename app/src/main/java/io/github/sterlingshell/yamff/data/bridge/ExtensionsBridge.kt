package io.github.sterlingshell.yamff.data.bridge

import android.app.Application
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.manager.service.IpcProxy
import io.github.sterlingshell.yamff.xposed.IExtensionsChangeListener
import io.github.sterlingshell.yamff.xposed.sys.ExtensionDiscovery
import io.github.sterlingshell.yamff.xposed.sys.ExtensionMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionsBridge(private val application: Application) {

    private val _extensions = MutableStateFlow<List<ExtensionMetadata>>(emptyList())
    val extensions: StateFlow<List<ExtensionMetadata>> = _extensions.asStateFlow()

    private val extensionsChangeListener = object : IExtensionsChangeListener.Stub() {
        override fun onExtensionsChanged(extensionsJson: String) {
            refreshWithJson(extensionsJson)
        }
    }

    init {
        IpcProxy.registerExtensionsChangeListener(extensionsChangeListener)
        refresh()
    }

    private fun refreshWithJson(json: String) {
        runCatching {
            val authorizedPackages: Set<String> = gson.fromJson(
                json,
                object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
            )
            _extensions.value = ExtensionDiscovery.discover(application, authorizedPackages)
        }
    }

    fun refresh() {
        runCatching {
            refreshWithJson(IpcProxy.getExtensionsJson())
        }
    }

    fun toggleAuthorization(packageName: String, authorized: Boolean) {
        runCatching {
            val authorizedJson = IpcProxy.getExtensionsJson()
            val currentAuthorized: MutableSet<String> = gson.fromJson(
                authorizedJson,
                object : com.google.gson.reflect.TypeToken<MutableSet<String>>() {}.type
            )
            if (authorized) {
                currentAuthorized.add(packageName)
            } else {
                currentAuthorized.remove(packageName)
            }
            IpcProxy.updateExtensions(gson.toJson(currentAuthorized))
            // The listener will trigger local update
        }
    }
}
