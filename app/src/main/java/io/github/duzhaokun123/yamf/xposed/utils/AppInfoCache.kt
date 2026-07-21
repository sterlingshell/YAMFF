package io.github.duzhaokun123.yamf.xposed.utils

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import io.github.duzhaokun123.yamf.xposed.utils.extensions.componentName
import io.github.duzhaokun123.yamf.xposed.utils.graphics.YAMFRoundedDrawable

object AppInfoCache {
    private val map = mutableMapOf<ComponentName, Pair<Drawable, CharSequence>>()

    fun getIconLabel(info: ActivityInfo): Pair<Drawable, CharSequence> {
        return map.getOrPut(info.componentName) {
            YAMFRoundedDrawable().apply {
                isClipEnabled = true
                radius = 100
                drawable = info.loadIcon(SystemServices.packageManager)
            } to info.loadLabel(SystemServices.packageManager)
        }
    }
}
