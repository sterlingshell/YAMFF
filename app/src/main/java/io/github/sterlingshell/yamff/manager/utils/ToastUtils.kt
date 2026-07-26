package io.github.sterlingshell.yamff.manager.utils

import android.widget.Toast
import androidx.annotation.StringRes
import io.github.sterlingshell.yamff.common.extensions.runMain
import io.github.sterlingshell.yamff.manager.AppHolder

object ToastUtils {
    fun show(msg: CharSequence?) {
        runMain {
            Toast.makeText(AppHolder.app, "$msg", Toast.LENGTH_LONG).show()
        }
    }

    fun show(@StringRes resId: Int) =
        show(AppHolder.app.getText(resId))
}
