package io.github.sterlingshell.yamff.xposed.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.IActivityTaskManager
import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.input.IInputManager
import android.os.ServiceManager
import android.os.UserManager
import android.view.IWindowManager
import android.view.WindowManager
import com.android.internal.statusbar.IStatusBarService
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import io.github.sterlingshell.yamff.xposed.utils.extensions.log

@SuppressLint("StaticFieldLeak")
object SystemServices {
    private const val TAG = "SystemServices"

    lateinit var windowManager: WindowManager
        private set
    lateinit var iWindowManager: IWindowManager
        private set
    lateinit var inputManager: IInputManager
        private set
    lateinit var displayManager: DisplayManager
        private set
    lateinit var activityTaskManager: IActivityTaskManager
        private set
    lateinit var packageManager: PackageManager
        private set
    lateinit var activityManager: ActivityManager
        private set
    lateinit var userManager: UserManager
        private set
    lateinit var iPackageManager: IPackageManager
        private set
    lateinit var iStatusBarService: IStatusBarService
        private set
    lateinit var activityManagerService: Any
        private set
    lateinit var systemContext: Context
        private set
    val systemUiContext: Context
        get() = activityManagerService.getObjectAs("mUiContext")


    var initialized = false
        private set

    fun checkInitialized(): Boolean {
        if (!initialized) {
            log(TAG, "SystemServices NOT fully initialized, some features may not work")
        }
        return initialized
    }

    fun init(activityManagerService: Any) {
        this.activityManagerService = activityManagerService
        var success = true
        runCatching {
            systemContext = activityManagerService.getObjectAs("mContext")
            windowManager = systemContext.getSystemService(WindowManager::class.java)
            displayManager = systemContext.getSystemService(DisplayManager::class.java)
            packageManager = systemContext.packageManager
            activityManager = systemContext.getSystemService(ActivityManager::class.java)
            userManager = systemContext.getSystemService(UserManager::class.java)
        }.onFailure { 
            log(TAG, "Failed to init basic services from context", it)
            success = false
        }

        runCatching { iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window")) }.onFailure { log(TAG, "Failed to get window service", it); success = false }
        runCatching { inputManager = IInputManager.Stub.asInterface(ServiceManager.getService("input")) }.onFailure { log(TAG, "Failed to get input service", it); success = false }
        runCatching { activityTaskManager = IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task")) }.onFailure { log(TAG, "Failed to get activity_task service", it); success = false }
        runCatching { iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package")) }.onFailure { log(TAG, "Failed to get package service", it); success = false }
        runCatching { iStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar")) }.onFailure { log(TAG, "Failed to get statusbar service", it); success = false }
        
        initialized = success
    }
}
