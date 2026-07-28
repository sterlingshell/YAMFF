package io.github.sterlingshell.yamff.xposed.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.xposed.util.ext.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

class ConfigManager {
    companion object {
        private const val TAG = "ConfigManager"
        private val KEY_CONFIG_JSON = stringPreferencesKey("config_json")
        private const val CONFIG_FILE_PATH = "/data/system/yamff.json"
        
        @Volatile
        lateinit var instance: ConfigManager
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(CONFIG_FILE_PATH) }
    )

    @Volatile
    var config: Config = Config()
        private set

    init {
        instance = this
        loadConfig()
    }

    fun loadConfig() {
        config = try {
            runBlocking {
                val json = dataStore.data.map { it[KEY_CONFIG_JSON] }.first()
                if (json != null) {
                    gson.fromJson(json, Config::class.java).validateAndFix()
                } else {
                    // Fallback to old file if exists
                    val oldFile = File(CONFIG_FILE_PATH)
                    if (oldFile.exists() && !oldFile.name.endsWith(".pb")) {
                         // Note: PreferenceDataStore uses .preferences_pb by default if not specified, 
                         // but here we are producing a File. 
                         // Actually, if we use the same path, it might conflict. 
                         // Better use a different path for DataStore or handle migration.
                         val oldConfig = tryLoadOld(oldFile)
                         if (oldConfig != null) {
                             updateConfig(gson.toJson(oldConfig))
                             oldConfig
                         } else Config().validateAndFix()
                    } else Config().validateAndFix()
                }
            }
        } catch (t: Throwable) {
            log(TAG, "Failed to load config, using defaults", t)
            Config().validateAndFix()
        }
    }

    private fun tryLoadOld(file: File): Config? {
        return runCatching {
            file.bufferedReader().use {
                gson.fromJson(it, Config::class.java)
            }
        }.getOrNull()
    }

    fun updateConfig(newConfigJson: String) {
        runBlocking {
            try {
                val newConfig = gson.fromJson(newConfigJson, Config::class.java).validateAndFix()
                dataStore.edit { prefs ->
                    prefs[KEY_CONFIG_JSON] = newConfigJson
                }
                config = newConfig
                log(TAG, "Config updated")
            } catch (t: Throwable) {
                log(TAG, "Failed to update config", t)
            }
        }
    }
}
