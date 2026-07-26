package io.github.sterlingshell.yamff.xposed.window.logic

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import io.github.sterlingshell.yamff.xposed.window.model.AppWindowState
import io.github.sterlingshell.yamff.xposed.utils.graphics.dpToPx

class AutoHideManager(
    private val context: Context,
    private val handler: Handler,
    private val stateProvider: () -> AppWindowState,
    private val updateState: ((AppWindowState) -> AppWindowState) -> Unit
) {
    private val autoHideRunnable = Runnable { enterAutoHide() }
    private var xAnimation: SpringAnimation? = null

    fun startAutoHideTimer() {
        cancelAutoHideTimer()
        handler.postDelayed(autoHideRunnable, 3000)
    }

    fun cancelAutoHideTimer() {
        handler.removeCallbacks(autoHideRunnable)
    }

    fun cancelAnimations() {
        xAnimation?.cancel()
    }

    private fun enterAutoHide() {
        val state = stateProvider()
        if (state.isAutoHide || !state.isCollapsed) return
        val lp = state.windowRect
        val screenWidth = context.resources.displayMetrics.widthPixels
        
        // BUG Fix: Use dynamic bubbleWidth and percentage-based burying
        val bubbleWidth = if (state.bubbleWidth > 0) state.bubbleWidth else AppWindowLogic.BUBBLE_WIDTH_COLLAPSED_DP.dpToPx().toInt()
        val isLeft = lp.left < screenWidth / 2
        
        // Deeply buried: 85% hidden
        val hiddenWidth = (bubbleWidth * 0.85f).toInt()
        val targetX = if (isLeft) -hiddenWidth else (screenWidth - (bubbleWidth - hiddenWidth))

        xAnimation?.cancel()
        xAnimation = SpringAnimation(FloatValueHolder(state.windowRect.left.toFloat())).apply {
            spring = SpringForce(targetX.toFloat()).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                stiffness = SpringForce.STIFFNESS_VERY_LOW
            }
            addUpdateListener { _, value, _ ->
                val currentRect = stateProvider().windowRect
                val rect = Rect(currentRect)
                rect.offsetTo(value.toInt(), rect.top)
                updateState { it.copy(windowRect = rect) }
            }
            start()
        }
        updateState { it.copy(isAutoHide = true) }
    }

    fun exitAutoHide() {
        val state = stateProvider()
        if (!state.isAutoHide || !state.isCollapsed) return
        val lp = state.windowRect
        val screenWidth = context.resources.displayMetrics.widthPixels
        
        // BUG Fix: Use dynamic bubbleWidth. Match snapToEdge positioning (45% hidden).
        val bubbleWidth = if (state.bubbleWidth > 0) state.bubbleWidth else AppWindowLogic.BUBBLE_WIDTH_COLLAPSED_DP.dpToPx().toInt()
        val isLeft = lp.left < screenWidth / 2
        
        val hiddenWidth = (bubbleWidth * 0.45f).toInt()
        val targetX = if (isLeft) -hiddenWidth else (screenWidth - (bubbleWidth - hiddenWidth))

        xAnimation?.cancel()
        xAnimation = SpringAnimation(FloatValueHolder(state.windowRect.left.toFloat())).apply {
            spring = SpringForce(targetX.toFloat()).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            addUpdateListener { _, value, _ ->
                val currentRect = stateProvider().windowRect
                val rect = Rect(currentRect)
                rect.offsetTo(value.toInt(), rect.top)
                updateState { it.copy(windowRect = rect) }
            }
            start()
        }
        updateState { it.copy(isAutoHide = false) }
    }
}
