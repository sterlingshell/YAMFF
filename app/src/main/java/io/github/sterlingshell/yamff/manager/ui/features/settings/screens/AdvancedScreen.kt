package io.github.sterlingshell.yamff.manager.ui.features.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.manager.ui.components.cards.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.dialogs.ListPreferenceDialog
import io.github.sterlingshell.yamff.manager.ui.components.dialogs.MultiChoiceDialog
import io.github.sterlingshell.yamff.manager.ui.components.items.PreferenceItem
import io.github.sterlingshell.yamff.manager.ui.components.items.SwitchPreferenceItem
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel

private sealed class AdvancedDialogState {
    object Flags : AdvancedDialogState()
    object Surface : AdvancedDialogState()
}

@Composable
fun AdvancedScreen(viewModel: SettingsViewModel) {
    val config by viewModel.config.collectAsState()
    var dialogState by remember { mutableStateOf<AdvancedDialogState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.pref_advanced_title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        PreferenceCard {
            PreferenceItem(
                title = stringResource(R.string.pref_surface_view),
                summary = stringResource(
                    if (config.surfaceView == SurfaceType.TEXTURE) R.string.pref_surface_view_0 else R.string.pref_surface_view_1
                ),
                onClick = { dialogState = AdvancedDialogState.Surface }
            )
            PreferenceItem(
                title = stringResource(R.string.pref_flags),
                summary = config.flags.toString(),
                onClick = { dialogState = AdvancedDialogState.Flags }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_use_app_list),
                checked = config.useAppList,
                onCheckedChange = { viewModel.updateUseAppList(it) }
            )
        }

        PreferenceCard {
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_hook_recents),
                checked = config.hookLauncher.hookRecents,
                onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookRecents = value } }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_hook_taskbar),
                checked = config.hookLauncher.hookTaskbar,
                onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookTaskbar = value } }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_hook_popup),
                checked = config.hookLauncher.hookPopup,
                onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookPopup = value } }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_hook_transient_taskbar),
                checked = config.hookLauncher.hookTransientTaskbar,
                onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookTransientTaskbar = value } }
            )
        }
        Text(
            text = stringResource(R.string.pref_hook_launcher_restart_desc),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    when (dialogState) {
        AdvancedDialogState.Flags -> {
            val options = Config.ALL_FLAGS.map { it.first }
            val checkedStates = Config.ALL_FLAGS.map { (_, bit) ->
                (config.flags and bit) != 0
            }
            MultiChoiceDialog(
                title = stringResource(R.string.pref_flags),
                options = options,
                checkedStates = checkedStates,
                onDismiss = { dialogState = null },
                onConfirm = { states ->
                    val newFlags = states.foldIndexed(0) { index, acc, checked ->
                        if (checked) acc or Config.ALL_FLAGS[index].second else acc
                    }
                    viewModel.updateFlags(newFlags)
                }
            )
        }
        AdvancedDialogState.Surface -> {
            ListPreferenceDialog(
                title = stringResource(R.string.pref_surface_view),
                options = listOf(
                    stringResource(R.string.pref_surface_view_0),
                    stringResource(R.string.pref_surface_view_1)
                ),
                selectedIndex = config.surfaceView.value,
                onDismiss = { dialogState = null },
                onSelect = { viewModel.updateSurfaceView(it) }
            )
        }
        null -> {}
    }
}
