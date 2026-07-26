package io.github.sterlingshell.yamff.xposed.window.logic

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.ITaskStackListener
import android.content.ComponentName
import android.content.Context
import android.content.pm.IPackageManagerHidden
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.IRotationWatcher
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowManagerHidden
import android.window.TaskSnapshot
import androidx.core.graphics.ColorUtils
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.google.android.material.color.MaterialColors
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.extensions.getAttr
import io.github.sterlingshell.yamff.common.extensions.runMain
import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.xposed.services.ConfigManager
import io.github.sterlingshell.yamff.xposed.services.YAMFFWindowManager
import io.github.sterlingshell.yamff.xposed.window.model.AppWindowAction
import io.github.sterlingshell.yamff.xposed.window.model.AppWindowState
import io.github.sterlingshell.yamff.xposed.window.model.ThemeColors
import io.github.sterlingshell.yamff.xposed.utils.SystemServices
import io.github.sterlingshell.yamff.xposed.utils.MainThreadQueue
import io.github.sterlingshell.yamff.xposed.utils.TipUtil
import io.github.sterlingshell.yamff.xposed.utils.graphics.YAMFFRoundedDrawable
import io.github.sterlingshell.yamff.xposed.utils.graphics.dpToPx
import io.github.sterlingshell.yamff.xposed.utils.extensions.getActivityInfoCompat
import kotlinx.coroutines.delay
import kotlin.math.abs

class AppWindowLogic(
    private val context: Context,
    private val densityDpi: Int,
    private val flags: Int,
    private val onStateChanged: (AppWindowState) -> Unit,
    private val onVirtualDisplayCreated: (Int) -> Unit,
    private val onDestroyCallback: () -> Unit
) {
    companion object {
        const val TAG = "AppWindowLogic"
        
        // Constants for magic numbers
        const val FLING_THRESHOLD = 2000f
        const val MINI_SCALE = 0.5f
        const val MIN_WINDOW_SIZE_DP = 100
        const val BUBBLE_WIDTH_COLLAPSED_DP = 56
        const val BUBBLE_RADIUS_DP = 28
        const val BUBBLE_WIDTH_MINI_DP = 48
    }

    var state = AppWindowState()
        private set
    private val stateLock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val taskStackListener = TaskStackListener()
    private val rotationWatcher = RotationWatcher()

    private val gestureHandler = WindowGestureHandler(context, { state }, { updateState(it) }, { dispatch(it) })
    private val autoHideManager = AutoHideManager(context, handler, { state }, { updateState(it) })
    
    private var virtualDisplay: VirtualDisplay? = null
    var displayId = -1
        private set
    var hostedTaskId = -1
        private set
    var isDestroyed = false
        private set
    private var rotateLock = false

    data class TaskInfoSnapshot(
        val taskId: Int,
        val userId: Int,
        val topActivity: ComponentName?,
        val isVisible: Boolean
    )

    init {
        val defaultWidth = ConfigManager.config.defaultWindowWidth.dpToPx().toInt()
        val defaultHeight = ConfigManager.config.defaultWindowHeight.dpToPx().toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        val initialRect = Rect(
            (screenWidth - defaultWidth) / 2,
            (screenHeight - defaultHeight) / 2,
            (screenWidth + defaultWidth) / 2,
            (screenHeight + defaultHeight) / 2
        )

        updateState { it.copy(windowRect = initialRect) }

        runCatching {
            val vd = SystemServices.displayManager.createVirtualDisplay("yamff${System.currentTimeMillis()}", initialRect.width(), initialRect.height(), densityDpi, null, flags)
            virtualDisplay = vd
            displayId = vd.display.displayId
            updateState { it.copy(displayId = displayId) }
            
            (SystemServices.windowManager as WindowManagerHidden).setDisplayImePolicy(displayId, if (ConfigManager.config.showImeInWindow) WindowManagerHidden.DISPLAY_IME_POLICY_LOCAL else WindowManagerHidden.DISPLAY_IME_POLICY_FALLBACK_DISPLAY)
            
            SystemServices.activityTaskManager.registerTaskStackListener(taskStackListener)
            
            fun watchRotation() {
                runCatching {
                    SystemServices.iWindowManager.watchRotation(rotationWatcher, displayId)
                }.onFailure {
                    Log.d(TAG, "watchRotation: fail")
                    if (!isDestroyed) handler.postDelayed({ watchRotation() }, 100)
                }
            }
            watchRotation()
            
            onVirtualDisplayCreated(displayId)
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize AppWindowLogic", e)
            runMain { onDestroy() }
        }
    }

    fun setSurface(surface: Surface?) {
        if (isDestroyed) return
        runCatching {
            virtualDisplay?.surface = surface
        }
    }

    fun resizeDisplay(width: Int, height: Int) {
        if (isDestroyed) return
        runCatching {
            if (width > 0 && height > 0) {
                virtualDisplay?.resize(width, height, densityDpi)
            }
        }
    }

    fun dispatch(action: AppWindowAction) {
        if (isDestroyed) return
        when (action) {
            is AppWindowAction.Close -> handleClose()
            is AppWindowAction.Minimize -> handleMinimize()
            is AppWindowAction.Expand -> handleExpand()
            is AppWindowAction.Fullscreen -> handleFullscreen()
            is AppWindowAction.Rotate -> rotate(Surface.ROTATION_90)
            is AppWindowAction.ToggleAspectLock -> toggleAspectLock()
            is AppWindowAction.ShowIme -> {} 
            is AppWindowAction.Back -> injectKey(KeyEvent.KEYCODE_BACK)
            is AppWindowAction.Home -> injectKey(KeyEvent.KEYCODE_HOME)
            is AppWindowAction.OpenAppList -> {} 
            is AppWindowAction.Move -> gestureHandler.handleMove(action.dx, action.dy)
            is AppWindowAction.Fling -> gestureHandler.handleFling(action.velocityX, action.velocityY)
            is AppWindowAction.UpdateDimensions -> handleUpdateDimensions(action.width, action.height)
            is AppWindowAction.SurfaceTouch -> forwardMotionEvent(action.event)
            is AppWindowAction.SurfaceGenericMotion -> forwardMotionEvent(action.event)
            is AppWindowAction.RequestMoveToTop -> moveToTop()
            is AppWindowAction.OpeningAnimationEnd -> updateState { it.copy(contentReady = true) }
            is AppWindowAction.SnapToEdge -> gestureHandler.snapToEdge { isDestroyed }
            is AppWindowAction.FlickAwayBubble -> handleFlickAwayBubble(action.velocityX)
            is AppWindowAction.StartBubbleDrag -> {
                autoHideManager.cancelAutoHideTimer()
                autoHideManager.cancelAnimations()
                updateState { it.copy(
                    isDraggingBubble = true,
                    bubbleWidth = BUBBLE_WIDTH_MINI_DP.dpToPx().toInt(),
                    bubbleRadius = BUBBLE_RADIUS_DP.dpToPx()
                ) }
            }
            is AppWindowAction.EndBubbleDrag -> {
                updateState { it.copy(
                    isDraggingBubble = false,
                    bubbleWidth = BUBBLE_WIDTH_COLLAPSED_DP.dpToPx().toInt(),
                    bubbleRadius = BUBBLE_RADIUS_DP.dpToPx()
                ) }
                autoHideManager.startAutoHideTimer()
            }
            is AppWindowAction.TapBubble -> handleExpand()
            is AppWindowAction.LongPressBubble -> {}
            is AppWindowAction.ResetWindow -> resetWindow()
            is AppWindowAction.ToggleMini -> toggleMini()
            is AppWindowAction.WakeUpBubble -> handleWakeUpBubble()
            is AppWindowAction.SetScaling -> updateState { 
                it.copy(isScaling = action.scaling, isMini = if (action.scaling) false else it.isMini) 
            }
        }
    }

    private fun toggleMini() {
        updateState { it.copy(isMini = !it.isMini) }
    }

    private fun handleUpdateDimensions(width: Int, height: Int) {
        val rect = Rect(state.windowRect)
        rect.right = rect.left + width
        rect.bottom = rect.top + height
        updateState { it.copy(windowRect = rect) }
    }

    private fun resetWindow() {
        val defaultWidth = ConfigManager.config.defaultWindowWidth.dpToPx().toInt()
        val defaultHeight = ConfigManager.config.defaultWindowHeight.dpToPx().toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        
        val targetX = (screenWidth - defaultWidth) / 2
        val targetY = (screenHeight - defaultHeight) / 2
        
        gestureHandler.animateTo(targetX, targetY) { isDestroyed }
        gestureHandler.animateResize(defaultWidth, defaultHeight) { isDestroyed }
    }

    private fun handleClose() {
        onDestroy()
    }

    private fun handleFullscreen() {
        val task = getTopRootTask()
        if (task != null) {
            runCatching {
                SystemServices.activityTaskManager.moveRootTaskToDisplay(task.taskId, 0)
            }.onFailure { t ->
                TipUtil.showToast("${t.message}")
            }.onSuccess {
                handleClose()
            }
        }
    }

    private fun toggleAspectLock() {
        updateState { it.copy(isAspectLocked = !it.isAspectLocked) }
        TipUtil.showToast(if (state.isAspectLocked) "Aspect Ratio Locked" else "Aspect Ratio Unlocked")
        if (!state.isAspectLocked) {
            updateState { it.copy(scaleFactor = 1.0f) }
        }
    }

    private fun handleMinimize() {
        updateState { it.copy(
            isCollapsed = true, 
            ghostIconVisible = false,
            bubbleWidth = BUBBLE_WIDTH_COLLAPSED_DP.dpToPx().toInt(),
            bubbleRadius = BUBBLE_RADIUS_DP.dpToPx()
        ) }
        setFrameRate(15f)
        gestureHandler.snapToEdge { isDestroyed }
        autoHideManager.startAutoHideTimer()
    }

    private fun handleExpand() {
        updateState { it.copy(isCollapsed = false, isAutoHide = false) }
        setFrameRate(0f)
        autoHideManager.cancelAutoHideTimer()
        
        val rect = Rect(state.windowRect)
        if (rect.left < 0) rect.offset(-rect.left, 0)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val windowWidth = (rect.width() * state.scaleFactor).toInt()
        if (rect.left + windowWidth > screenWidth) rect.offset(screenWidth - (rect.left + windowWidth), 0)
        updateState { it.copy(windowRect = rect) }
    }

    private fun handleWakeUpBubble() {
        if (!state.isCollapsed) return
        autoHideManager.cancelAutoHideTimer()
        autoHideManager.cancelAnimations()
        if (state.isAutoHide) {
            autoHideManager.exitAutoHide()
        }
        autoHideManager.startAutoHideTimer()
    }

    private fun handleFlickAwayBubble(velocityX: Float) {
        if (ConfigManager.config.enableFlickAway && abs(velocityX) > FLING_THRESHOLD) {
            handleClose()
        } else {
            gestureHandler.snapToEdge { isDestroyed }
        }
    }

    private fun setFrameRate(fps: Float) {
        if (isDestroyed) return
        val surface = virtualDisplay?.surface
        if (surface != null && surface.isValid) {
            SystemCompat.setFrameRate(surface, fps)
        }
    }

    fun onDestroy() {
        if (isDestroyed) return
        isDestroyed = true
        autoHideManager.cancelAutoHideTimer()
        handler.removeCallbacksAndMessages(null)
        gestureHandler.cancelAnimations()
        runCatching { SystemServices.iWindowManager.removeRotationWatcher(rotationWatcher) }
        runCatching { SystemServices.activityTaskManager.unregisterTaskStackListener(taskStackListener) }
        runCatching { virtualDisplay?.surface = null }
        runCatching { virtualDisplay?.release() }
        onDestroyCallback()
    }

    private fun getTopRootTask(): ActivityTaskManager.RootTaskInfo? {
        if (displayId == -1) return null
        SystemServices.activityTaskManager.getAllRootTaskInfosOnDisplay(displayId).forEach { task ->
            if (task.visible) return task
        }
        return null
    }

    private fun moveToTop() {
        if (displayId == -1) return
        YAMFFWindowManager.moveToTop(displayId)
        SystemCompat.setFocusedDisplay(SystemServices.iWindowManager, displayId)
        updateState { it.copy(isFocused = true) }
    }

    private fun injectKey(keyCode: Int) {
        if (displayId == -1) return
        val down = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0).apply {
            source = InputDevice.SOURCE_KEYBOARD
            SystemCompat.setDisplayId(this, displayId)
        }
        SystemServices.inputManager.injectInputEvent(down, 0)
        val up = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0).apply {
            source = InputDevice.SOURCE_KEYBOARD
            SystemCompat.setDisplayId(this, displayId)
        }
        SystemServices.inputManager.injectInputEvent(up, 0)
    }

    private fun forwardMotionEvent(event: MotionEvent) {
        if (displayId == -1) return
        val scale = state.scaleFactor * if (state.isMini) MINI_SCALE else 1f
        val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(event.pointerCount)
        val pointerProperties: Array<MotionEvent.PointerProperties?> = arrayOfNulls(event.pointerCount)
        for (i in 0 until event.pointerCount) {
            val oldCoords = MotionEvent.PointerCoords()
            val pointerProperty = MotionEvent.PointerProperties()
            event.getPointerCoords(i, oldCoords)
            event.getPointerProperties(i, pointerProperty)
            pointerCoords[i] = oldCoords
            pointerCoords[i]!!.apply {
                x = oldCoords.x / scale
                y = oldCoords.y / scale
            }
            pointerProperties[i] = pointerProperty
        }

        val newEvent = MotionEvent.obtain(
            event.downTime, event.eventTime, event.action, event.pointerCount,
            pointerProperties, pointerCoords, event.metaState, event.buttonState,
            event.xPrecision, event.yPrecision, event.deviceId, event.edgeFlags,
            event.source, event.flags
        )
        SystemCompat.setDisplayId(newEvent, displayId)
        SystemServices.inputManager.injectInputEvent(newEvent, 0)
        // BUG Fix: Recycle after synchronous injection is complete to prevent Native memory leak.
        newEvent.recycle()
    }

    fun rotate(rotation: Int) {
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            val rect = Rect(state.windowRect)
            val w = rect.width()
            val h = rect.height()
            rect.right = rect.left + h
            rect.bottom = rect.top + w
            updateState { it.copy(windowRect = rect) }
        }
    }

    private fun updateState(block: (AppWindowState) -> AppWindowState) {
        // BUG Fix: Synchronize logic update to prevent race conditions from Binder threads.
        // Keep UI notification on main thread to avoid layout thread crashes.
        val newState: AppWindowState
        synchronized(stateLock) {
            state = block(state)
            newState = state
        }
        handler.post { onStateChanged(newState) }
    }

    private fun updateTask(snapshot: TaskInfoSnapshot) {
        hostedTaskId = snapshot.taskId
        YAMFFWindowManager.associateTaskWithDisplay(hostedTaskId, displayId)
        MainThreadQueue.add {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && !snapshot.isVisible) {
                delay(500)
            }
            val topActivity = snapshot.topActivity ?: return@add
            val taskDescription = SystemServices.activityTaskManager.getTaskDescription(snapshot.taskId) ?: return@add
            val activityInfo = (SystemServices.iPackageManager as IPackageManagerHidden).getActivityInfoCompat(topActivity, 0, snapshot.userId) ?: return@add
            
            val icon = YAMFFRoundedDrawable().apply {
                drawable = runCatching { taskDescription.icon }.getOrNull()?.let { BitmapDrawable(it) } ?: activityInfo.loadIcon(SystemServices.packageManager)
                isClipEnabled = true
                radius = 100
            }
            
            val label = taskDescription.label ?: activityInfo.loadLabel(SystemServices.packageManager)
            val debugLabel = if (BuildConfig.DEBUG) "(${snapshot.taskId}-$displayId) $label" else label
            
            var themeColors = ThemeColors()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ConfigManager.config.coloredController) {
                val backgroundColor = taskDescription.backgroundColor
                val statusBarColor = taskDescription.statusBarColor
                val navigationBarColor = taskDescription.navigationBarColor
                
                val onStatusBar = if (MaterialColors.isColorLight(ColorUtils.compositeColors(statusBarColor, backgroundColor)) xor ((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
                    context.theme.getAttr(com.google.android.material.R.attr.colorOnPrimaryContainer).data
                } else {
                    context.theme.getAttr(com.google.android.material.R.attr.colorOnPrimary).data
                }
                
                val onNavigationBar = if (MaterialColors.isColorLight(ColorUtils.compositeColors(navigationBarColor, backgroundColor)) xor ((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
                    context.theme.getAttr(com.google.android.material.R.attr.colorOnPrimaryContainer).data
                } else {
                    context.theme.getAttr(com.google.android.material.R.attr.colorOnPrimary).data
                }
                
                themeColors = ThemeColors(
                    appCardBackground = backgroundColor,
                    statusBarColor = statusBarColor,
                    navigationBarColor = navigationBarColor,
                    onStatusBarColor = onStatusBar,
                    onNavigationBarColor = onNavigationBar
                )
            }
            
            updateState { it.copy(
                taskId = hostedTaskId,
                appIcon = icon,
                appLabel = debugLabel,
                themeColors = themeColors
            ) }
        }
    }

    inner class TaskStackListener : ITaskStackListener.Stub() {
        private fun ActivityManager.RunningTaskInfo.toSnapshot() = TaskInfoSnapshot(
            taskId = taskId,
            userId = runCatching { getObjectAs<Int>("userId") }.getOrDefault(0),
            topActivity = topActivity,
            isVisible = runCatching { getObjectAs<Boolean>("isVisible") }.getOrDefault(true)
        )

        override fun onTaskStackChanged() {}
        override fun onActivityPinned(packageName: String?, userId: Int, taskId: Int, stackId: Int) {}
        override fun onActivityUnpinned() {}
        override fun onActivityRestartAttempt(task: ActivityManager.RunningTaskInfo?, homeTaskVisible: Boolean, clearedTask: Boolean, wasVisible: Boolean) {}
        override fun onActivityForcedResizable(packageName: String?, taskId: Int, reason: Int) {}
        override fun onActivityDismissingDockedTask() {}
        override fun onActivityLaunchOnSecondaryDisplayFailed(taskInfo: ActivityManager.RunningTaskInfo?, requestedDisplayId: Int) {}
        override fun onActivityLaunchOnSecondaryDisplayRerouted(taskInfo: ActivityManager.RunningTaskInfo?, requestedDisplayId: Int) {}
        override fun onTaskCreated(taskId: Int, componentName: ComponentName?) {}
        override fun onTaskRemoved(taskId: Int) {
            if (taskId == hostedTaskId) {
                runMain { handleClose() }
            }
        }
        override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo) {
            if (taskInfo.getObject("displayId") == displayId) {
                updateTask(taskInfo.toSnapshot())
            }
        }
        override fun onTaskDescriptionChanged(taskInfo: ActivityManager.RunningTaskInfo) {
            if (taskInfo.getObject("displayId") == displayId) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && !taskInfo.isVisible){
                    return
                }
                updateTask(taskInfo.toSnapshot())
            }
        }
        override fun onActivityRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {}
        override fun onTaskRemovalStarted(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onTaskProfileLocked(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onTaskSnapshotChanged(taskId: Int, snapshot: TaskSnapshot?) {}
        override fun onBackPressedOnTaskRoot(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onTaskDisplayChanged(taskId: Int, newDisplayId: Int) {}
        override fun onRecentTaskListUpdated() {}
        override fun onRecentTaskListFrozenChanged(frozen: Boolean) {}
        override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {}
        override fun onTaskRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {}
        override fun onActivityRotation(displayId: Int) {}
        override fun onTaskMovedToBack(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onLockTaskModeChanged(mode: Int) {}
    }

    inner class RotationWatcher : IRotationWatcher.Stub() {
        override fun onRotationChanged(rotation: Int) {
            runMain {
                if (!rotateLock) rotate(rotation)
            }
        }
    }
}
