package io.github.sterlingshell.yamff.xposed.window.logic

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.ITaskStackListener
import android.content.ComponentName
import android.content.Context
import android.content.pm.IPackageManagerHidden
import android.content.res.Configuration
import android.graphics.Rect
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
import androidx.core.graphics.drawable.toDrawable
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.google.android.material.color.MaterialColors
import io.github.sterlingshell.yamff.BuildConfig
import io.github.sterlingshell.yamff.common.ext.getAttr
import io.github.sterlingshell.yamff.common.ext.runMain
import io.github.sterlingshell.yamff.xposed.compat.SystemCompat
import io.github.sterlingshell.yamff.xposed.core.ConfigManager
import io.github.sterlingshell.yamff.xposed.core.FreeformManager
import io.github.sterlingshell.yamff.xposed.window.model.Action
import io.github.sterlingshell.yamff.xposed.window.model.State
import io.github.sterlingshell.yamff.xposed.window.model.ThemeColors
import io.github.sterlingshell.yamff.xposed.sys.SystemServices
import io.github.sterlingshell.yamff.xposed.util.MainThreadQueue
import io.github.sterlingshell.yamff.xposed.util.Toast
import io.github.sterlingshell.yamff.xposed.sys.graphics.RoundedDrawable
import io.github.sterlingshell.yamff.xposed.sys.graphics.dpToPx
import io.github.sterlingshell.yamff.xposed.util.ext.getActivityInfoCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

class Controller(
    private val context: Context,
    private var densityDpi: Int,
    private val flags: Int,
    private val onStateChanged: (State) -> Unit,
    private val onVirtualDisplayCreated: (Int) -> Unit,
    private val onDestroyCallback: () -> Unit
) {
    companion object {
        const val TAG = "Controller"
        
        const val FLING_THRESHOLD = 2000f
        const val MINI_SCALE = 0.5f
        const val BUBBLE_WIDTH_COLLAPSED_DP = 56
        const val BUBBLE_RADIUS_DP = 28
        const val BUBBLE_WIDTH_MINI_DP = 48
        
        private const val BASE_DPI = 160
    }

    var state = State()
        private set
    private val stateLock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private val taskStackListener = TaskStackListener()
    private val rotationWatcher = RotationWatcher()

    private val gestureHandler = GestureHandler(context, { state }, { updateState(it) }, { dispatch(it) })
    private val autoHideHandler = AutoHideHandler(context, handler, { state }, { updateState(it) })
    
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
        val config = ConfigManager.instance.config
        val defaultWidth = config.defaultWindowWidth.dpToPx().toInt()
        val defaultHeight = config.defaultWindowHeight.dpToPx().toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        val initialRect = Rect(
            (screenWidth - defaultWidth) / 2,
            (screenHeight - defaultHeight) / 2,
            (screenWidth + defaultWidth) / 2,
            (screenHeight + defaultHeight) / 2
        )

        updateState { it.copy(windowRect = initialRect) }
        
        densityDpi = calculateDensity(initialRect.width())

        runCatching {
            val vd = SystemServices.displayManager.createVirtualDisplay("YAMFF${System.currentTimeMillis()}", initialRect.width(), initialRect.height(), densityDpi, null, flags)
            virtualDisplay = vd
            displayId = vd.display.displayId
            updateState { it.copy(displayId = displayId) }
            
            (SystemServices.windowManager as WindowManagerHidden).setDisplayImePolicy(displayId, if (config.showImeInWindow) WindowManagerHidden.DISPLAY_IME_POLICY_LOCAL else WindowManagerHidden.DISPLAY_IME_POLICY_FALLBACK_DISPLAY)
            
            SystemServices.activityTaskManager.registerTaskStackListener(taskStackListener)
            
            fun watchRotation() {
                runCatching {
                    SystemServices.iWindowManager.watchRotation(rotationWatcher, displayId)
                }.onFailure {
                    if (!isDestroyed) handler.postDelayed({ watchRotation() }, 100)
                }
            }
            watchRotation()
            
            onVirtualDisplayCreated(displayId)
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize Controller", e)
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
                densityDpi = calculateDensity(width)
                virtualDisplay?.resize(width, height, densityDpi)
            }
        }
    }

    private fun calculateDensity(width: Int): Int {
        val config = ConfigManager.instance.config
        return if (config.dpiMode == io.github.sterlingshell.yamff.common.model.DpiMode.AUTO) {
            val calculated = (width.toFloat() / config.autoDpiTargetWidth * BASE_DPI).toInt()
            calculated.coerceIn(72, 1000)
        } else {
            config.densityDpi
        }
    }

    fun dispatch(action: Action) {
        if (isDestroyed) return
        when (action) {
            is Action.Close -> handleClose()
            is Action.Minimize -> handleMinimize()
            is Action.Expand -> handleExpand()
            is Action.Fullscreen -> handleFullscreen()
            is Action.Rotate -> rotate(Surface.ROTATION_90)
            is Action.ToggleAspectLock -> toggleAspectLock()
            is Action.ShowIme -> {} 
            is Action.Back -> injectKey(KeyEvent.KEYCODE_BACK)
            is Action.Home -> injectKey(KeyEvent.KEYCODE_HOME)
            is Action.OpenAppList -> {} 
            is Action.Move -> gestureHandler.handleMove(action.dx, action.dy)
            is Action.Fling -> gestureHandler.handleFling(action.velocityX, action.velocityY)
            is Action.UpdateDimensions -> handleUpdateDimensions(action.width, action.height)
            is Action.SurfaceTouch -> forwardMotionEvent(action.event)
            is Action.SurfaceGenericMotion -> forwardMotionEvent(action.event)
            is Action.RequestMoveToTop -> moveToTop()
            is Action.OpeningAnimationEnd -> updateState { it.copy(contentReady = true) }
            is Action.SnapToEdge -> gestureHandler.snapToEdge { isDestroyed }
            is Action.FlickAwayBubble -> handleFlickAwayBubble(action.velocityX)
            is Action.StartBubbleDrag -> {
                autoHideHandler.cancelAutoHideTimer()
                autoHideHandler.cancelAnimations()
                updateState { it.copy(
                    isDraggingBubble = true,
                    bubbleWidth = BUBBLE_WIDTH_MINI_DP.dpToPx().toInt(),
                    bubbleRadius = BUBBLE_RADIUS_DP.dpToPx()
                ) }
            }
            is Action.EndBubbleDrag -> {
                updateState { it.copy(
                    isDraggingBubble = false,
                    bubbleWidth = BUBBLE_WIDTH_COLLAPSED_DP.dpToPx().toInt(),
                    bubbleRadius = BUBBLE_RADIUS_DP.dpToPx()
                ) }
                autoHideHandler.startAutoHideTimer()
            }
            is Action.TapBubble -> handleExpand()
            is Action.LongPressBubble -> {}
            is Action.ResetWindow -> resetWindow()
            is Action.ToggleMini -> toggleMini()
            is Action.WakeUpBubble -> handleWakeUpBubble()
            is Action.SetScaling -> updateState { 
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
        val defaultWidth = ConfigManager.instance.config.defaultWindowWidth.dpToPx().toInt()
        val defaultHeight = ConfigManager.instance.config.defaultWindowHeight.dpToPx().toInt()
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
                Toast.show("${t.message}")
            }.onSuccess {
                handleClose()
            }
        }
    }

    private fun toggleAspectLock() {
        updateState { it.copy(isAspectLocked = !it.isAspectLocked) }
        Toast.show(if (state.isAspectLocked) "Aspect Ratio Locked" else "Aspect Ratio Unlocked")
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
        autoHideHandler.startAutoHideTimer()
    }

    private fun handleExpand() {
        updateState { it.copy(isCollapsed = false, isAutoHide = false) }
        setFrameRate(0f)
        autoHideHandler.cancelAutoHideTimer()
        
        val rect = Rect(state.windowRect)
        if (rect.left < 0) rect.offset(-rect.left, 0)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val windowWidth = (rect.width() * state.scaleFactor).toInt()
        if (rect.left + windowWidth > screenWidth) rect.offset(screenWidth - (rect.left + windowWidth), 0)
        updateState { it.copy(windowRect = rect) }
    }

    private fun handleWakeUpBubble() {
        if (!state.isCollapsed) return
        autoHideHandler.cancelAutoHideTimer()
        autoHideHandler.cancelAnimations()
        if (state.isAutoHide) {
            autoHideHandler.exitAutoHide()
        }
        autoHideHandler.startAutoHideTimer()
    }

    private fun handleFlickAwayBubble(velocityX: Float) {
        if (ConfigManager.instance.config.enableFlickAway && abs(velocityX) > FLING_THRESHOLD) {
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
        autoHideHandler.cancelAutoHideTimer()
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
        FreeformManager.moveToTop(displayId)
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

    private fun updateState(block: (State) -> State) {
        val newState: State
        synchronized(stateLock) {
            state = block(state)
            newState = state
        }
        handler.post { onStateChanged(newState) }
    }

    private fun updateTask(snapshot: TaskInfoSnapshot) {
        hostedTaskId = snapshot.taskId
        FreeformManager.associateTaskWithDisplay(hostedTaskId, displayId)
        MainThreadQueue.add {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && !snapshot.isVisible) {
                delay(500.milliseconds)
            }
            val topActivity = snapshot.topActivity ?: return@add
            val taskDescription = SystemCompat.getTaskDescription(snapshot.taskId) ?: return@add
            val activityInfo = (SystemServices.iPackageManager as IPackageManagerHidden).getActivityInfoCompat(topActivity, 0, snapshot.userId)
            
            val icon = RoundedDrawable().apply {
                @Suppress("DEPRECATION")
                drawable = runCatching { taskDescription.icon }.getOrNull()?.toDrawable(context.resources) ?: activityInfo.loadIcon(SystemServices.packageManager)
                isClipEnabled = true
                radius = 100
            }
            
            val label = taskDescription.label ?: activityInfo.loadLabel(SystemServices.packageManager)
            val debugLabel = if (BuildConfig.DEBUG) "(${snapshot.taskId}-$displayId) $label" else label
            
            var themeColors = ThemeColors()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ConfigManager.instance.config.coloredController) {
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
