package io.github.sterlingshell.yamff.xposed.window

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
import io.github.sterlingshell.yamff.databinding.ViewBubbleBinding
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.window.model.Action
import kotlin.math.hypot

@SuppressLint("ClickableViewAccessibility")
class Bubble(context: Context) : FrameLayout(context) {
    private val binding = ViewBubbleBinding.inflate(LayoutInflater.from(context), this, true)
    private var actionHandler: ((Action) -> Unit)? = null

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
                    actionHandler?.invoke(Action.WakeUpBubble)
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging && hypot(event.rawX - startX, event.rawY - startY) > touchSlop) {
                        isDragging = true
                        actionHandler?.invoke(Action.StartBubbleDrag)
                        actionHandler?.invoke(Action.RequestMoveToTop)
                        if (ConfigManager.instance.config.hapticFeedback) {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    }
                    if (isDragging) {
                        actionHandler?.invoke(Action.Move(event.rawX - startX, event.rawY - startY))
                        startX = event.rawX
                        startY = event.rawY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    if (isDragging) {
                        actionHandler?.invoke(Action.EndBubbleDrag)
                        val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                        val velocityX = (event.rawX - startX) / duration.toFloat() * 1000
                        actionHandler?.invoke(Action.FlickAwayBubble(velocityX))
                    } else if (System.currentTimeMillis() - startTime < ViewConfiguration.getTapTimeout()) {
                        actionHandler?.invoke(Action.TapBubble)
                    }
                }
            }
            true
        }
    }

    fun setIcon(icon: Drawable?) {
        binding.ivBubbleIcon.setImageDrawable(icon)
    }

    fun setActionHandler(handler: (Action) -> Unit) {
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