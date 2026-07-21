package io.github.duzhaokun123.yamf.xposed.window.render

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import io.github.duzhaokun123.yamf.databinding.ViewBubbleBinding
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowAction
import kotlin.math.hypot

@SuppressLint("ClickableViewAccessibility")
class BubbleView(context: Context) : FrameLayout(context) {
    private val binding = ViewBubbleBinding.inflate(LayoutInflater.from(context), this, true)
    private var actionHandler: ((AppWindowAction) -> Unit)? = null
    
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private var startTime = 0L

    init {
        binding.cvBubbleRoot.setOnTouchListener { v: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isDragging = false
                    startTime = System.currentTimeMillis()
                    actionHandler?.invoke(AppWindowAction.WakeUpBubble)
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging && hypot(event.rawX - startX, event.rawY - startY) > touchSlop) {
                        isDragging = true
                        actionHandler?.invoke(AppWindowAction.StartBubbleDrag)
                        actionHandler?.invoke(AppWindowAction.RequestMoveToTop)
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    if (isDragging) {
                        actionHandler?.invoke(AppWindowAction.Move(event.rawX - startX, event.rawY - startY))
                        startX = event.rawX
                        startY = event.rawY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    if (isDragging) {
                        actionHandler?.invoke(AppWindowAction.EndBubbleDrag)
                        val velocityX = (event.rawX - startX) / (System.currentTimeMillis() - startTime).toFloat() * 1000
                        actionHandler?.invoke(AppWindowAction.FlickAwayBubble(velocityX))
                    } else if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                        actionHandler?.invoke(AppWindowAction.TapBubble)
                    }
                }
            }
            true
        }
    }

    fun setIcon(icon: Drawable?) {
        binding.ivBubbleIcon.setImageDrawable(icon)
    }

    fun setActionHandler(handler: (AppWindowAction) -> Unit) {
        this.actionHandler = handler
    }

    fun updateUI(width: Int, radius: Float, alpha: Float) {
        binding.cvBubbleRoot.updateLayoutParams {
            this.width = width
            this.height = width
        }
        binding.cvBubbleRoot.radius = radius
        binding.cvBubbleRoot.alpha = alpha
    }
}
