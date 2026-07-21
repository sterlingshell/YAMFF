package io.github.duzhaokun123.yamf.manager.utils

import android.widget.Toast
import androidx.annotation.StringRes
import io.github.duzhaokun123.yamf.common.extensions.runMain
import io.github.duzhaokun123.yamf.manager.application

fun showToast(msg: CharSequence?) {
    runMain {
        Toast.makeText(application, "$msg", Toast.LENGTH_LONG).show()
    }
}

fun showToast(@StringRes resId: Int) =
    showToast(application.getText(resId))
