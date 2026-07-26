package io.github.sterlingshell.yamff.xposed.window.model

import android.view.MotionEvent

sealed class AppWindowAction {
    // Basic Controls
    object Close : AppWindowAction()
    object Minimize : AppWindowAction()
    object Expand : AppWindowAction()
    object Fullscreen : AppWindowAction()
    object Rotate : AppWindowAction()
    object ToggleAspectLock : AppWindowAction()
    object ShowIme : AppWindowAction()
    object Back : AppWindowAction()
    object Home : AppWindowAction()
    object OpenAppList : AppWindowAction()
    
    // Gestures & Movement
    data class Move(val dx: Float, val dy: Float) : AppWindowAction()
    data class Fling(val velocityX: Float, val velocityY: Float) : AppWindowAction()
    data class UpdateDimensions(val width: Int, val height: Int) : AppWindowAction()
    data class FlickAwayBubble(val velocityX: Float) : AppWindowAction()
    object SnapToEdge : AppWindowAction()
    object StartBubbleDrag : AppWindowAction()
    object EndBubbleDrag : AppWindowAction()
    
    // Interaction
    data class SurfaceTouch(val event: MotionEvent) : AppWindowAction()
    data class SurfaceGenericMotion(val event: MotionEvent) : AppWindowAction()
    object RequestMoveToTop : AppWindowAction()
    object TapBubble : AppWindowAction()
    object LongPressBubble : AppWindowAction()
    object ResetWindow : AppWindowAction()
    object ToggleMini : AppWindowAction()
    object WakeUpBubble : AppWindowAction()
    data class SetScaling(val scaling: Boolean) : AppWindowAction()
    
    // Animation Callbacks
    object OpeningAnimationEnd : AppWindowAction()
}
