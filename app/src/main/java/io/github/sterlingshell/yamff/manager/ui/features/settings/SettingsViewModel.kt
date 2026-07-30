package io.github.sterlingshell.yamff.manager.ui.features.settings

import androidx.lifecycle.ViewModel
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.common.model.DpiMode
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.common.model.WindowStyle
import io.github.sterlingshell.yamff.data.bridge.ConfigBridge
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val configBridge: ConfigBridge
) : ViewModel() {
    
    val config: StateFlow<Config> = configBridge.config

    val useAppList: Boolean get() = config.value.useAppList

    fun updateConfig(update: (Config) -> Unit) {
        val newConfig = config.value.copy()
        // Deep copy nested objects manually because Config.copy() is shallow
        newConfig.hookLauncher = config.value.hookLauncher.copy()
        
        update(newConfig)
        configBridge.updateConfig(newConfig)
    }

    fun updateUseAppList(value: Boolean) = updateConfig { it.useAppList = value }
    fun updateEnableStatusAnimations(value: Boolean) = updateConfig { it.enableStatusAnimations = value }

    // Individual update helpers for convenience
    fun updateDensityDpi(value: Int) = updateConfig { it.densityDpi = value }
    fun updateDpiMode(value: Int) = updateConfig { it.dpiMode = DpiMode.fromInt(value) }
    fun updateAutoDpiTargetWidth(value: Int) = updateConfig { it.autoDpiTargetWidth = value }
    fun updateAutoDpiOffset(value: Int) = updateConfig { it.autoDpiOffset = value }
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
        update(it.hookLauncher)
    }
    fun updateShowForceShowIME(value: Boolean) = updateConfig { it.showForceShowIME = value }
    fun updateHapticFeedback(value: Boolean) = updateConfig { it.hapticFeedback = value }
    fun updateExcludeSystemGesture(value: Boolean) = updateConfig { it.excludeSystemGesture = value }
    fun updateEnableFlickAway(value: Boolean) = updateConfig { it.enableFlickAway = value }
}
