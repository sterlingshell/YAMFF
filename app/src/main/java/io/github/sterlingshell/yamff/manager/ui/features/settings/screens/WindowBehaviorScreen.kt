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
import io.github.sterlingshell.yamff.common.model.RecentTaskMode
import io.github.sterlingshell.yamff.common.model.SnapshotBackground
import io.github.sterlingshell.yamff.manager.ui.components.cards.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.dialogs.ListPreferenceDialog
import io.github.sterlingshell.yamff.manager.ui.components.items.PreferenceItem
import io.github.sterlingshell.yamff.manager.ui.components.items.SwitchPreferenceItem
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel

private sealed class BehaviorDialogState {
    object Windowfy : BehaviorDialogState()
    object RecentTaskMode : BehaviorDialogState()
    object SnapshotBackground : BehaviorDialogState()
}

@Composable
fun WindowBehaviorScreen(viewModel: SettingsViewModel) {
    val config by viewModel.config.collectAsState()
    var dialogState by remember { mutableStateOf<BehaviorDialogState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.pref_behavior_title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        PreferenceCard {
            PreferenceItem(
                title = stringResource(R.string.pref_recent_task_mode),
                summary = stringResource(
                    when (config.recentTaskMode) {
                        RecentTaskMode.NATIVE -> R.string.pref_recent_task_mode_none
                        RecentTaskMode.HIDDEN -> R.string.pref_recent_task_mode_hide
                        RecentTaskMode.DECORATED -> R.string.pref_recent_task_mode_optimize
                    }
                ),
                onClick = { dialogState = BehaviorDialogState.RecentTaskMode }
            )
            if (config.recentTaskMode == RecentTaskMode.DECORATED) {
                PreferenceItem(
                    title = stringResource(R.string.pref_snapshot_background),
                    summary = stringResource(
                        when (config.snapshotBackground) {
                            SnapshotBackground.BLUR -> R.string.pref_snapshot_background_blur
                            SnapshotBackground.TRANSPARENT -> R.string.pref_snapshot_background_transparent
                            SnapshotBackground.SOLID_COLOR -> R.string.pref_snapshot_background_solid
                        }
                    ),
                    onClick = { dialogState = BehaviorDialogState.SnapshotBackground }
                )
            }
            PreferenceItem(
                title = stringResource(R.string.pref_windowfy),
                summary = stringResource(
                    when (config.windowfy) {
                        0 -> R.string.pref_windowfy_0
                        1 -> R.string.pref_windowfy_1
                        else -> R.string.pref_windowfy_2
                    }
                ),
                onClick = { dialogState = BehaviorDialogState.Windowfy }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_haptic_feedback),
                checked = config.hapticFeedback,
                onCheckedChange = { viewModel.updateHapticFeedback(it) }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_back_home),
                checked = config.recentsBackHome,
                onCheckedChange = { viewModel.updateRecentsBackHome(it) }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_exclude_system_gesture),
                checked = config.excludeSystemGesture,
                onCheckedChange = { viewModel.updateExcludeSystemGesture(it) }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_enable_flick_away),
                checked = config.enableFlickAway,
                onCheckedChange = { viewModel.updateEnableFlickAway(it) }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_show_ime),
                summary = stringResource(R.string.pref_show_ime_desc),
                checked = config.showImeInWindow,
                onCheckedChange = { viewModel.updateShowImeInWindow(it) }
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.pref_force_show_ime),
                summary = stringResource(R.string.pref_force_show_ime_desc),
                checked = config.showForceShowIME,
                onCheckedChange = { viewModel.updateShowForceShowIME(it) }
            )
        }
    }

    when (dialogState) {
        BehaviorDialogState.Windowfy -> {
            ListPreferenceDialog(
                title = stringResource(R.string.pref_windowfy),
                options = listOf(
                    stringResource(R.string.pref_windowfy_0),
                    stringResource(R.string.pref_windowfy_1),
                    stringResource(R.string.pref_windowfy_2)
                ),
                selectedIndex = config.windowfy,
                onDismiss = { dialogState = null },
                onSelect = { viewModel.updateWindowfy(it) }
            )
        }
        BehaviorDialogState.RecentTaskMode -> {
            ListPreferenceDialog(
                title = stringResource(R.string.pref_recent_task_mode),
                options = listOf(
                    stringResource(R.string.pref_recent_task_mode_none),
                    stringResource(R.string.pref_recent_task_mode_hide),
                    stringResource(R.string.pref_recent_task_mode_optimize)
                ),
                selectedIndex = config.recentTaskMode.ordinal,
                onDismiss = { dialogState = null },
                onSelect = { viewModel.updateRecentTaskMode(it) }
            )
        }
        BehaviorDialogState.SnapshotBackground -> {
            ListPreferenceDialog(
                title = stringResource(R.string.pref_snapshot_background),
                options = listOf(
                    stringResource(R.string.pref_snapshot_background_blur),
                    stringResource(R.string.pref_snapshot_background_transparent),
                    stringResource(R.string.pref_snapshot_background_solid)
                ),
                selectedIndex = config.snapshotBackground.ordinal,
                onDismiss = { dialogState = null },
                onSelect = { viewModel.updateSnapshotBackground(it) }
            )
        }
        null -> {}
    }
}
