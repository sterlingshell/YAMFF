package io.github.sterlingshell.yamff.common.model

enum class SnapshotBackground {
    /**
     * Blur the empty space in the snapshot.
     */
    BLUR,
    /**
     * Make the empty space in the snapshot transparent.
     */
    TRANSPARENT,
    /**
     * Use a solid color (usually secondary background) for empty space.
     */
    SOLID_COLOR
}
