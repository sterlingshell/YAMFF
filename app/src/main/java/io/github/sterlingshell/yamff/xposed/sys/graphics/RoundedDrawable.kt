package io.github.sterlingshell.yamff.xposed.sys.graphics

import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.withSave

class RoundedDrawable : Drawable() {
    var drawable: Drawable? = null
        set(value) {
            field = value
            invalidateSelf()
        }
    var radius: Int = 0
        set(value) {
            field = value
            invalidateSelf()
        }
    var isClipEnabled: Boolean = true
        set(value) {
            field = value
            invalidateSelf()
        }

    private val path = Path()
    private val rectF = RectF()

    override fun draw(canvas: Canvas) {
        val d = drawable ?: return
        d.bounds = bounds
        if (isClipEnabled && radius > 0) {
            canvas.withSave {
                rectF.set(bounds)
                path.reset()
                path.addRoundRect(rectF, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
                canvas.clipPath(path)
                d.draw(canvas)
            }
        } else {
            d.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        drawable?.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        drawable?.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = drawable?.opacity ?: PixelFormat.TRANSLUCENT
}
