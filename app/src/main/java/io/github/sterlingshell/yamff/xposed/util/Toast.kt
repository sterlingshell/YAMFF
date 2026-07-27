package io.github.sterlingshell.yamff.xposed.util

import android.annotation.SuppressLint
import android.widget.Toast as AndroidToast
import io.github.sterlingshell.yamff.xposed.sys.SystemServices

@SuppressLint("StaticFieldLeak")
object Toast {
    fun show(msg: String) {
        AndroidToast.makeText(SystemServices.systemContext, "[Freeform] $msg", AndroidToast.LENGTH_LONG).show()
    }
}
