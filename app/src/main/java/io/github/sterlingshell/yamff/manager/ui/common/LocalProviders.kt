package io.github.sterlingshell.yamff.manager.ui.common

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel

val LocalHapticEnabled = staticCompositionLocalOf { true }
val LocalSettingsViewModel = staticCompositionLocalOf<SettingsViewModel> { error("No SettingsViewModel provided") }
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> { error("No WindowSizeClass provided") }
