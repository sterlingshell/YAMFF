package io.github.sterlingshell.yamff.xposed.window.model

import android.view.MotionEvent

sealed class Action {
    // Basic Controls
    object Close : Action()
    object Minimize : Action()
    object Expand : Action()
    object Fullscreen : Action()
    object Rotate : Action()
    object ToggleAspectLock : Action()
    object ShowIme : Action()
    object Back : Action()
    object Home : Action()
    object OpenAppList : Action()
    
    // Gestures & Movement
    data class Move(val dx: Float, val dy: Float) : Action()
    data class Fling(val velocityX: Float, val velocityY: Float) : Action()
    data class UpdateDimensions(val width: Int, val height: Int) : Action()
    data class FlickAwayBubble(val velocityX: Float) : Action()
    object SnapToEdge : Action()
    object StartBubbleDrag : Action()
    object EndBubbleDrag : Action()
    
    // Interaction
    data class SurfaceTouch(val event: MotionEvent) : Action()
    data class SurfaceGenericMotion(val event: MotionEvent) : Action()
    object RequestMoveToTop : Action()
    object TapBubble : Action()
    object LongPressBubble : Action()
    object ResetWindow : Action()
    object ToggleMini : Action()
    object WakeUpBubble : Action()
    data class SetScaling(val scaling: Boolean) : Action()
    
    // Animation Callbacks
    object OpeningAnimationEnd : Action()
}
