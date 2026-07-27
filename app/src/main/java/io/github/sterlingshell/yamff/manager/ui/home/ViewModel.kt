package io.github.sterlingshell.yamff.manager.ui.home

import androidx.lifecycle.ViewModel as AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.manager.service.IpcProxy
import io.github.sterlingshell.yamff.xposed.IOpenCountListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ViewModel : AndroidViewModel() {
    private val _openCount = MutableStateFlow(0)
    val openCount: StateFlow<Int> = _openCount

    val buildTimeSystem = IpcProxy.buildTime
    val buildTimeModule = BuildConfig.BUILD_TIME
    val versionName = IpcProxy.versionName ?: ""
    val versionCode = IpcProxy.versionCode

    private val _config = MutableStateFlow(gson.fromJson(IpcProxy.configJson, Config::class.java))
    val config: StateFlow<Config> = _config

    fun updateConfig(update: (Config) -> Unit) {
        val newConfig = _config.value.copy()
        update(newConfig)
        _config.value = newConfig
        IpcProxy.updateConfig(gson.toJson(newConfig))
    }

    private val openCountListener = object : IOpenCountListener.Stub() {
        override fun onUpdate(count: Int) {
            _openCount.value = count
        }
    }

    init {
        viewModelScope.launch {
            IpcProxy.registerOpenCountListener(openCountListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        IpcProxy.unregisterOpenCountListener(openCountListener)
    }

    fun createWindow() = IpcProxy.createWindow()
    fun openAppList() = IpcProxy.openAppList()
    fun currentToWindow() = IpcProxy.currentToWindow()
    fun resetAllWindow() = IpcProxy.resetAllWindow()
}