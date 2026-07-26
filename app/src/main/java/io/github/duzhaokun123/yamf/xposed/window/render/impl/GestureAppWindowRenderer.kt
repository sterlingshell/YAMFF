package io.github.duzhaokun123.yamf.xposed.window.render.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.common.model.SurfaceType
import io.github.duzhaokun123.yamf.databinding.WindowAppGestureBinding
import io.github.duzhaokun123.yamf.xposed.services.ConfigManager
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowAction
import io.github.duzhaokun123.yamf.xposed.window.model.AppWindowState
import io.github.duzhaokun123.yamf.xposed.window.model.ThemeColors
import io.github.duzhaokun123.yamf.xposed.window.render.BaseAppWindowRenderer
import io.github.duzhaokun123.yamf.xposed.window.render.BubbleView
import io.github.duzhaokun123.yamf.xposed.utils.graphics.dpToPx
import io.github.duzhaokun123.yamf.common.extensions.getAttr

@SuppressLint("ClickableViewAccessibility", "SetTextI18n")
class GestureAppWindowRenderer(context: Context) : BaseAppWindowRenderer<WindowAppGestureBinding>(context) {
    override val binding = WindowAppGestureBinding.inflate(LayoutInflater.from(context))
    override lateinit var internalSurfaceView: View
    override val internalBubbleView = BubbleView(context).apply { isVisible = false }
    override lateinit var internalBlurImageView: android.widget.ImageView
    override lateinit var internalGhostImageView: android.widget.ImageView
    override lateinit var internalViewportContainer: android.view.ViewGroup
    
    private val viewportOutlineProvider = object : android.view.ViewOutlineProvider() {
        override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, 12.dpToPx())
        }
    }

    init {
        setupSurfaceView()
        setupListeners()
        setupSurfaceViewCommon()
    }

    private fun setupSurfaceView() {
        val viewSurface = binding.viewSurface
        val viewTexture = binding.viewTexture
        
        val surfaceType = runCatching { ConfigManager.config.surfaceView }.getOrNull() ?: SurfaceType.TEXTURE
        if (surfaceType == SurfaceType.SURFACE) {
            internalSurfaceView = viewSurface
            viewSurface.isVisible = true
            viewTexture.isVisible = false
        } else {
            internalSurfaceView = viewTexture
            viewSurface.isVisible = false
            viewTexture.isVisible = true
        }
        internalSurfaceView.id = R.id.surface
        
        internalViewportContainer = binding.viewport
        
        internalBlurImageView = android.widget.ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            isVisible = false
        }
        binding.viewport.addView(internalBlurImageView, 0) // Behind surface
        
        internalGhostImageView = binding.ivGhost

        setupOverlayViews(binding.root)
    }

    private fun setupListeners() {
        internalBubbleView.setActionHandler { internalActionHandler?.invoke(it) }

        binding.root.setOnTouchListener { _, event ->
            val state = lastState ?: return@setOnTouchListener false
            
            // Standardized Touch Strategy
            if (state.isCollapsed) return@setOnTouchListener false
            
            internalActionHandler?.invoke(AppWindowAction.RequestMoveToTop)
            false
        }

        val moveTouchListener = View.OnTouchListener { _, event ->
            moveGestureDetector.onTouchEvent(event)
            true
        }
        binding.cvBarClickMask.setOnTouchListener(moveTouchListener)
        binding.cvBarSideClickMask.setOnTouchListener(moveTouchListener)

        binding.ibSuper.setOnClickListener { toggleSuperMenu(!binding.clSuperLayout.isVisible) }
        binding.ibSuperClose.setOnClickListener { toggleSuperMenu(false) }
        binding.ibClose.setOnClickListener { internalActionHandler?.invoke(AppWindowAction.Close) }
        binding.ibClose.setOnLongClickListener { internalActionHandler?.invoke(AppWindowAction.OpenAppList); true }
        binding.ibFullscreen.setOnClickListener { internalActionHandler?.invoke(AppWindowAction.Fullscreen) }
        binding.ibFullscreen.setOnLongClickListener { internalActionHandler?.invoke(AppWindowAction.ToggleMini); true }
        binding.ibMinimize.setOnClickListener { internalActionHandler?.invoke(AppWindowAction.ToggleMini) }
        binding.ibCollapse.setOnClickListener { internalActionHandler?.invoke(AppWindowAction.Minimize) }

        binding.ibRightResize.setOnTouchListener(ResizeTouchHandler(binding.vSizePreviewer))

        internalSurfaceView.setOnTouchListener { _, event ->
            internalActionHandler?.invoke(AppWindowAction.SurfaceTouch(event))
            internalActionHandler?.invoke(AppWindowAction.RequestMoveToTop)
            true
        }
    }

    private fun toggleSuperMenu(show: Boolean) {
        if (show) {
            binding.clSuperLayout.isVisible = true
            binding.clSuperLayout.alpha = 0f
            binding.clSuperLayout.scaleX = 0.9f
            binding.clSuperLayout.scaleY = 0.9f
            binding.clSuperLayout.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200).setInterpolator(OvershootInterpolator()).start()
            binding.rootClickMask.isVisible = true
            binding.rootClickMask.setOnClickListener { toggleSuperMenu(false) }
        } else {
            binding.clSuperLayout.animate()
                .alpha(0f).scaleX(0.9f).scaleY(0.9f)
                .setDuration(200).setInterpolator(AnticipateInterpolator())
                .withEndAction { 
                    binding.clSuperLayout.isVisible = false
                    binding.rootClickMask.isVisible = false
                }.start()
        }
    }

    override fun onStateChanged(state: AppWindowState) {
        val oldState = lastState
        lastState = state
        
        // BUG Fix: Apply rounded corners to the viewport to clip SurfaceView
        binding.viewport.outlineProvider = viewportOutlineProvider
        binding.viewport.clipToOutline = true
        
        // Use theme surface color for background to prevent black flashes during transitions
        val surfaceColor = context.theme.getAttr(com.google.android.material.R.attr.colorSurface).data
        binding.viewport.setBackgroundColor(surfaceColor)
        
        binding.cvBackground.clipToOutline = true
        binding.cvBackground.clipChildren = true
        binding.cvParent.clipToOutline = true
        
        binding.ibSuper.bringToFront()
        
        if (state.isCollapsed != oldState?.isCollapsed) {
            if (state.isCollapsed) {
                toggleSuperMenu(false) // BUG-7, 10 Fix
                animateCommonCollapsed(binding.cvParent)
            } else {
                animateCommonExpanded(binding.cvParent)
                updateMiniUI(state.isMini) // Reset controller visibility - BUG-6, 8 Fix
            }
        }
        
        if (state.appIcon != oldState?.appIcon) {
            internalBubbleView.setIcon(state.appIcon)
            binding.ivGhost.setImageDrawable(state.appIcon)
            binding.ivGhost.updateLayoutParams {
                width = 48.dpToPx().toInt()
                height = 48.dpToPx().toInt()
            }
        }
        if (state.themeColors != oldState?.themeColors) applyThemeColors(state.themeColors)
        if (state.isMini != oldState?.isMini) updateMiniUI(state.isMini)
        
        updateWindowLayout(state)
    }

    override fun applyThemeColors(colors: ThemeColors) {
        if (colors.appCardBackground != 0) {
            binding.cvBackground.setCardBackgroundColor(colors.appCardBackground)
            binding.background.setBackgroundColor(colors.appCardBackground)
            binding.clSuperLayout.setBackgroundColor(colors.appCardBackground)
            val tint = ColorStateList.valueOf(colors.onStatusBarColor)
            binding.ibSuperClose.imageTintList = tint
            binding.ibClose.imageTintList = tint
            binding.ibFullscreen.imageTintList = tint
            binding.ibMinimize.imageTintList = tint
            binding.ibCollapse.imageTintList = tint
        }
    }

    override fun updateMiniUI(isMini: Boolean) {
        super.updateMiniUI(isMini)
        if (isMini) {
            toggleSuperMenu(false)
            binding.ibSuper.isVisible = false
            binding.rlBarControllerBottom.isVisible = false
            binding.rlBarControllerSide.isVisible = false
            binding.ibRightResize.isVisible = false
        } else {
            binding.ibSuper.isVisible = true
            binding.rlBarControllerBottom.isVisible = true
            binding.rlBarControllerSide.isVisible = true
            binding.ibRightResize.isVisible = true
        }
    }

    override fun updateWindowLayout(state: AppWindowState) {
        val root = binding.root
        val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
        
        updateCommonWindowLayout(state, lp)
        
        if (state.isCollapsed) {
            binding.cvParent.visibility = View.GONE
            binding.cvBackground.visibility = View.GONE
            binding.rlBarControllerBottom.visibility = View.GONE
            binding.rlBarControllerSide.visibility = View.GONE
            binding.ibRightResize.visibility = View.GONE
        } else {
            binding.cvParent.isVisible = true
            binding.cvBackground.isVisible = true
            
            val containerScale = if (state.isMini) 0.5f else 1.0f
            val scaledWidth = (state.windowRect.width() * containerScale).toInt()
            val scaledHeight = (state.windowRect.height() * containerScale).toInt()

            // Update viewport container width and height.
            // XML constraints will ensure ibSuper stays below this container.
            internalViewportContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                this.width = scaledWidth
                this.height = if (state.contentReady) scaledHeight else 0
            }

            // Conditional sizing: 
            // 1. SurfaceView locks buffer size during scaling.
            // 2. Mini mode layouts content at FULL size but container is small (scaled visually).
            val surfaceType = runCatching { ConfigManager.config.surfaceView }.getOrNull() ?: SurfaceType.TEXTURE
            val isSurfaceView = surfaceType == SurfaceType.SURFACE
            val lockContentSize = state.isScaling && isSurfaceView

            internalSurfaceView.updateLayoutParams<FrameLayout.LayoutParams> {
                width = if (lockContentSize) bufferWidth else state.windowRect.width()
                height = if (lockContentSize) bufferHeight else state.windowRect.height()
                if (lockContentSize || state.isMini) {
                    this.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                }
            }

            if (internalSurfaceView is SurfaceView && state.contentReady && !state.isScaling && !state.isMini) {
                (internalSurfaceView as SurfaceView).holder.setFixedSize(state.windowRect.width(), state.windowRect.height())
            }

            binding.vSizePreviewer.updateLayoutParams<FrameLayout.LayoutParams> {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.MATCH_PARENT
            }
            binding.vSupporter.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = scaledWidth
                height = scaledHeight
            }
            // BUG Fix: Use INVISIBLE to keep layout stable during expansion
            binding.vSupporter.visibility = if (state.contentReady) View.VISIBLE else View.INVISIBLE

            // Apply matrix transformation using intended dimensions to avoid layout-delay jitter
            applyViewportTransform(state, scaledWidth, scaledHeight)
        }
        
        safeUpdateViewLayout()
        updateExclusionRects()
    }

    override fun startOpeningAnimation(startRect: Rect?) {
        val root = binding.root
        // BUG Fix: Disable opening animation for Gesture mode as requested.
        // Directly set to final state to avoid "ugly" expansion.
        
        root.post {
            if (!root.isAttachedToWindow) return@post
            val lp = root.layoutParams as WindowManager.LayoutParams
            
            binding.cvParent.translationX = 0f
            binding.cvParent.translationY = 0f
            binding.cvParent.scaleX = 1f
            binding.cvParent.scaleY = 1f
            binding.cvParent.alpha = 1f
            binding.cvParent.isVisible = true
            
            internalSurfaceView.alpha = 1f
            
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            safeUpdateViewLayout()

            // BUG Fix: Use post to ensure one more layout pass before measuring dimensions
            if (lastState?.contentReady == false) root.post { onAppContentReady() }
        }
    }

    private fun onAppContentReady() {
        val root = binding.root
        if (!root.isAttachedToWindow) return
        val lp = root.layoutParams as WindowManager.LayoutParams
        runCommonAppContentReady(root, lp, listOf(internalSurfaceView))
    }

    override fun startClosingAnimation(onEnd: () -> Unit) {
        binding.cvParent.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(250).setInterpolator(AnticipateInterpolator()).withEndAction(onEnd).start()
    }
}
