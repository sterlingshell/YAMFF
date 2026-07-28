package io.github.sterlingshell.yamff.common.model

data class ExtensionInfo(
    val packageName: String,
    val authorized: Boolean = false,
    val label: String? = null
)

data class ExtensionConfig(
    val authorizedPackages: MutableSet<String> = mutableSetOf()
)
