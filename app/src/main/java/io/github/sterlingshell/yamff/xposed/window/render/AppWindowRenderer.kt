package io.github.sterlingshell.yamff.xposed.window.render

import android.graphics.Rect
import android.view.View
import io.github.sterlingshell.yamff.xposed.window.model.AppWindowAction
import io.github.sterlingshell.yamff.xposed.window.model.AppWindowState

interface AppWindowRenderer {
    /**
     * Called when the window state changes. The renderer should update its UI accordingly.
     */
    fun onStateChanged(state: AppWindowState)

    /**
     * Sets the handler for actions triggered by the UI.
     */
    fun setActionHandler(handler: (AppWindowAction) -> Unit)

    /**
     * Returns the root view of the window to be added to WindowManager.
     */
    fun getRootView(): View

    /**
     * Returns the View that should be used as the surface for VirtualDisplay.
     * This should be a TextureView or SurfaceView.
     */
    fun getSurfaceView(): View

    /**
     * Starts the opening animation.
     */
    fun startOpeningAnimation(startRect: Rect?)

    /**
     * Starts the closing animation.
     * @param onEnd Callback to be executed when the animation finishes.
     */
    fun startClosingAnimation(onEnd: () -> Unit)
}
