package io.github.sterlingshell.yamff.xposed.util.ext

import android.util.Log

fun log(tag: String, message: String) {
    if (isXposedAvailable()) {
        XposedLogProxy.log("[$tag] $message")
    } else {
        Log.d(tag, message)
    }
}

fun log(tag: String, message: String, t: Throwable) {
    if (isXposedAvailable()) {
        XposedLogProxy.log("[$tag] $message", t)
    } else {
        Log.e(tag, message, t)
    }
}

private fun isXposedAvailable(): Boolean {
    return try {
        Class.forName("de.robv.android.xposed.XposedBridge", false, Log::class.java.classLoader)
        true
    } catch (e: Throwable) {
        false
    }
}

private object XposedLogProxy {
    fun log(msg: String) {
        de.robv.android.xposed.XposedBridge.log(msg)
    }
    fun log(msg: String, t: Throwable) {
        de.robv.android.xposed.XposedBridge.log(msg)
        de.robv.android.xposed.XposedBridge.log(t)
    }
}
