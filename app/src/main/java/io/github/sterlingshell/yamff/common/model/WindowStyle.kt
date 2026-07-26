package io.github.sterlingshell.yamff.common.model

enum class WindowStyle(val value: Int) {
    CLASSIC(0),
    GESTURE(1);

    companion object {
        fun fromInt(value: Int) = values().find { it.value == value } ?: GESTURE
    }
}
