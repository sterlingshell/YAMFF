package io.github.duzhaokun123.yamf.manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.duzhaokun123.yamf.BuildConfig
import io.github.duzhaokun123.yamf.manager.services.YAMFManagerProxy
import io.github.duzhaokun123.yamf.xposed.IOpenCountListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _openCount = MutableStateFlow(0)
    val openCount: StateFlow<Int> = _openCount

    val buildTimeSystem = YAMFManagerProxy.buildTime
    val buildTimeModule = BuildConfig.BUILD_TIME
    val versionName = YAMFManagerProxy.versionName ?: ""
    val versionCode = YAMFManagerProxy.versionCode

    private val openCountListener = object : IOpenCountListener.Stub() {
        override fun onUpdate(count: Int) {
            _openCount.value = count
        }
    }

    init {
        viewModelScope.launch {
            YAMFManagerProxy.registerOpenCountListener(openCountListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        YAMFManagerProxy.unregisterOpenCountListener(openCountListener)
    }

    fun createWindow() = YAMFManagerProxy.createWindow()
    fun openAppList() = YAMFManagerProxy.openAppList()
    fun currentToWindow() = YAMFManagerProxy.currentToWindow()
    fun resetAllWindow() = YAMFManagerProxy.resetAllWindow()
}