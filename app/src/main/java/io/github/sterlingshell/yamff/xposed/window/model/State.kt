package io.github.sterlingshell.yamff.xposed.window.model

import android.graphics.Rect
import android.graphics.drawable.Drawable

data class State(
    val displayId: Int = -1,
    val taskId: Int = -1,
    val appIcon: Drawable? = null,
    val appLabel: CharSequence? = null,
    
    // Window State
    val isMini: Boolean = false,
    val isCollapsed: Boolean = false,
    val isAutoHide: Boolean = false,
    val isAspectLocked: Boolean = true,
    val isFocused: Boolean = false,
    val isScaling: Boolean = false,
    val scaleFactor: Float = 1.0f,
    val contentReady: Boolean = false,

    // Geometry
    val windowRect: Rect = Rect(),
    val lastNormalRect: Rect? = null,
    val ghostIconVisible: Boolean = false,
    val ghostIconPos: Pair<Float, Float> = 0f to 0f, // Relative to window
    val bubbleWidth: Int = 0,
    val bubbleRadius: Float = 0f,
    val isDraggingBubble: Boolean = false,
    
    // Colors
    val themeColors: ThemeColors = ThemeColors()
)

data class ThemeColors(
    val appCardBackground: Int = 0,
    val statusBarColor: Int = 0,
    val navigationBarColor: Int = 0,
    val onStatusBarColor: Int = 0,
    val onNavigationBarColor: Int = 0
)
