package io.github.duzhaokun123.yamf.xposed.window.logic

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowAction
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowState
import io.github.duzhaokun123.yamf.xposed.utils.graphics.dpToPx

class WindowGestureHandler(
    private val context: Context,
    private val stateProvider: () -> AppWindowState,
    private val updateState: ((AppWindowState) -> AppWindowState) -> Unit,
    private val onAction: (AppWindowAction) -> Unit
) {
    private var xAnimation: SpringAnimation? = null
    private var yAnimation: SpringAnimation? = null

    fun handleMove(dx: Float, dy: Float) {
        val state = stateProvider()
        val rect = Rect(state.windowRect)
        rect.offset(dx.toInt(), dy.toInt())
        clampRect(rect, state)
        updateState { it.copy(windowRect = rect) }
        if (!state.isCollapsed) {
            updateGhostIcon(rect)
        } else {
            if (state.ghostIconVisible) updateState { it.copy(ghostIconVisible = false) }
        }
    }

    private fun clampRect(rect: Rect, state: AppWindowState) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        
        // BUG Fix: Use bubbleWidth for constraints when collapsed.
        // Formula: visualWidth is what the user SEES on screen.
        val visualWidth: Int
        val minVisible: Int
        
        if (state.isCollapsed) {
            visualWidth = if (state.bubbleWidth > 0) state.bubbleWidth else 56.dpToPx().toInt()
            // For bubble, ensure at least 48dp is always reachable
            minVisible = 48.dpToPx().toInt()
        } else {
            val scale = state.scaleFactor * (if (state.isMini) AppWindowLogic.MINI_SCALE else 1f)
            visualWidth = (rect.width() * scale).toInt()
            minVisible = (visualWidth * 0.25f).toInt().coerceIn(48.dpToPx().toInt(), 200)
        }
        
        val left = rect.left.coerceIn(minVisible - visualWidth, screenWidth - minVisible)
        val top = rect.top.coerceIn(0, screenHeight - minVisible)
        
        rect.offsetTo(left, top)
    }

    fun handleFling(velocityX: Float, velocityY: Float) {
        val state = stateProvider()
        if (state.isCollapsed) return
        xAnimation?.cancel()
        yAnimation?.cancel()

        runCatching {
            val windowWidth = (state.windowRect.width() * state.scaleFactor).toInt()
            val windowHeight = (state.windowRect.height() * state.scaleFactor).toInt()
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels

            val clampedVx = velocityX.coerceIn(-20000f, 20000f)
            val clampedVy = velocityY.coerceIn(-20000f, 20000f)

            val flingX = FlingAnimation(FloatValueHolder(state.windowRect.left.toFloat())).apply {
                setStartVelocity(clampedVx)
                setMinValue(-windowWidth.toFloat())
                setMaxValue(screenWidth.toFloat())
                addUpdateListener { _, value, _ ->
                    val currentRect = stateProvider().windowRect
                    val rect = Rect(currentRect)
                    rect.offsetTo(value.toInt(), rect.top)
                    updateState { it.copy(windowRect = rect) }
                }
                addEndListener { _, _, value, _ ->
                    val currentState = stateProvider()
                    if (value <= -windowWidth || value >= screenWidth) {
                        onAction(AppWindowAction.Minimize)
                    } else {
                        updateGhostIcon(currentState.windowRect)
                        if (currentState.ghostIconVisible) onAction(AppWindowAction.Minimize)
                    }
                }
            }

            val flingY = FlingAnimation(FloatValueHolder(state.windowRect.top.toFloat())).apply {
                setStartVelocity(clampedVy)
                setMinValue(0f)
                setMaxValue((screenHeight - windowHeight).toFloat().coerceAtLeast(0f))
                addUpdateListener { _, value, _ ->
                    val currentRect = stateProvider().windowRect
                    val rect = Rect(currentRect)
                    rect.offsetTo(rect.left, value.toInt())
                    updateState { it.copy(windowRect = rect) }
                }
            }

            flingX.start()
            flingY.start()
        }.onFailure { e ->
            Log.e("WindowGestureHandler", "handleFling failed", e)
        }
    }

    fun animateTo(targetX: Int, targetY: Int, isDestroyedProvider: () -> Boolean) {
        if (isDestroyedProvider()) return
        xAnimation?.cancel()
        yAnimation?.cancel()

        val state = stateProvider()
        val rect = Rect(state.windowRect).apply { offsetTo(targetX, targetY) }
        clampRect(rect, state)
        val finalX = rect.left
        val finalY = rect.top

        xAnimation = SpringAnimation(FloatValueHolder(stateProvider().windowRect.left.toFloat())).apply {
            spring = SpringForce(finalX.toFloat()).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                if (isDestroyedProvider()) {
                    cancel()
                    return@addUpdateListener
                }
                val rect = Rect(stateProvider().windowRect)
                rect.offsetTo(value.toInt(), rect.top)
                updateState { it.copy(windowRect = rect) }
            }
            start()
        }

        yAnimation = SpringAnimation(FloatValueHolder(stateProvider().windowRect.top.toFloat())).apply {
            spring = SpringForce(finalY.toFloat()).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                if (isDestroyedProvider()) {
                    cancel()
                    return@addUpdateListener
                }
                val rect = Rect(stateProvider().windowRect)
                rect.offsetTo(rect.left, value.toInt())
                updateState { it.copy(windowRect = rect) }
            }
            start()
        }
    }

    fun snapToEdge(isDestroyedProvider: () -> Boolean) {
        val state = stateProvider()
        val lp = state.windowRect
        val screenWidth = context.resources.displayMetrics.widthPixels
        val bubbleWidth = if (state.bubbleWidth > 0) state.bubbleWidth else AppWindowLogic.BUBBLE_WIDTH_COLLAPSED_DP.dpToPx().toInt()
        val isLeft = lp.left < screenWidth / 2
        
        // BUG Fix: Ensure bubble is deeply buried on the right edge.
        // Formula: hide 45% of width
        val hiddenWidth = (bubbleWidth * 0.45f).toInt()
        val targetX = if (isLeft) -hiddenWidth else (screenWidth - (bubbleWidth - hiddenWidth))

        val screenHeight = context.resources.displayMetrics.heightPixels
        val minY = (screenHeight * 0.3).toInt()
        val maxY = (screenHeight * 0.7).toInt()
        val targetY = lp.top.coerceIn(minY, maxY)

        animateTo(targetX, targetY, isDestroyedProvider)
    }

    fun updateGhostIcon(rect: Rect) {
        val state = stateProvider()
        if (state.isCollapsed) {
            if (state.ghostIconVisible) updateState { it.copy(ghostIconVisible = false) }
            return
        }
        val screenWidth = context.resources.displayMetrics.widthPixels
        val windowWidth = (rect.width() * state.scaleFactor).toInt()
        // Threshold: 70% of window width
        val threshold = (windowWidth * 0.7f).toInt()

        val outLeft = rect.left < -threshold
        val outRight = rect.left + windowWidth > screenWidth + threshold

        if (outLeft || outRight) {
            val iconSize = 48.dpToPx()
            // BUG Fix: Ghost icon is in a non-scaled root container. Remove scaleFactor compensation.
            val posX = if (outLeft) ((-rect.left - iconSize / 2)) else ((screenWidth - rect.left - iconSize / 2))
            val posY = (rect.height() / 2 - iconSize / 2)
            updateState { it.copy(ghostIconVisible = true, ghostIconPos = posX to posY) }
        } else {
            updateState { it.copy(ghostIconVisible = false) }
        }
    }

    fun cancelAnimations() {
        xAnimation?.cancel()
        yAnimation?.cancel()
    }

    fun animateResize(targetWidth: Int, targetHeight: Int, isDestroyedProvider: () -> Boolean) {
        val currentWidth = stateProvider().windowRect.width()
        val currentHeight = stateProvider().windowRect.height()

        SpringAnimation(FloatValueHolder(currentWidth.toFloat())).apply {
            spring = SpringForce(targetWidth.toFloat()).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                if (isDestroyedProvider()) {
                    cancel()
                    return@addUpdateListener
                }
                val rect = Rect(stateProvider().windowRect)
                rect.right = rect.left + value.toInt()
                updateState { it.copy(windowRect = rect) }
            }
            start()
        }

        SpringAnimation(FloatValueHolder(currentHeight.toFloat())).apply {
            spring = SpringForce(targetHeight.toFloat()).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                if (isDestroyedProvider()) {
                    cancel()
                    return@addUpdateListener
                }
                val rect = Rect(stateProvider().windowRect)
                rect.bottom = rect.top + value.toInt()
                updateState { it.copy(windowRect = rect) }
            }
            start()
        }
    }
}
