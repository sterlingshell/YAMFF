package io.github.sterlingshell.yamff.xposed.sys.graphics

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import androidx.core.graphics.createBitmap
import io.github.sterlingshell.yamff.common.model.SnapshotBackground

fun Number.dpToPx() =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
    )

fun Bitmap.processForRecentTask(
    targetWidth: Int,
    targetHeight: Int,
    mode: SnapshotBackground,
    addDecoration: Boolean = false
): Bitmap {
    val result = createBitmap(targetWidth, targetHeight)
    val canvas = Canvas(result)
    
    // Draw background
    when (mode) {
        SnapshotBackground.BLUR -> {
             // Simple "blur" by stretching and darkening
             val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { 
                 alpha = 180
             }
             canvas.drawBitmap(this, null, Rect(0, 0, targetWidth, targetHeight), bgPaint)
             canvas.drawColor(0x66000000) 
        }
        SnapshotBackground.TRANSPARENT -> {
            // Default is transparent
        }
        SnapshotBackground.SOLID_COLOR -> {
            canvas.drawColor(0xCC222222u.toInt()) 
        }
    }
    
    // Draw original bitmap centered and scaled down to 85% to ensure it fits with padding
    val scale = Math.min(targetWidth.toFloat() / this.width, targetHeight.toFloat() / this.height) * 0.85f
    val w = (this.width * scale).toInt()
    val h = (this.height * scale).toInt()
    val left = (targetWidth - w) / 2
    val top = (targetHeight - h) / 2
    
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        isAntiAlias = true
    }
    canvas.drawBitmap(this, null, Rect(left, top, left + w, top + h), paint)
    
    if (addDecoration) {
        val borderPaint = Paint().apply {
            color = 0xFFFFFFFFu.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2.dpToPx()
            isAntiAlias = true
        }
        canvas.drawRect(left.toFloat(), top.toFloat(), (left + w).toFloat(), (top + h).toFloat(), borderPaint)
        
        // Simple "title bar" indicator
        val barPaint = Paint().apply {
            color = 0xAAFFFFFFu.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(left.toFloat(), top.toFloat(), (left + w).toFloat(), top + 8.dpToPx(), barPaint)
    }
    
    return result
}
