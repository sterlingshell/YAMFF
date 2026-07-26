package io.github.sterlingshell.yamff.xposed.utils

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.util.LruCache
import io.github.sterlingshell.yamff.xposed.utils.extensions.componentName
import io.github.sterlingshell.yamff.xposed.utils.graphics.YAMFFRoundedDrawable

object AppInfoCache {
    private val map = LruCache<ComponentName, Pair<Drawable, CharSequence>>(50)

    fun getIconLabel(info: ActivityInfo): Pair<Drawable, CharSequence> {
        return map.get(info.componentName) ?: run {
            val res = YAMFFRoundedDrawable().apply {
                isClipEnabled = true
                radius = 100
                drawable = info.loadIcon(SystemServices.packageManager)
            } to info.loadLabel(SystemServices.packageManager)
            map.put(info.componentName, res)
            res
        }
    }
}
