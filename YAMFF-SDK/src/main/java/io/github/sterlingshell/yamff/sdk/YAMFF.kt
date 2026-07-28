package io.github.sterlingshell.yamff.sdk

import io.github.sterlingshell.yamff.xposed.IConfigChangeListener
import io.github.sterlingshell.yamff.xposed.IExtensionsChangeListener
import io.github.sterlingshell.yamff.xposed.IOpenCountListener

/**
 * High-level API for YAMFF Extensions.
 */
object YAMFF {
    
    val isActivated: Boolean get() = YAMFFClient.isActivated()

    fun createWindow() {
        YAMFFClient.runSafe { it.createWindow() }
    }

    fun currentToWindow() {
        YAMFFClient.runSafe { it.currentToWindow() }
    }

    fun openAppList() {
        YAMFFClient.runSafe { it.openAppList() }
    }

    fun resetAllWindow() {
        YAMFFClient.runSafe { it.resetAllWindow() }
    }

    fun getVersionName(): String? {
        return YAMFFClient.runSafe { it.versionName }
    }

    fun getVersionCode(): Int {
        return YAMFFClient.runSafe { it.versionCode } ?: 0
    }

    fun getConfigJson(): String? {
        return YAMFFClient.runSafe { it.configJson }
    }

    fun registerOpenCountListener(listener: IOpenCountListener) {
        YAMFFClient.runSafe { it.registerOpenCountListener(listener) }
    }

    fun unregisterOpenCountListener(listener: IOpenCountListener) {
        YAMFFClient.runSafe { it.unregisterOpenCountListener(listener) }
    }

    fun registerConfigChangeListener(listener: IConfigChangeListener) {
        YAMFFClient.runSafe { it.registerConfigChangeListener(listener) }
    }

    fun unregisterConfigChangeListener(listener: IConfigChangeListener) {
        YAMFFClient.runSafe { it.unregisterConfigChangeListener(listener) }
    }
}
