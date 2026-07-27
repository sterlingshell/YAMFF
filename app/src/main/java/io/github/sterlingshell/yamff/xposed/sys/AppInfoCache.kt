package io.github.sterlingshell.yamff.xposed.sys

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.util.LruCache
import io.github.sterlingshell.yamff.xposed.util.ext.componentName
import io.github.sterlingshell.yamff.xposed.sys.graphics.RoundedDrawable

object AppInfoCache {
    private val map = LruCache<ComponentName, Pair<Drawable, CharSequence>>(50)

    fun getIconLabel(info: ActivityInfo): Pair<Drawable, CharSequence> {
        return map.get(info.componentName) ?: run {
            val res = RoundedDrawable().apply {
                isClipEnabled = true
                radius = 100
                drawable = info.loadIcon(SystemServices.packageManager)
            } to info.loadLabel(SystemServices.packageManager)
            map.put(info.componentName, res)
            res
        }
    }
}
