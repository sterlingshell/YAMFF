package io.github.duzhaokun123.yamf.xposed.utils.graphics

import android.content.res.Resources
import android.util.TypedValue

fun Number.dpToPx() =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
    )
