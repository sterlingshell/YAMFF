package io.github.duzhaokun123.yamf.common.model

data class Config(
    var densityDpi: Int = 200,
    var flags: Int = DEFAULT_FLAGS,
    var coloredController: Boolean = false,
    /*
     * 0: move task only
     * 1: start activity only
     * 2: move task, failback to start activity
     */
    var windowfy: Int = 0,
    /*
     * 0: TextureView
     * 1: SurfaceView
     */
    var surfaceView: SurfaceType = SurfaceType.TEXTURE,
    var recentsBackHome: Boolean = false,
    var showImeInWindow: Boolean = false,
    var defaultWindowWidth: Int = 200,
    var defaultWindowHeight: Int = 300,
    var hookLauncher: HookLauncher = HookLauncher(),
    var showForceShowIME: Boolean = false,
    var hapticFeedback: Boolean = true,
    var excludeSystemGesture: Boolean = true,
    var enableFlickAway: Boolean = true,
    var windowStyle: WindowStyle = WindowStyle.GESTURE,
) {
    companion object {
        const val STYLE_CLASSIC = 0
        const val STYLE_GESTURE = 1

        const val VIRTUAL_DISPLAY_FLAG_PUBLIC = 1 shl 0
        const val VIRTUAL_DISPLAY_FLAG_PRESENTATION = 1 shl 1
        const val VIRTUAL_DISPLAY_FLAG_SECURE = 1 shl 2
        const val VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 1 shl 3
        const val VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 1 shl 4
        const val VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 1 shl 5
        const val VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT = 1 shl 7
        const val VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 shl 8
        const val VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 shl 9
        const val VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 shl 10

        const val DEFAULT_FLAGS = VIRTUAL_DISPLAY_FLAG_SECURE or
                VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT or
                VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS or
                VIRTUAL_DISPLAY_FLAG_TRUSTED
    }

    data class HookLauncher(
        var hookRecents: Boolean = true,
        var hookTaskbar: Boolean = true,
        var hookPopup: Boolean = true,
        var hookTransientTaskbar: Boolean = false,
    )
}