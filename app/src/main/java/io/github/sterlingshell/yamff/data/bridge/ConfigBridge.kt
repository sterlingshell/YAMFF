package io.github.sterlingshell.yamff.data.bridge

import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.manager.service.IpcProxy
import io.github.sterlingshell.yamff.xposed.IConfigChangeListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge responsible for managing the connection to the YAMFF Xposed service
 * and providing a reactive stream of the current configuration.
 */
class ConfigBridge {

    private val _config = MutableStateFlow(loadInitialConfig())
    val config: StateFlow<Config> = _config.asStateFlow()

    private val configChangeListener = object : IConfigChangeListener.Stub() {
        override fun onConfigChanged(configJson: String) {
            runCatching {
                val newConfig = gson.fromJson(configJson, Config::class.java)
                _config.value = newConfig
            }
        }
    }

    init {
        // IpcProxy handles the queue internally if service is null.
        IpcProxy.registerConfigChangeListener(configChangeListener)
    }

    private fun loadInitialConfig(): Config {
        return runCatching {
            gson.fromJson(IpcProxy.configJson, Config::class.java)
        }.getOrDefault(Config())
    }

    fun updateConfig(newConfig: Config) {
        runCatching {
            val json = gson.toJson(newConfig)
            IpcProxy.updateConfig(json)
            // Local update for immediate feedback
            _config.value = newConfig
        }
    }

    fun refresh() {
        _config.value = loadInitialConfig()
    }
}
