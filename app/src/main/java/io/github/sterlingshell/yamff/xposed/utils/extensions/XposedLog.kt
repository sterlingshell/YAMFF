package io.github.sterlingshell.yamff.xposed.utils.extensions

import de.robv.android.xposed.XposedBridge

fun log(tag: String, message: String) {
    XposedBridge.log("[$tag] $message")
}

fun log(tag: String, message: String, t: Throwable) {
    XposedBridge.log("[$tag] $message")
    XposedBridge.log(t)
}
