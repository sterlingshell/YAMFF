package io.github.sterlingshell.yamff.manager.ui.features.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.common.model.DpiMode
import io.github.sterlingshell.yamff.common.model.WindowStyle
import io.github.sterlingshell.yamff.manager.ui.components.cards.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.dialogs.EditTextDialog
import io.github.sterlingshell.yamff.manager.ui.components.dialogs.ListPreferenceDialog
import io.github.sterlingshell.yamff.manager.ui.components.dialogs.WindowSizeDialog
import io.github.sterlingshell.yamff.manager.ui.components.items.PreferenceItem
import io.github.sterlingshell.yamff.manager.ui.components.items.SwitchPreferenceItem
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel

private sealed class AppearanceDialogState {
    object DpiMode : AppearanceDialogState()
    object DpiValue : AppearanceDialogState()
    object AutoDpiWidth : AppearanceDialogState()
    object WindowSize : AppearanceDialogState()
    object WindowStyle : AppearanceDialogState()
}

@Composable
fun AppearanceScreen(viewModel: SettingsViewModel) {
    val config by viewModel.config.collectAsState()
    var dialogState by remember { mutableStateOf<AppearanceDialogState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.pref_display_title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        PreferenceCard {
            PreferenceItem(
                title = stringResource(R.string.pref_window_style),
                summary = stringResource(
                    if (config.windowStyle == WindowStyle.CLASSIC) R.string.pref_window_style_classic else R.string.pref_window_style_gesture
                ),
                onClick = { dialogState = AppearanceDialogState.WindowStyle }
            )
            PreferenceItem(
                title = stringResource(R.string.pref_dpi_mode),
                summary = stringResource(
                    if (config.dpiMode == DpiMode.AUTO) R.string.pref_dpi_mode_auto else R.string.pref_dpi_mode_fixed
                ),
                onClick = { dialogState = AppearanceDialogState.DpiMode }
            )
            if (config.dpiMode == DpiMode.AUTO) {
                PreferenceItem(
                    title = stringResource(R.string.pref_auto_dpi_target_width),
                    summary = config.autoDpiTargetWidth.toString(),
                    onClick = { dialogState = AppearanceDialogState.AutoDpiWidth }
                )
            } else {
                PreferenceItem(
                    title = stringResource(R.string.pref_density_dpi),
                    summary = config.densityDpi.toString(),
                    onClick = { dialogState = AppearanceDialogState.DpiValue }
                )
            }
            PreferenceItem(
                title = stringResource(R.string.pref_default_window_size),
                summary = "${config.defaultWindowWidth} x ${config.defaultWindowHeight}",
                onClick = { dialogState = AppearanceDialogState.WindowSize }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_colored_controller),
                checked = config.coloredController,
                onCheckedChange = { viewModel.updateColoredController(it) }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_status_animations),
                checked = config.enableStatusAnimations,
                onCheckedChange = { viewModel.updateEnableStatusAnimations(it) }
            )
        }
    }

    // Dialogs
    when (dialogState) {
        AppearanceDialogState.DpiMode -> {
            ListPreferenceDialog(
                title = stringResource(R.string.pref_dpi_mode),
                options = listOf(
                    stringResource(R.string.pref_dpi_mode_fixed),
                    stringResource(R.string.pref_dpi_mode_auto)
                ),
                selectedIndex = config.dpiMode.value,
                onDismiss = { dialogState = null },
                onSelect = { viewModel.updateDpiMode(it) }
            )
        }
        AppearanceDialogState.DpiValue -> {
            EditTextDialog(
                title = stringResource(R.string.pref_density_dpi),
                initialValue = config.densityDpi.toString(),
                onDismiss = { dialogState = null },
                onConfirm = { 
                    it.toIntOrNull()?.let { dpi -> viewModel.updateDensityDpi(dpi) }
                    dialogState = null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                showPreview = true,
                previewRatio = 1f
            )
        }
        AppearanceDialogState.AutoDpiWidth -> {
            EditTextDialog(
                title = stringResource(R.string.pref_auto_dpi_target_width),
                description = stringResource(R.string.pref_auto_dpi_target_width_desc),
                initialValue = config.autoDpiTargetWidth.toString(),
                onDismiss = { dialogState = null },
                onConfirm = { 
                    it.toIntOrNull()?.let { width -> viewModel.updateAutoDpiTargetWidth(width) }
                    dialogState = null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        AppearanceDialogState.WindowSize -> {
            WindowSizeDialog(
                initialWidth = config.defaultWindowWidth,
                initialHeight = config.defaultWindowHeight,
                onDismiss = { dialogState = null },
                onConfirm = { w, h ->
                    viewModel.updateDefaultWindowSize(w, h)
                    dialogState = null
                }
            )
        }
        AppearanceDialogState.WindowStyle -> {
            ListPreferenceDialog(
                title = stringResource(R.string.pref_window_style),
                options = listOf(
                    stringResource(R.string.pref_window_style_classic),
                    stringResource(R.string.pref_window_style_gesture)
                ),
                selectedIndex = config.windowStyle.value,
                onDismiss = { dialogState = null },
                onSelect = { viewModel.updateWindowStyle(it) }
            )
        }
        null -> {}
    }
}
