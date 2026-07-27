package io.github.sterlingshell.yamff.xposed.core

import android.util.AtomicFile
import android.util.Log
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.xposed.util.ext.log
import java.io.File
import java.io.FileOutputStream

object ConfigManager {
    private const val TAG = "ConfigManager"
    private val configFile = File("/data/system/yamff.json")
    private val backupFile = File("/data/system/yamff.json.bak")
    private val atomicFile = AtomicFile(configFile)

    @Volatile
    var config: Config = Config()
        private set

    fun loadConfig() {
        var loadedConfig: Config? = null

        // 1. Try primary file
        if (configFile.exists()) {
            loadedConfig = tryLoad(configFile)
        }

        // 2. Try backup file if primary failed
        if (loadedConfig == null && backupFile.exists()) {
            log(TAG, "Primary config failed, trying backup...")
            loadedConfig = tryLoad(backupFile)
        }

        if (loadedConfig != null) {
            val oldVersion = loadedConfig.version
            loadedConfig.validateAndFix()
            
            if (oldVersion < Config.CURRENT_VERSION) {
                log(TAG, "Migrating config from $oldVersion to ${Config.CURRENT_VERSION}")
                loadedConfig.version = Config.CURRENT_VERSION
                config = loadedConfig
                saveConfig(gson.toJson(config))
            } else {
                config = loadedConfig
            }
        } else {
            log(TAG, "No valid config found, using defaults")
            config = Config().validateAndFix()
            // Save defaults if file doesn't exist
            if (!configFile.exists()) {
                saveConfig(gson.toJson(config))
            }
        }
        log(TAG, "Config initialized: $config")
    }

    private fun tryLoad(file: File): Config? {
        return runCatching {
            file.bufferedReader().use {
                gson.fromJson(it, Config::class.java)
            }
        }.getOrNull()
    }

    fun updateConfig(newConfigJson: String) {
        runCatching {
            val newConfig = gson.fromJson(newConfigJson, Config::class.java)
            newConfig.validateAndFix()
            newConfig.version = Config.CURRENT_VERSION
            
            saveConfig(newConfigJson)
            config = newConfig
            log(TAG, "Config updated via IPC")
        }.onFailure { e ->
            log(TAG, "Failed to update config", e)
        }
    }

    private fun saveConfig(json: String) {
        // Create backup before writing
        if (configFile.exists()) {
            runCatching {
                configFile.copyTo(backupFile, overwrite = true)
            }
        }

        var fos: FileOutputStream? = null
        try {
            fos = atomicFile.startWrite()
            fos.write(json.toByteArray())
            atomicFile.finishWrite(fos)
            Log.d(TAG, "Config saved successfully")
        } catch (e: Exception) {
            atomicFile.failWrite(fos)
            log(TAG, "Failed to save config", e)
        }
    }
}
