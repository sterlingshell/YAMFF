package io.github.sterlingshell.yamff.common.model

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
    var useAppList: Boolean = true,
    var enableStatusAnimations: Boolean = true,
    var lastSeenActivatedBuildTime: Long = 0L,
    var version: Int = CURRENT_VERSION
) {
    fun validateAndFix(): Config {
        if (densityDpi < 72) densityDpi = 72
        if (densityDpi > 1000) densityDpi = 1000
        if (defaultWindowWidth < 100) defaultWindowWidth = 100
        if (defaultWindowHeight < 100) defaultWindowHeight = 100
        
        if ((surfaceView as Any?) == null) surfaceView = SurfaceType.TEXTURE
        if ((windowStyle as Any?) == null) windowStyle = WindowStyle.GESTURE
        if ((hookLauncher as Any?) == null) {
            hookLauncher = HookLauncher()
        } else {
            hookLauncher.validateAndFix()
        }
        
        // Fix other potential nulls if GSON was mean
        if ((flags as Any?) == null) flags = DEFAULT_FLAGS
        if ((useAppList as Any?) == null) useAppList = true
        if ((enableStatusAnimations as Any?) == null) enableStatusAnimations = true
        if ((lastSeenActivatedBuildTime as Any?) == null) lastSeenActivatedBuildTime = 0L
        
        return this
    }

    companion object {
        const val CURRENT_VERSION = 3
        
        const val STYLE_CLASSIC = 0
        const val STYLE_GESTURE = 1

        const val VIRTUAL_DISPLAY_FLAG_PUBLIC = 1 shl 0
        const val VIRTUAL_DISPLAY_FLAG_PRESENTATION = 1 shl 1
        const val VIRTUAL_DISPLAY_FLAG_SECURE = 1 shl 2
        const val VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 1 shl 3
        const val VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 1 shl 4
        const val VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 1 shl 5
        const val VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH = 1 shl 6
        const val VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT = 1 shl 7
        const val VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 shl 8
        const val VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 shl 9
        const val VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 shl 10
        const val VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 shl 11
        const val VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 shl 12
        const val VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED = 1 shl 13

        val ALL_FLAGS = listOf(
            "VIRTUAL_DISPLAY_FLAG_PUBLIC" to VIRTUAL_DISPLAY_FLAG_PUBLIC,
            "VIRTUAL_DISPLAY_FLAG_PRESENTATION" to VIRTUAL_DISPLAY_FLAG_PRESENTATION,
            "VIRTUAL_DISPLAY_FLAG_SECURE" to VIRTUAL_DISPLAY_FLAG_SECURE,
            "VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY" to VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            "VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR" to VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            "VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD" to VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD,
            "VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH" to VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH,
            "VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT" to VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT,
            "VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL" to VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL,
            "VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS" to VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS,
            "VIRTUAL_DISPLAY_FLAG_TRUSTED" to VIRTUAL_DISPLAY_FLAG_TRUSTED,
            "VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP" to VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP,
            "VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED" to VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED,
            "VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED" to VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED,
        )

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
    ) {
        fun validateAndFix(): HookLauncher {
            return this
        }

        fun copy(update: (HookLauncher) -> Unit): HookLauncher {
            val new = this.copy()
            update(new)
            return new
        }
    }
}
