package io.github.sterlingshell.yamff.manager.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.common.model.WindowStyle
import io.github.sterlingshell.yamff.manager.service.IpcProxy
import io.github.sterlingshell.yamff.xposed.IConfigChangeListener

class ViewModel(application: Application) : AndroidViewModel(application) {
    var config: Config by mutableStateOf(gson.fromJson(IpcProxy.configJson, Config::class.java))
        private set

    private val configChangeListener = object : IConfigChangeListener.Stub() {
        override fun onConfigChanged(configJson: String) {
            config = gson.fromJson(configJson, Config::class.java)
        }
    }

    init {
        IpcProxy.registerConfigChangeListener(configChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        IpcProxy.unregisterConfigChangeListener(configChangeListener)
    }

    val useAppList: Boolean get() = config.useAppList

    fun updateConfig(update: (Config) -> Unit) {
        val newConfig = config.copy()
        // Deep copy nested objects manually because Config.copy() is shallow
        newConfig.hookLauncher = config.hookLauncher.copy()
        
        update(newConfig)
        config = newConfig
        IpcProxy.updateConfig(gson.toJson(config))
    }

    fun updateUseAppList(value: Boolean) = updateConfig { it.useAppList = value }
    fun updateEnableStatusAnimations(value: Boolean) = updateConfig { it.enableStatusAnimations = value }

    // Individual update helpers for convenience
    fun updateDensityDpi(value: Int) = updateConfig { it.densityDpi = value }
    fun updateFlags(value: Int) = updateConfig { it.flags = value }
    fun updateWindowStyle(value: Int) = updateConfig { it.windowStyle = WindowStyle.fromInt(value) }
    fun updateColoredController(value: Boolean) = updateConfig { it.coloredController = value }
    fun updateWindowfy(value: Int) = updateConfig { it.windowfy = value }
    fun updateSurfaceView(value: Int) = updateConfig { it.surfaceView = SurfaceType.fromInt(value) }
    fun updateRecentsBackHome(value: Boolean) = updateConfig { it.recentsBackHome = value }
    fun updateShowImeInWindow(value: Boolean) = updateConfig { it.showImeInWindow = value }
    fun updateDefaultWindowSize(width: Int, height: Int) = updateConfig {
        it.defaultWindowWidth = width
        it.defaultWindowHeight = height
    }
    fun updateHookLauncher(update: (Config.HookLauncher) -> Unit) = updateConfig {
        // Deep copy already handled in updateConfig, but let's be safe
        update(it.hookLauncher)
    }
    fun updateShowForceShowIME(value: Boolean) = updateConfig { it.showForceShowIME = value }
    fun updateHapticFeedback(value: Boolean) = updateConfig { it.hapticFeedback = value }
    fun updateExcludeSystemGesture(value: Boolean) = updateConfig { it.excludeSystemGesture = value }
    fun updateEnableFlickAway(value: Boolean) = updateConfig { it.enableFlickAway = value }
}