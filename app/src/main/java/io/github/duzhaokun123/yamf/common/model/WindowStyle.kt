package io.github.duzhaokun123.yamf.common.model

enum class WindowStyle(val value: Int) {
    CLASSIC(0),
    GESTURE(1);

    companion object {
        fun fromInt(value: Int) = values().find { it.value == value } ?: GESTURE
    }
}
