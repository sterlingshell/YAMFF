package io.github.sterlingshell.yamff.xposed.window.render

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
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
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.window.model.Action
import io.github.sterlingshell.yamff.xposed.window.model.State
import io.github.sterlingshell.yamff.xposed.sys.SystemServices
import io.github.sterlingshell.yamff.xposed.sys.graphics.dpToPx

import android.view.GestureDetector
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.xposed.window.model.ThemeColors
import androidx.core.graphics.createBitmap
import io.github.sterlingshell.yamff.xposed.window.Bubble

abstract class BaseRenderer<VB : ViewBinding>(val context: Context) : Renderer {

    protected abstract val binding: VB
    protected var internalActionHandler: ((Action) -> Unit)? = null
    protected abstract val internalSurfaceView: View
    protected abstract val internalBubble: Bubble
    protected abstract val internalBlurImageView: ImageView
    protected abstract val internalGhostImageView: ImageView
    protected abstract val internalViewportContainer: android.view.ViewGroup
    
    protected var lastState: State? = null

    protected var bufferWidth = 0
    protected var bufferHeight = 0

    override fun setActionHandler(handler: (Action) -> Unit) {
        this.internalActionHandler = handler
    }

    override fun getRootView(): View = binding.root
    override fun getSurfaceView(): View = internalSurfaceView

    protected fun setupOverlayViews(root: android.view.ViewGroup) {
        (internalBubble.parent as? android.view.ViewGroup)?.removeView(internalBubble)
        (internalGhostImageView.parent as? android.view.ViewGroup)?.removeView(internalGhostImageView)

        root.addView(internalGhostImageView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        root.addView(internalBubble, 56.dpToPx().toInt(), 56.dpToPx().toInt())
        
        internalGhostImageView.isVisible = false
        internalBubble.isVisible = false
    }

    protected fun setupSurfaceViewCommon() {
        (internalSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
    }

    protected fun updateExclusionRects() {
        val root = binding.root
        if (!root.isAttachedToWindow) return
        if (ConfigManager.instance.config.excludeSystemGesture) {
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

    protected fun updateCommonWindowLayout(state: State, lp: WindowManager.LayoutParams) {
        val root = binding.root

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
        lp.format = android.graphics.PixelFormat.TRANSLUCENT

        if (state.isCollapsed) {
            internalBubble.isVisible = true
            internalGhostImageView.isVisible = false
            val p = (state.bubbleWidth.coerceAtLeast(56.dpToPx().toInt()) * 0.5f).toInt()
            root.setPadding(p, p, p, p)

            internalBubble.updateUI(
                if (state.bubbleWidth > 0) state.bubbleWidth else 56.dpToPx().toInt(),
                if (state.bubbleRadius > 0f) state.bubbleRadius else (state.bubbleWidth / 2f).coerceAtLeast(28.dpToPx()),
                if (state.isAutoHide) 0.3f else 1.0f
            )
            internalSurfaceView.isVisible = false
        } else {
            root.setPadding(0, 0, 0, 0)
            internalBubble.isVisible = false
            internalGhostImageView.isVisible = state.ghostIconVisible
            if (state.ghostIconVisible) {
                internalGhostImageView.x = state.ghostIconPos.first
                internalGhostImageView.y = state.ghostIconPos.second
            }
            internalSurfaceView.isVisible = state.contentReady
        }

        lp.x = state.windowRect.left - root.paddingLeft
        lp.y = state.windowRect.top - root.paddingTop
    }

    override fun decorateSnapshot(appContent: android.graphics.Bitmap): android.graphics.Bitmap {
        val root = binding.root
        if (root.width <= 0 || root.height <= 0 || !root.isAttachedToWindow) return appContent

        val surfaceView = internalSurfaceView
        val location = IntArray(2)
        surfaceView.getLocationInWindow(location)
        val rootLocation = IntArray(2)
        root.getLocationInWindow(rootLocation)
        
        val relativeX = location[0] - rootLocation[0]
        val relativeY = location[1] - rootLocation[1]
        val surfaceW = surfaceView.width
        val surfaceH = surfaceView.height

        val result = createBitmap(root.width, root.height)
        val canvas = android.graphics.Canvas(result)

        // 1. Capture the "Frame" (Decorations)
        val originalVisibility = surfaceView.visibility
        surfaceView.visibility = View.INVISIBLE
        root.draw(canvas)
        surfaceView.visibility = originalVisibility

        // 2. Synthesize the "Content" (App Screenshot)
        if (surfaceW > 0 && surfaceH > 0) {
            val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
            val destRect = Rect(relativeX, relativeY, relativeX + surfaceW, relativeY + surfaceH)
            canvas.drawBitmap(appContent, null, destRect, paint)
        }

        return result
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
            
            internalBlurImageView.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP))
        } catch (e: Exception) {
            Log.e("BaseRenderer", "Capture and blur failed", e)
        }
    }

    protected fun applyViewportTransform(state: State, containerW: Int, containerH: Int) {
        val content = internalSurfaceView
        val blur = internalBlurImageView

        if (state.isCollapsed) return

        val isSurfaceView = ConfigManager.instance.config.surfaceView == SurfaceType.SURFACE

        if (state.isScaling) {
            if (!isSurfaceView) {
                blur.isVisible = false
                content.scaleX = 1.0f
                content.scaleY = 1.0f
                content.pivotX = 0f
                content.pivotY = 0f
                content.translationX = 0f
                content.translationY = 0f
            } else {
                blur.isVisible = true
                if (containerW <= 0 || containerH <= 0 || bufferWidth <= 0 || bufferHeight <= 0) return

                val scaleW = containerW.toFloat() / bufferWidth
                val scaleH = containerH.toFloat() / bufferHeight
                val scale = Math.min(scaleW, scaleH)

                content.scaleX = scale
                content.scaleY = scale
                content.pivotX = 0f
                content.pivotY = 0f
                content.translationX = (containerW - bufferWidth * scale) / 2f
                content.translationY = (containerH - bufferHeight * scale) / 2f
            }
            return
        }

        if (state.isMini) {
            blur.isVisible = false
            content.scaleX = 0.5f
            content.scaleY = 0.5f
            content.pivotX = 0f
            content.pivotY = 0f
            content.translationX = 0f
            content.translationY = 0f
            return
        }

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
            internalSurfaceView.isVisible = false 
        }.start()
        internalBubble.isVisible = true
        internalBubble.alpha = 0f
        internalBubble.scaleX = 0f
        internalBubble.scaleY = 0f
        internalBubble.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(OvershootInterpolator()).start()
    }

    protected fun animateCommonExpanded(contentView: View) {
        contentView.isVisible = true
        contentView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(OvershootInterpolator()).start()
        internalBubble.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(300).setInterpolator(AnticipateInterpolator()).withEndAction { internalBubble.isVisible = false }.start()
    }

    protected fun runCommonAppContentReady(root: View, lp: WindowManager.LayoutParams, viewsToFadeIn: List<View>) {
        if (lastState?.contentReady == true) return
        internalActionHandler?.invoke(Action.OpeningAnimationEnd)
        
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

    @SuppressLint("ClickableViewAccessibility")
    protected inner class ResizeTouchHandler(
        private val sizePreviewer: View,
        private val tvSize: TextView? = null
    ) : View.OnTouchListener {
        private var initialRawX = 0f
        private var initialRawY = 0f
        private var initialWidth = 0
        private var initialHeight = 0
        private var initialRatio = 1f

        @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val state = lastState ?: return false
            val minSize = (context.resources.displayMetrics.widthPixels * 0.2f).toInt()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialRawX = event.rawX
                    initialRawY = event.rawY
                    initialWidth = state.windowRect.width()
                    initialHeight = state.windowRect.height()
                    initialRatio = initialWidth.toFloat() / initialHeight
                    sizePreviewer.isVisible = true
                    
                    if (ConfigManager.instance.config.surfaceView == SurfaceType.SURFACE) {
                        bufferWidth = initialWidth
                        bufferHeight = initialHeight
                        captureAndBlur()
                    }
                    
                    internalActionHandler?.invoke(Action.SetScaling(true))
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

                    internalActionHandler?.invoke(Action.UpdateDimensions(targetWidth, targetHeight))
                    tvSize?.text = "${targetWidth} x ${targetHeight}"
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    sizePreviewer.isVisible = false
                    internalActionHandler?.invoke(Action.SetScaling(false))
                    internalActionHandler?.invoke(Action.RequestMoveToTop)
                }
            }
            return true
        }
    }

    protected open fun applyThemeColors(colors: ThemeColors) {}

    protected open fun updateMiniUI(isMini: Boolean) {
        if (isMini) {
            internalSurfaceView.setOnTouchListener(miniTouchListener)
        } else {
            internalSurfaceView.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_UP) view.performClick()
                internalActionHandler?.invoke(Action.SurfaceTouch(event))
                internalActionHandler?.invoke(Action.RequestMoveToTop)
                true
            }
        }
    }

    protected abstract fun updateWindowLayout(state: State)

    protected val miniTouchListener = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            lastMiniX = event.rawX
            lastMiniY = event.rawY
        }
        if (event.action == MotionEvent.ACTION_UP) v.performClick()
        miniGestureDetector.onTouchEvent(event)
        true
    }

    private var lastMiniX = 0f
    private var lastMiniY = 0f
    private val miniGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            internalActionHandler?.invoke(Action.Move(e2.rawX - lastMiniX, e2.rawY - lastMiniY))
            lastMiniX = e2.rawX
            lastMiniY = e2.rawY
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            internalActionHandler?.invoke(Action.ToggleMini)
            return true
        }
    })

    protected val moveGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        var lastX = 0f
        var lastY = 0f
        
        override fun onDown(e: MotionEvent): Boolean {
            lastX = e.rawX
            lastY = e.rawY
            internalActionHandler?.invoke(Action.RequestMoveToTop)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            internalActionHandler?.invoke(Action.Move(e2.rawX - lastX, e2.rawY - lastY))
            lastX = e2.rawX
            lastY = e2.rawY
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            internalActionHandler?.invoke(Action.Fling(velocityX, velocityY))
            return true
        }
    })
}
