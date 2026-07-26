package io.github.sterlingshell.yamff.xposed.utils

import android.annotation.SuppressLint
import android.widget.Toast

@SuppressLint("StaticFieldLeak")
object TipUtil {
    fun showToast(msg: String) {
        Toast.makeText(SystemServices.systemContext, "[YAMFF] $msg", Toast.LENGTH_LONG).show()
    }
}
