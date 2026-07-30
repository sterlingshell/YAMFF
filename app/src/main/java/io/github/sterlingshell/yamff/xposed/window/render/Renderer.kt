package io.github.sterlingshell.yamff.xposed.window.render

import android.graphics.Rect
import android.view.View
import io.github.sterlingshell.yamff.xposed.window.model.Action
import io.github.sterlingshell.yamff.xposed.window.model.State

interface Renderer {
    fun onStateChanged(state: State)
    fun setActionHandler(handler: (Action) -> Unit)
    fun getRootView(): View
    fun getSurfaceView(): View
    fun startOpeningAnimation(startRect: Rect?)
    fun startClosingAnimation(onEnd: () -> Unit)
    fun decorateSnapshot(appContent: android.graphics.Bitmap): android.graphics.Bitmap
}
