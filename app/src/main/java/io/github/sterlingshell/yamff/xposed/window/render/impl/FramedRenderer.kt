package io.github.sterlingshell.yamff.xposed.window.render.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.common.ext.getAttr
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.databinding.WindowAppFramedBinding
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.window.model.Action
import io.github.sterlingshell.yamff.xposed.window.model.State
import io.github.sterlingshell.yamff.xposed.window.model.ThemeColors
import io.github.sterlingshell.yamff.xposed.window.render.BaseRenderer
import io.github.sterlingshell.yamff.xposed.window.Bubble
import io.github.sterlingshell.yamff.xposed.sys.graphics.dpToPx

@SuppressLint("ClickableViewAccessibility", "SetTextI18n")
class FramedRenderer(context: Context) : BaseRenderer<WindowAppFramedBinding>(context) {
    override val binding = WindowAppFramedBinding.inflate(LayoutInflater.from(context))
    override lateinit var internalSurfaceView: View
    override val internalBubble = Bubble(context).apply { isVisible = false }
    override lateinit var internalBlurImageView: android.widget.ImageView
    override lateinit var internalGhostImageView: android.widget.ImageView
    override lateinit var internalViewportContainer: android.view.ViewGroup
    
    init {
        setupSurfaceView()
        setupListeners()
        setupSurfaceViewCommon()
    }

    private fun setupSurfaceView() {
        val surfaceType = runCatching { ConfigManager.config.surfaceView }.getOrNull() ?: SurfaceType.TEXTURE
        if (surfaceType == SurfaceType.SURFACE) {
            internalSurfaceView = SurfaceView(context)
        } else {
            internalSurfaceView = TextureView(context)
        }

        internalViewportContainer = FrameLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                binding.vSizePreviewer.layoutParams.width,
                binding.vSizePreviewer.layoutParams.height
            )
            clipChildren = false
            clipToPadding = false
        }

        internalBlurImageView = android.widget.ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            isVisible = false
        }
        
        internalGhostImageView = binding.ivGhost

        internalViewportContainer.addView(internalBlurImageView)
        internalViewportContainer.addView(internalSurfaceView.apply {
            id = R.id.surface
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        })

        binding.rlCardRoot.addView(internalViewportContainer, 1)
        
        setupOverlayViews(binding.root)
    }

    private fun setupListeners() {
        internalBubble.setActionHandler { internalActionHandler?.invoke(it) }

        binding.root.setOnTouchListener { _, event ->
            val state = lastState ?: return@setOnTouchListener false
            if (state.isCollapsed) return@setOnTouchListener false
            
            moveGestureDetector.onTouchEvent(event)
            internalActionHandler?.invoke(Action.RequestMoveToTop)
            
            if (event.action == MotionEvent.ACTION_UP) {
                if (state.ghostIconVisible) {
                    internalActionHandler?.invoke(Action.Minimize)
                }
            }
            true
        }

        binding.ibLockAspect.setOnClickListener { internalActionHandler?.invoke(Action.ToggleAspectLock) }
        binding.ibClose.setOnClickListener { internalActionHandler?.invoke(Action.Close) }
        binding.ibClose.setOnLongClickListener { internalActionHandler?.invoke(Action.OpenAppList); true }
        binding.ibFullscreen.setOnClickListener { internalActionHandler?.invoke(Action.Fullscreen) }
        binding.ibFullscreen.setOnLongClickListener { internalActionHandler?.invoke(Action.ToggleMini); true }
        binding.ibRotate.setOnClickListener { internalActionHandler?.invoke(Action.Rotate) }
        binding.ibBack.setOnClickListener { internalActionHandler?.invoke(Action.Back) }
        binding.ibBack.setOnLongClickListener { internalActionHandler?.invoke(Action.Home); true }
        
        binding.ibResize.setOnTouchListener(ResizeTouchHandler(binding.vSizePreviewer, binding.tvSize))

        internalSurfaceView.setOnTouchListener { _, event ->
            internalActionHandler?.invoke(Action.SurfaceTouch(event))
            internalActionHandler?.invoke(Action.RequestMoveToTop)
            true
        }
        internalSurfaceView.setOnGenericMotionListener { _, event ->
            internalActionHandler?.invoke(Action.SurfaceGenericMotion(event))
            true
        }
    }

    override fun onStateChanged(state: State) {
        val oldState = lastState
        lastState = state
        
        binding.cvApp.clipToOutline = true
        
        if (state.isCollapsed != oldState?.isCollapsed) {
            if (state.isCollapsed) animateCommonCollapsed(binding.cvApp)
            else {
                if (!state.isMini) binding.rlTop.isVisible = true
                animateCommonExpanded(binding.cvApp)
                updateMiniUI(state.isMini)
            }
        }
        
        if (state.appIcon != oldState?.appIcon) {
            binding.ivIcon.setImageDrawable(state.appIcon)
            internalBubble.setIcon(state.appIcon)
            binding.ivGhost.setImageDrawable(state.appIcon)
            binding.ivGhost.updateLayoutParams {
                width = 48.dpToPx().toInt()
                height = 48.dpToPx().toInt()
            }
        }
        if (state.appLabel != oldState?.appLabel) binding.tvLabel.text = state.appLabel
        if (state.themeColors != oldState?.themeColors) applyThemeColors(state.themeColors)
        if (state.isMini != oldState?.isMini) updateMiniUI(state.isMini)
        
        updateWindowLayout(state)
        
        binding.ibLockAspect.imageTintList = ColorStateList.valueOf(
            if (state.isAspectLocked) context.theme.getAttr(com.google.android.material.R.attr.colorPrimary).data
            else context.theme.getAttr(com.google.android.material.R.attr.colorOnSurfaceVariant).data
        )
    }

    override fun applyThemeColors(colors: ThemeColors) {
        if (colors.appCardBackground != 0) {
            binding.cvApp.setCardBackgroundColor(colors.appCardBackground)
            binding.rlCardRoot.setBackgroundColor(colors.appCardBackground)
            binding.rlTop.setBackgroundColor(colors.statusBarColor)
            binding.tvLabel.setTextColor(colors.onStatusBarColor)
            binding.ibClose.imageTintList = ColorStateList.valueOf(colors.onStatusBarColor)
            
            binding.rlButton.setBackgroundColor(colors.navigationBarColor)
            binding.ibBack.imageTintList = ColorStateList.valueOf(colors.onNavigationBarColor)
            binding.ibRotate.imageTintList = ColorStateList.valueOf(colors.onNavigationBarColor)
            binding.ibFullscreen.imageTintList = ColorStateList.valueOf(colors.onNavigationBarColor)
            binding.ibResize.imageTintList = ColorStateList.valueOf(colors.onNavigationBarColor)
            binding.ibIme.imageTintList = ColorStateList.valueOf(colors.onNavigationBarColor)
            binding.ibLockAspect.imageTintList = ColorStateList.valueOf(colors.onNavigationBarColor)
        }
    }

    override fun updateWindowLayout(state: State) {
        val root = binding.root
        val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
        
        updateCommonWindowLayout(state, lp)
        
        if (state.isCollapsed) {
            binding.vSupporter.visibility = View.GONE
            binding.vSizePreviewer.visibility = View.GONE
            binding.cvApp.visibility = View.GONE
        } else {
            binding.cvApp.visibility = View.VISIBLE
            
            val containerScale = if (state.isMini) 0.5f else 1.0f
            val containerW = (state.windowRect.width() * containerScale).toInt()
            val containerH = (state.windowRect.height() * containerScale).toInt()
            
            binding.rlTop.updateLayoutParams { width = containerW }
            binding.rlButton.updateLayoutParams { width = containerW }
            
            internalViewportContainer.updateLayoutParams<LinearLayout.LayoutParams> {
                width = containerW
                height = if (state.contentReady) containerH else 0
            }
            
            val surfaceType = runCatching { ConfigManager.config.surfaceView }.getOrNull() ?: SurfaceType.TEXTURE
            val isSurfaceView = surfaceType == SurfaceType.SURFACE
            val lockContentSize = state.isScaling && isSurfaceView

            internalSurfaceView.updateLayoutParams<FrameLayout.LayoutParams> {
                width = if (lockContentSize) bufferWidth else state.windowRect.width()
                height = if (lockContentSize) bufferHeight else state.windowRect.height()
            }
            
            if (internalSurfaceView is SurfaceView && state.contentReady && !state.isScaling && !state.isMini) {
                (internalSurfaceView as SurfaceView).holder.setFixedSize(state.windowRect.width(), state.windowRect.height())
            }

            binding.vSizePreviewer.updateLayoutParams<FrameLayout.LayoutParams> {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.MATCH_PARENT
            }
            binding.vSupporter.updateLayoutParams {
                width = containerW
                height = containerH
            }
            binding.vSupporter.visibility = if (state.contentReady) View.VISIBLE else View.INVISIBLE
            
            binding.rlTop.isVisible = !state.isMini
            binding.rlButton.isVisible = state.contentReady && !state.isMini
            binding.ibResize.isVisible = !state.isMini

            binding.ivGhost.isVisible = state.ghostIconVisible
            if (state.ghostIconVisible) {
                binding.ivGhost.x = state.ghostIconPos.first
                binding.ivGhost.y = state.ghostIconPos.second
            }

            applyViewportTransform(state, containerW, containerH)
        }
        
        safeUpdateViewLayout()
        updateExclusionRects()
    }

    override fun startOpeningAnimation(startRect: Rect?) {
        val root = binding.root
        val effectiveStartRect = startRect ?: Rect(
            context.resources.displayMetrics.widthPixels / 2 - 24.dpToPx().toInt(),
            context.resources.displayMetrics.heightPixels / 2 - 24.dpToPx().toInt(),
            context.resources.displayMetrics.widthPixels / 2 + 24.dpToPx().toInt(),
            context.resources.displayMetrics.heightPixels / 2 + 24.dpToPx().toInt()
        )

        root.post {
            if (!root.isAttachedToWindow) return@post
            val lp = root.layoutParams as WindowManager.LayoutParams
            val destX = lp.x
            val destY = lp.y
            
            root.measure(View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
                         View.MeasureSpec.makeMeasureSpec(context.resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST))
            
            val iconSize = 48.dpToPx()
            val startScale = effectiveStartRect.width().toFloat() / iconSize
            
            binding.cvApp.translationX = (effectiveStartRect.centerX() - (destX + root.measuredWidth / 2)).toFloat()
            binding.cvApp.translationY = (effectiveStartRect.centerY() - (destY + root.measuredHeight / 2)).toFloat()
            binding.cvApp.scaleX = startScale
            binding.cvApp.scaleY = startScale
            binding.cvApp.alpha = if (startRect == null) 0f else 1f
            
            binding.cvApp.animate()
                .translationX(0f).translationY(0f)
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(500).setInterpolator(OvershootInterpolator()).start()
            
            internalSurfaceView.alpha = 0f
            if (internalSurfaceView is SurfaceView) {
                internalSurfaceView.scaleX = 0.8f
                internalSurfaceView.scaleY = 0.8f
                internalSurfaceView.animate().scaleX(1f).scaleY(1f).setDuration(500).start()
            }
            
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            safeUpdateViewLayout()

            root.postDelayed({ if (lastState?.contentReady == false) onAppContentReady() }, 2000)
        }
    }
    
    private fun onAppContentReady() {
        val root = binding.root
        if (!root.isAttachedToWindow) return
        val lp = root.layoutParams as WindowManager.LayoutParams
        runCommonAppContentReady(root, lp, listOf(internalSurfaceView, binding.rlButton))
    }

    override fun startClosingAnimation(onEnd: () -> Unit) {
        binding.cvApp.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(250).setInterpolator(AnticipateInterpolator()).withEndAction(onEnd).start()
    }
}
