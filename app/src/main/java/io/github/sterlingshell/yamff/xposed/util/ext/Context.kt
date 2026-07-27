package io.github.sterlingshell.yamff.xposed.util.ext

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextParams
import android.content.Intent
import android.os.Build

val emptyContextParams = ContextParams.Builder().build()

fun Context.createContext() = createContext(emptyContextParams)

fun Context.registerReceiver(action: String, onReceive: BroadcastReceiver.(Context, Intent) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
              onReceive(this, context, intent)
          }
      }, android.content.IntentFilter(action), Context.RECEIVER_EXPORTED)
    } else {
        registerReceiver(object : BroadcastReceiver() {
          override fun onReceive(context: Context, intent: Intent) {
              onReceive(this, context, intent)
          }
      }, android.content.IntentFilter(action))
    }
}
