package io.github.sterlingshell.yamff.xposed.core

import android.content.pm.IPackageManagerHidden
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.rikka.tools.refine.Refine
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.Constants
import io.github.sterlingshell.yamff.common.ext.gson
import io.github.sterlingshell.yamff.xposed.sys.SystemServices
import io.github.sterlingshell.yamff.xposed.util.ext.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Arrays

class ExtensionRegistry {
    companion object {
        private const val TAG = "ExtensionRegistry"
        private val KEY_AUTHORIZED_PACKAGES = stringSetPreferencesKey("authorized_packages")

        @Volatile
        lateinit var instance: ExtensionRegistry
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(Constants.EXTENSIONS_CONFIG_FILE) }
    )

    private var mySignatures: Array<Signature>? = null

    init {
        instance = this
        migrateOldConfig()
    }

    private fun migrateOldConfig() {
        runBlocking {
            val oldFile = File(Constants.OLD_EXTENSIONS_CONFIG_FILE)
            if (oldFile.exists()) {
                try {
                    val authorized: Set<String> = oldFile.bufferedReader().use {
                        gson.fromJson(it, object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type)
                    }
                    dataStore.edit { prefs ->
                        val current = prefs[KEY_AUTHORIZED_PACKAGES] ?: emptySet()
                        prefs[KEY_AUTHORIZED_PACKAGES] = current + authorized
                    }
                    // oldFile.delete()
                    log(TAG, "Migrated old extensions config")
                } catch (t: Throwable) {
                    log(TAG, "Failed to migrate old extensions config", t)
                }
            }
        }
    }

    fun isAuthorized(uid: Int, pms: Any? = null): Boolean {
        return try {
            val pm = pms ?: runCatching { SystemServices.iPackageManager }.getOrNull() ?: return false
            val packages = Refine.unsafeCast<IPackageManagerHidden>(pm).getPackagesForUid(uid) ?: return false
            
            for (pkg in packages) {
                if (isAuthorized(pkg, uid, pm)) return true
            }
            false
        } catch (t: Throwable) {
            log(TAG, "isAuthorized(uid) crash suppressed", t)
            false
        }
    }

    fun isAuthorized(packageName: String, uid: Int, pms: Any? = null): Boolean {
        return try {
            // 1. Check permission
            if (!hasPermission(packageName, uid, pms)) return false

            // 2. Check signature (Implicit trust)
            if (isSameSignature(packageName, uid, pms)) return true

            // 3. Check user whitelist (Explicit trust)
            runBlocking {
                val authorized = dataStore.data.map { it[KEY_AUTHORIZED_PACKAGES] ?: emptySet() }.first()
                authorized.contains(packageName)
            }
        } catch (t: Throwable) {
            log(TAG, "isAuthorized(pkg) crash suppressed", t)
            false
        }
    }

    private fun hasPermission(packageName: String, uid: Int, pms: Any? = null): Boolean {
        return try {
            val pm = pms ?: SystemServices.iPackageManager
            val result = Refine.unsafeCast<IPackageManagerHidden>(pm).checkPermission(
                Constants.PERMISSION_MANAGE_FREEFORM,
                packageName,
                uid / 100000
            )
            result == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            log(TAG, "hasPermission crash suppressed", t)
            false
        }
    }

    private fun isSameSignature(packageName: String, uid: Int, pms: Any? = null): Boolean {
        val mySigs = getMySignatures(pms) ?: return false
        val targetSigs = getSignatures(packageName, uid, pms) ?: return false
        
        if (mySigs.size != targetSigs.size) return false
        
        for (i in mySigs.indices) {
            val sig1 = mySigs[i].toByteArray()
            val sig2 = targetSigs[i].toByteArray()
            if (!Arrays.equals(sig1, sig2)) return false
        }
        return true
    }

    private fun getMySignatures(pms: Any? = null): Array<Signature>? {
        if (mySignatures == null) {
            mySignatures = getSignatures(BuildConfig.APPLICATION_ID, android.os.Process.myUid(), pms)
        }
        return mySignatures
    }

    private fun getSignatures(packageName: String, uid: Int, pms: Any? = null): Array<Signature>? {
        return try {
            val pm = pms ?: SystemServices.iPackageManager
            val pmHidden = Refine.unsafeCast<IPackageManagerHidden>(pm)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            
            val pi: PackageInfo = if (Build.VERSION.SDK_INT >= 33) {
                pmHidden.getPackageInfo(packageName, flags.toLong(), uid / 100000)
            } else {
                pmHidden.getPackageInfo(packageName, flags, uid / 100000)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pi.signatures
            }
        } catch (t: Throwable) {
            log(TAG, "getSignatures crash suppressed", t)
            null
        }
    }

    fun setAuthorized(packageName: String, authorized: Boolean) {
        runBlocking {
            dataStore.edit { prefs ->
                val current = prefs[KEY_AUTHORIZED_PACKAGES] ?: emptySet()
                val next = current.toMutableSet()
                if (authorized) next.add(packageName) else next.remove(packageName)
                prefs[KEY_AUTHORIZED_PACKAGES] = next
            }
        }
    }

    fun getAuthorizedPackages(uid: Int, pms: Any? = null): List<String> {
        return try {
            val pm = pms ?: runCatching { SystemServices.iPackageManager }.getOrNull() ?: return emptyList()
            val packages = Refine.unsafeCast<IPackageManagerHidden>(pm).getPackagesForUid(uid) ?: return emptyList()
            
            packages.filter { isAuthorized(it, uid, pm) }
        } catch (t: Throwable) {
            log(TAG, "getAuthorizedPackages crash suppressed", t)
            return emptyList()
        }
    }

    fun getAllAuthorizedPackages(): Set<String> {
        return runBlocking {
            dataStore.data.map { it[KEY_AUTHORIZED_PACKAGES] ?: emptySet() }.first()
        }
    }
}
