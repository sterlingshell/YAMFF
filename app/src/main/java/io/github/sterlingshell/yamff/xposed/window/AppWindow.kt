package io.github.sterlingshell.yamff.xposed.window

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.WindowManager
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.common.model.WindowStyle
import io.github.sterlingshell.yamff.xposed.services.ConfigManager
import io.github.sterlingshell.yamff.xposed.services.YAMFFWindowManager
import io.github.sterlingshell.yamff.xposed.window.logic.AppWindowLogic
import io.github.sterlingshell.yamff.xposed.window.model.AppWindowAction
import io.github.sterlingshell.yamff.xposed.window.render.AppWindowRenderer
import io.github.sterlingshell.yamff.xposed.window.render.impl.ClassicAppWindowRenderer
import io.github.sterlingshell.yamff.xposed.window.render.impl.GestureAppWindowRenderer
import io.github.sterlingshell.yamff.xposed.utils.SystemServices

@SuppressLint("ClickableViewAccessibility", "UnspecifiedRegisterReceiverFlag")
class AppWindow(
    val context: Context,
    private val densityDpi: Int,
    private val flags: Int,
    private val startRect: Rect? = null,
    private val onVirtualDisplayCreated: ((Int) -> Unit)
) : TextureView.SurfaceTextureListener, SurfaceHolder.Callback {

    companion object {
        const val ACTION_RESET_ALL_WINDOW = "io.github.sterlingshell.yamff.ui.window.action.ACTION_RESET_ALL_WINDOW"
    }

    private val renderer: AppWindowRenderer = run {
        val style = runCatching { ConfigManager.config.windowStyle }.getOrNull() ?: WindowStyle.GESTURE
        if (style == WindowStyle.GESTURE) {
            GestureAppWindowRenderer(context)
        } else {
            ClassicAppWindowRenderer(context)
        }
    }
    private val logic: AppWindowLogic = AppWindowLogic(
        context, densityDpi, flags,
        onStateChanged = { state -> renderer.onStateChanged(state) },
        onVirtualDisplayCreated = { displayId -> onVirtualDisplayCreated(displayId) },
        onDestroyCallback = { cleanup() }
    )

    private var currentSurface: Surface? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_RESET_ALL_WINDOW) {
                logic.dispatch(AppWindowAction.ResetWindow)
            }
        }
    }

    init {
        val currentLogic = logic
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        
        val finalFlags = if (ConfigManager.config.showForceShowIME) {
            flags // Remove ALT_FOCUSABLE_IM to allow window to handle IME
        } else {
            flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            finalFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = currentLogic.state.windowRect.left
            y = currentLogic.state.windowRect.top
        }
        renderer.getRootView().layoutParams = lp

        renderer.onStateChanged(logic.state)
        
        renderer.setActionHandler { action ->
            when (action) {
                is AppWindowAction.OpenAppList -> {
                    AppListWindow.show(context, logic.displayId)
                }
                is AppWindowAction.Close -> {
                    renderer.startClosingAnimation {
                        logic.dispatch(AppWindowAction.Close)
                    }
                }
                is AppWindowAction.RequestMoveToTop -> {
                    YAMFFWindowManager.moveToTop(logic.displayId)
                    logic.dispatch(action)
                }
                else -> logic.dispatch(action)
            }
        }
        
        SystemServices.windowManager.addView(renderer.getRootView(), lp)
        
        val surfaceView = renderer.getSurfaceView()
        (surfaceView as? TextureView)?.surfaceTextureListener = this
        (surfaceView as? android.view.SurfaceView)?.holder?.addCallback(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, IntentFilter(ACTION_RESET_ALL_WINDOW), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, IntentFilter(ACTION_RESET_ALL_WINDOW))
        }
        
        renderer.startOpeningAnimation(startRect)
    }

    private fun cleanup() {
        runCatching { context.unregisterReceiver(broadcastReceiver) }
        SystemServices.windowManager.removeView(renderer.getRootView())
        currentSurface?.release()
        currentSurface = null
        YAMFFWindowManager.removeWindow(logic.displayId)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (logic.isDestroyed) return
        runCatching {
            if (width > 0 && height > 0) {
                surface.setDefaultBufferSize(width, height)
                logic.resizeDisplay(width, height)
            }
            currentSurface?.release()
            currentSurface = Surface(surface)
            logic.setSurface(currentSurface)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        if (logic.isDestroyed) return
        runCatching {
            if (width > 0 && height > 0) {
                surface.setDefaultBufferSize(width, height)
                // TextureView mode always resizes the display to match physical dimensions
                val isSurfaceView = ConfigManager.config.surfaceView == SurfaceType.SURFACE
                if (!isSurfaceView || !logic.state.isScaling) {
                    logic.resizeDisplay(width, height)
                }
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        runCatching {
            logic.setSurface(null)
        }
        currentSurface?.release()
        currentSurface = null
        return true
    }
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        runCatching {
            logic.setSurface(holder.surface)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (logic.isDestroyed) return
        runCatching {
            if (!logic.state.isScaling) {
                logic.resizeDisplay(width, height)
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        runCatching {
            logic.setSurface(null)
        }
    }
}
