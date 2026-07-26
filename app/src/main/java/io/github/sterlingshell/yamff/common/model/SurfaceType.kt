package io.github.sterlingshell.yamff.common.model

enum class SurfaceType(val value: Int) {
    TEXTURE(0),
    SURFACE(1);

    companion object {
        fun fromInt(value: Int) = values().find { it.value == value } ?: TEXTURE
    }
}
