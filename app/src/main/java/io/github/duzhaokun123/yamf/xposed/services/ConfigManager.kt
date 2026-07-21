package io.github.duzhaokun123.yamf.xposed.services

import android.util.AtomicFile
import android.util.Log
import io.github.duzhaokun123.yamf.common.extensions.gson
import io.github.duzhaokun123.yamf.common.model.Config
import io.github.duzhaokun123.yamf.xposed.utils.extensions.log
import java.io.File
import java.io.FileOutputStream

object ConfigManager {
    private const val TAG = "ConfigManager"
    private val configFile = File("/data/system/yamf.json")
    private val atomicFile = AtomicFile(configFile)

    @Volatile
    var config: Config = Config()
        private set

    fun loadConfig() {
        config = if (configFile.exists()) {
            runCatching {
                atomicFile.openRead().use {
                    gson.fromJson(it.reader(), Config::class.java)
                }
            }.getOrElse { e ->
                log(TAG, "Failed to load config, using default", e)
                Config()
            }
        } else {
            Config()
        }
        log(TAG, "Config loaded: $config")
    }

    fun updateConfig(newConfigJson: String) {
        runCatching {
            // Validate JSON first
            val newConfig = gson.fromJson(newConfigJson, Config::class.java)
            // Try saving to disk first
            saveConfig(newConfigJson)
            // Update memory only if save succeeded
            config = newConfig
        }.onFailure { e ->
            log(TAG, "Failed to update config", e)
        }
    }

    private fun saveConfig(json: String) {
        var fos: FileOutputStream? = null
        try {
            fos = atomicFile.startWrite()
            fos.write(json.toByteArray())
            atomicFile.finishWrite(fos)
            Log.d(TAG, "Config saved successfully")
        } catch (e: Exception) {
            atomicFile.failWrite(fos)
            throw e
        }
    }
}
