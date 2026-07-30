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
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.WindowManager
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.common.model.WindowStyle
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import io.github.sterlingshell.yamff.xposed.window.logic.Controller
import io.github.sterlingshell.yamff.xposed.window.model.Action
import io.github.sterlingshell.yamff.xposed.window.render.Renderer
import io.github.sterlingshell.yamff.xposed.window.render.impl.FramedRenderer
import io.github.sterlingshell.yamff.xposed.window.render.impl.MinimalRenderer
import io.github.sterlingshell.yamff.xposed.sys.SystemServices

@SuppressLint("ClickableViewAccessibility", "UnspecifiedRegisterReceiverFlag")
class Window(
    val context: Context,
    densityDpi: Int,
    flags: Int,
    startRect: Rect? = null,
    private val onVirtualDisplayCreated: ((Int) -> Unit)
) : TextureView.SurfaceTextureListener, SurfaceHolder.Callback {

    companion object {
        const val TAG = "Window"
        const val ACTION_RESET_ALL_WINDOW = "io.github.sterlingshell.yamff.ui.window.action.ACTION_RESET_ALL_WINDOW"
    }

    val renderer: Renderer = run {
        val style = runCatching { ConfigManager.instance.config.windowStyle }.getOrNull() ?: WindowStyle.GESTURE
        if (style == WindowStyle.GESTURE) {
            MinimalRenderer(context)
        } else {
            FramedRenderer(context)
        }
    }
    private val controller: Controller = Controller(
        context, densityDpi, flags,
        onStateChanged = { state -> renderer.onStateChanged(state) },
        onVirtualDisplayCreated = { displayId -> 
            FreeformManager.addWindow(displayId, this@Window) { forceClose() }
            onVirtualDisplayCreated(displayId) 
        },
        onDestroyCallback = { dismiss() }
    )

    private var currentSurface: Surface? = null
    private var isClosing = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_RESET_ALL_WINDOW) {
                controller.dispatch(Action.ResetWindow)
            }
        }
    }

    init {
        val currentController = controller
        val lpFlags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        
        val finalFlags = if (ConfigManager.instance.config.showForceShowIME) {
            lpFlags
        } else {
            lpFlags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            finalFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = currentController.state.windowRect.left
            y = currentController.state.windowRect.top
        }
        renderer.getRootView().layoutParams = lp

        renderer.onStateChanged(controller.state)
        
        renderer.setActionHandler { action ->
            when (action) {
                is Action.OpenAppList -> {
                    AppPicker.show(context, controller.displayId)
                }
                is Action.Close -> {
                    renderer.startClosingAnimation {
                        controller.dispatch(Action.Close)
                    }
                }
                is Action.RequestMoveToTop -> {
                    FreeformManager.moveToTop(controller.displayId)
                    controller.dispatch(action)
                }
                else -> controller.dispatch(action)
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

    /**
     * Emergency close. Must be called on main thread or via controller.
     * Order of operations is CRITICAL to prevent Kernel Panic:
     * 1. Set surface to NULL (cut off data stream)
     * 2. Remove View from WindowManager
     * 3. Release VirtualDisplay (handled by controller.onDestroy)
     */
    fun forceClose() {
        if (isClosing) return
        isClosing = true
        Log.i(TAG, "forceClose executing for display ${controller.displayId}")
        
        runCatching { 
            controller.setSurface(null)
            currentSurface?.release()
            currentSurface = null
        }
        
        runCatching { 
            SystemServices.windowManager.removeView(renderer.getRootView()) 
        }
        
        runCatching {
            context.unregisterReceiver(broadcastReceiver)
        }
        
        controller.onDestroy()
        FreeformManager.removeWindow(controller.displayId)
    }

    private fun dismiss() {
        forceClose()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (controller.isDestroyed) return
        runCatching {
            if (width > 0 && height > 0) {
                surface.setDefaultBufferSize(width, height)
                controller.resizeDisplay(width, height)
            }
            currentSurface?.release()
            currentSurface = Surface(surface)
            controller.setSurface(currentSurface)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        if (controller.isDestroyed) return
        runCatching {
            if (width > 0 && height > 0) {
                surface.setDefaultBufferSize(width, height)
                val isSurfaceView = ConfigManager.instance.config.surfaceView == SurfaceType.SURFACE
                if (!isSurfaceView || !controller.state.isScaling) {
                    controller.resizeDisplay(width, height)
                }
            }
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        runCatching {
            controller.setSurface(null)
        }
        currentSurface?.release()
        currentSurface = null
        return true
    }
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        runCatching {
            controller.setSurface(holder.surface)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (controller.isDestroyed) return
        runCatching {
            if (!controller.state.isScaling) {
                controller.resizeDisplay(width, height)
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        runCatching {
            controller.setSurface(null)
        }
    }
}
