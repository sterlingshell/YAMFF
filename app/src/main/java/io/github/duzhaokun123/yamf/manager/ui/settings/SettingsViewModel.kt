package io.github.duzhaokun123.yamf.manager.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import io.github.duzhaokun123.yamf.common.extensions.gson
import io.github.duzhaokun123.yamf.common.model.Config
import io.github.duzhaokun123.yamf.common.model.SurfaceType
import io.github.duzhaokun123.yamf.common.model.WindowStyle
import io.github.duzhaokun123.yamf.manager.services.YAMFManagerProxy

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preference = PreferenceManager.getDefaultSharedPreferences(application)

    var config: Config by mutableStateOf(gson.fromJson(YAMFManagerProxy.configJson, Config::class.java))
        private set

    var useAppList by mutableStateOf(preference.getBoolean("useAppList", true))
        private set

    fun updateConfig(update: (Config) -> Unit) {
        val newConfig = config.copy()
        update(newConfig)
        config = newConfig
        YAMFManagerProxy.updateConfig(gson.toJson(config))
    }

    fun updateUseAppList(value: Boolean) {
        useAppList = value
        preference.edit().putBoolean("useAppList", value).apply()
    }

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
        update(it.hookLauncher)
    }
    fun updateShowForceShowIME(value: Boolean) = updateConfig { it.showForceShowIME = value }
    fun updateHapticFeedback(value: Boolean) = updateConfig { it.hapticFeedback = value }
    fun updateExcludeSystemGesture(value: Boolean) = updateConfig { it.excludeSystemGesture = value }
    fun updateEnableFlickAway(value: Boolean) = updateConfig { it.enableFlickAway = value }
}