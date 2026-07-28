package io.github.sterlingshell.yamff.common.model

enum class DpiMode(val value: Int) {
    FIXED(0),
    AUTO(1);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: FIXED
    }
}
