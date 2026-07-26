package io.github.sterlingshell.yamff.manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.extensions.gson
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.manager.services.YAMFFManagerProxy
import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _openCount = MutableStateFlow(0)
    val openCount: StateFlow<Int> = _openCount

    val buildTimeSystem = YAMFFManagerProxy.buildTime
    val buildTimeModule = BuildConfig.BUILD_TIME
    val versionName = YAMFFManagerProxy.versionName ?: ""
    val versionCode = YAMFFManagerProxy.versionCode

    private val _config = MutableStateFlow(gson.fromJson(YAMFFManagerProxy.configJson, Config::class.java))
    val config: StateFlow<Config> = _config

    fun updateConfig(update: (Config) -> Unit) {
        val newConfig = _config.value.copy()
        update(newConfig)
        _config.value = newConfig
        YAMFFManagerProxy.updateConfig(gson.toJson(newConfig))
    }

    private val openCountListener = object : IOpenCountListener.Stub() {
        override fun onUpdate(count: Int) {
            _openCount.value = count
        }
    }

    init {
        viewModelScope.launch {
            YAMFFManagerProxy.registerOpenCountListener(openCountListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        YAMFFManagerProxy.unregisterOpenCountListener(openCountListener)
    }

    fun createWindow() = YAMFFManagerProxy.createWindow()
    fun openAppList() = YAMFFManagerProxy.openAppList()
    fun currentToWindow() = YAMFFManagerProxy.currentToWindow()
    fun resetAllWindow() = YAMFFManagerProxy.resetAllWindow()
}