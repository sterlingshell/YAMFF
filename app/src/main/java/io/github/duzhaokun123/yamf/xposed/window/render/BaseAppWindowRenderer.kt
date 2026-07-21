package io.github.duzhaokun123.yamf.xposed.window.render

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import io.github.duzhaokun123.yamf.xposed.services.ConfigManager
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowAction
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowState
import io.github.duzhaokun123.yamf.xposed.utils.SystemServices
import io.github.duzhaokun123.yamf.xposed.utils.graphics.dpToPx

import android.view.GestureDetector
import io.github.duzhaokun123.yamf.common.model.SurfaceType
import io.github.duzhaokun123.yamf.common.model.WindowStyle
import io.github.duzhaokun123.yamf.xposed.window.model.ThemeColors
import io.github.duzhaokun123.yamf.common.extensions.getAttr
import io.github.duzhaokun123.yamf.common.extensions.runMain
import androidx.core.graphics.createBitmap

abstract class BaseAppWindowRenderer<VB : ViewBinding>(val context: Context) : AppWindowRenderer {

    protected abstract val binding: VB
    protected var internalActionHandler: ((AppWindowAction) -> Unit)? = null
    protected abstract val internalSurfaceView: View
    protected abstract val internalBubbleView: BubbleView
    protected abstract val internalBlurImageView: ImageView
    protected abstract val internalViewportContainer: android.view.ViewGroup
    
    protected var lastState: AppWindowState? = null

    protected var bufferWidth = 0
    protected var bufferHeight = 0

    override fun setActionHandler(handler: (AppWindowAction) -> Unit) {
        this.internalActionHandler = handler
    }

    override fun getRootView(): View = binding.root
    override fun getSurfaceView(): View = internalSurfaceView

    protected fun setupSurfaceViewCommon() {
        (internalSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
    }

    protected fun updateExclusionRects() {
        val root = binding.root
        if (!root.isAttachedToWindow) return
        if (ConfigManager.config.excludeSystemGesture) {
            root.post {
                if (root.isAttachedToWindow) {
                    val rects = listOf(Rect(0, 0, root.width, root.height))
                    root.systemGestureExclusionRects = rects
                }
            }
        } else {
            root.post {
                if (root.isAttachedToWindow) {
                    root.systemGestureExclusionRects = emptyList()
                }
            }
        }
    }

    protected fun updateCommonWindowLayout(state: AppWindowState, lp: WindowManager.LayoutParams) {
        val root = binding.root

        // Ensure root doesn't clip shadows
        if (root is android.view.ViewGroup) {
            root.clipChildren = false
            root.clipToPadding = false
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                if (child is android.view.ViewGroup) {
                    child.clipChildren = false
                    child.clipToPadding = false
                }
            }
        }

        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        // BUG Fix: ALWAYS use PixelFormat.TRANSLUCENT to ensure alpha and shadows work across all modes
        lp.format = android.graphics.PixelFormat.TRANSLUCENT

        if (state.isCollapsed) {
            internalBubbleView.isVisible = true
            // Dynamic Shadow Padding based on bubble size
            val p = (state.bubbleWidth.coerceAtLeast(56.dpToPx().toInt()) * 0.5f).toInt()
            // Set padding BEFORE calculating lp.x/lp.y so the position accounts for the new padding
            root.setPadding(p, p, p, p)

            internalBubbleView.updateUI(
                if (state.bubbleWidth > 0) state.bubbleWidth else 56.dpToPx().toInt(),
                if (state.bubbleRadius > 0f) state.bubbleRadius else (state.bubbleWidth / 2f).coerceAtLeast(28.dpToPx()),
                if (state.isAutoHide) 0.3f else 1.0f
            )
            internalSurfaceView.isVisible = false
        } else {
            // Restore zero padding for expanded mode to avoid viewport shift
            root.setPadding(0, 0, 0, 0)
            internalBubbleView.isVisible = false
            internalSurfaceView.isVisible = state.contentReady
        }

        // Add extra padding to the window coordinate to account for root view's internal padding (shadow space)
        // MUST be after setPadding() so root.paddingLeft/Top reflect the current state
        lp.x = state.windowRect.left - root.paddingLeft
        lp.y = state.windowRect.top - root.paddingTop
    }

    protected fun captureAndBlur() {
        val view = internalSurfaceView
        if (view.width <= 0 || view.height <= 0) return
        
        try {
            if (view is TextureView) {
                val bitmap = view.bitmap ?: return
                internalBlurImageView.setImageBitmap(bitmap)
            } else if (view is SurfaceView) {
                val bitmap = createBitmap(view.width, view.height)
                PixelCopy.request(view, bitmap, { result: Int ->
                    if (result == PixelCopy.SUCCESS) {
                        internalBlurImageView.setImageBitmap(bitmap)
                    }
                }, Handler(Looper.getMainLooper()))
            }
            

            // BUG Fix: Use RenderEffect for high-quality blur. 
            // Note: RenderEffect requires API 31+. Project minSdk is 31.
            internalBlurImageView.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP))

        } catch (e: Exception) {
            Log.e("BaseRenderer", "Capture and blur failed", e)
        }
    }

    protected fun applyViewportTransform(state: AppWindowState, containerW: Int, containerH: Int) {
        val content = internalSurfaceView
        val blur = internalBlurImageView

        if (state.isCollapsed) return

        val isSurfaceView = ConfigManager.config.surfaceView == SurfaceType.SURFACE

        if (state.isScaling) {
            if (!isSurfaceView) {
                // TextureView: Native 1:1 re-layout mechanism
                blur.isVisible = false
                content.scaleX = 1.0f
                content.scaleY = 1.0f
                content.pivotX = 0f
                content.pivotY = 0f
                content.translationX = 0f
                content.translationY = 0f
            } else {
                // SurfaceView: FitCenter + Blur background
                blur.isVisible = true
                if (containerW <= 0 || containerH <= 0 || bufferWidth <= 0 || bufferHeight <= 0) return

                val scaleW = containerW.toFloat() / bufferWidth
                val scaleH = containerH.toFloat() / bufferHeight
                val scale = Math.min(scaleW, scaleH)

                content.scaleX = scale
                content.scaleY = scale
                content.pivotX = 0f
                content.pivotY = 0f
                
                // Precise mathematical centering
                content.translationX = (containerW - bufferWidth * scale) / 2f
                content.translationY = (containerH - bufferHeight * scale) / 2f
            }
            return
        }

        if (state.isMini) {
            blur.isVisible = false
            // Visual Matrix Scale: App content is layouted at full size, but displayed at 0.5x.
            // This prevents triggering Configuration Change / Re-layout.
            content.scaleX = 0.5f
            content.scaleY = 0.5f
            content.pivotX = 0f
            content.pivotY = 0f
            content.translationX = 0f
            content.translationY = 0f
            return
        }

        // Normal state: Reset to 1.0 and clear offsets
        blur.isVisible = false
        content.scaleX = state.scaleFactor
        content.scaleY = state.scaleFactor
        content.pivotX = 0f
        content.pivotY = 0f
        content.translationX = 0f
        content.translationY = 0f
    }

    protected fun safeUpdateViewLayout() {
        val root = binding.root
        if (root.isAttachedToWindow) {
            val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
            runCatching {
                SystemServices.windowManager.updateViewLayout(root, lp)
            }.onFailure { e ->
                Log.e("BaseRenderer", "updateViewLayout failed", e)
            }
        }
    }

    protected fun animateCommonCollapsed(contentView: View) {
        contentView.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(300).setInterpolator(AnticipateInterpolator()).withEndAction { 
            contentView.isVisible = false
            internalSurfaceView.isVisible = false // Force GONE for SurfaceView
        }.start()
        internalBubbleView.isVisible = true
        internalBubbleView.alpha = 0f
        internalBubbleView.scaleX = 0f
        internalBubbleView.scaleY = 0f
        internalBubbleView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(OvershootInterpolator()).start()
    }

    protected fun animateCommonExpanded(contentView: View) {
        contentView.isVisible = true
        contentView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(OvershootInterpolator()).start()
        internalBubbleView.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(300).setInterpolator(AnticipateInterpolator()).withEndAction { internalBubbleView.isVisible = false }.start()
    }

    protected fun runCommonAppContentReady(root: View, lp: WindowManager.LayoutParams, viewsToFadeIn: List<View>) {
        if (lastState?.contentReady == true) return
        internalActionHandler?.invoke(AppWindowAction.OpeningAnimationEnd)
        
        if (!root.isAttachedToWindow) return
        val currentHeight = lp.height
        
        root.measure(View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
                     View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST))
        val targetHeight = root.measuredHeight
        
        if (targetHeight <= currentHeight) {
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            runCatching { SystemServices.windowManager.updateViewLayout(root, lp) }
            viewsToFadeIn.forEach { it.animate().alpha(1f).setDuration(500).start() }
            return
        }

        val animator = ValueAnimator.ofInt(currentHeight, targetHeight)
        animator.addUpdateListener {
            if (root.isAttachedToWindow) {
                lp.height = it.animatedValue as Int
                runCatching { SystemServices.windowManager.updateViewLayout(root, lp) }
            }
        }
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (root.isAttachedToWindow) {
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                    runCatching { SystemServices.windowManager.updateViewLayout(root, lp) }
                }
            }
        })
        animator.start()
        viewsToFadeIn.forEach { it.animate().alpha(1f).setDuration(500).start() }
    }

    protected inner class ResizeTouchHandler(
        private val sizePreviewer: View,
        private val tvSize: TextView? = null
    ) : View.OnTouchListener {
        private var initialRawX = 0f
        private var initialRawY = 0f
        private var initialWidth = 0
        private var initialHeight = 0
        private var initialRatio = 1f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val state = lastState ?: return false
            // Dynamic min size: 20% of screen width
            val minSize = (context.resources.displayMetrics.widthPixels * 0.2f).toInt()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialRawX = event.rawX
                    initialRawY = event.rawY
                    initialWidth = state.windowRect.width()
                    initialHeight = state.windowRect.height()
                    initialRatio = initialWidth.toFloat() / initialHeight
                    sizePreviewer.isVisible = true
                    
                    if (ConfigManager.config.surfaceView == SurfaceType.SURFACE) {
                        bufferWidth = initialWidth
                        bufferHeight = initialHeight
                        captureAndBlur()
                    }
                    
                    internalActionHandler?.invoke(AppWindowAction.SetScaling(true))
                }
                MotionEvent.ACTION_MOVE -> {
                    val totalDeltaX = (event.rawX - initialRawX).toInt()
                    val totalDeltaY = (event.rawY - initialRawY).toInt()

                    val targetWidth: Int
                    val targetHeight: Int

                    if (state.isAspectLocked) {
                        targetWidth = (initialWidth + totalDeltaX).coerceIn(minSize, 4000)
                        targetHeight = (targetWidth / initialRatio).toInt()
                    } else {
                        targetWidth = (initialWidth + totalDeltaX).coerceIn(minSize, 4000)
                        targetHeight = (initialHeight + totalDeltaY).coerceIn(minSize, 4000)
                    }

                    // For both modes, update window dimensions for real-time feedback
                    internalActionHandler?.invoke(AppWindowAction.UpdateDimensions(targetWidth, targetHeight))
                    
                    // Display size text
                    tvSize?.text = "${targetWidth} x ${targetHeight}"
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    sizePreviewer.isVisible = false
                    
                    internalActionHandler?.invoke(AppWindowAction.SetScaling(false))
                    internalActionHandler?.invoke(AppWindowAction.RequestMoveToTop)
                }
            }
            return true
        }
    }

    // --- Sub-renderer Common Logics ---

    protected open fun applyThemeColors(colors: ThemeColors) {
        // Implementation provided by subclasses or generic if possible
    }

    protected open fun updateMiniUI(isMini: Boolean) {
        if (isMini) {
            internalSurfaceView.setOnTouchListener(miniTouchListener)
        } else {
            internalSurfaceView.setOnTouchListener { _, event ->
                internalActionHandler?.invoke(AppWindowAction.SurfaceTouch(event))
                internalActionHandler?.invoke(AppWindowAction.RequestMoveToTop)
                true
            }
        }
    }

    protected abstract fun updateWindowLayout(state: AppWindowState)

    protected val miniTouchListener = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            lastMiniX = event.rawX
            lastMiniY = event.rawY
        }
        miniGestureDetector.onTouchEvent(event)
        true
    }

    private var lastMiniX = 0f
    private var lastMiniY = 0f
    private val miniGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            internalActionHandler?.invoke(AppWindowAction.Move(e2.rawX - lastMiniX, e2.rawY - lastMiniY))
            lastMiniX = e2.rawX
            lastMiniY = e2.rawY
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            internalActionHandler?.invoke(AppWindowAction.ToggleMini)
            return true
        }
    })

    protected val moveGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        var lastX = 0f
        var lastY = 0f
        
        override fun onDown(e: MotionEvent): Boolean {
            lastX = e.rawX
            lastY = e.rawY
            internalActionHandler?.invoke(AppWindowAction.RequestMoveToTop)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            internalActionHandler?.invoke(AppWindowAction.Move(e2.rawX - lastX, e2.rawY - lastY))
            lastX = e2.rawX
            lastY = e2.rawY
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            internalActionHandler?.invoke(AppWindowAction.Fling(velocityX, velocityY))
            return true
        }
    })
}
