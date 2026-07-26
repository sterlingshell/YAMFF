package io.github.sterlingshell.yamff.manager.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.common.model.Config
import io.github.sterlingshell.yamff.common.model.SurfaceType
import io.github.sterlingshell.yamff.common.model.WindowStyle
import io.github.sterlingshell.yamff.manager.ui.components.EditTextDialog
import io.github.sterlingshell.yamff.manager.ui.components.ListPreferenceDialog
import io.github.sterlingshell.yamff.manager.ui.components.MultiChoiceDialog
import io.github.sterlingshell.yamff.manager.ui.components.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.PreferenceCategory
import io.github.sterlingshell.yamff.manager.ui.components.PreferenceItem
import io.github.sterlingshell.yamff.manager.ui.components.SwitchPreference

sealed class DialogState {
    object Dpi : DialogState()
    object WindowSize : DialogState()
    object WindowStyle : DialogState()
    object Flags : DialogState()
    object Windowfy : DialogState()
    object Surface : DialogState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val config = viewModel.config
    var searchQuery by remember { mutableStateOf("") }
    var dialogState by remember { mutableStateOf<DialogState?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.nav_settings)) })
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.pref_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Display Settings
            SettingsSection(
                title = stringResource(R.string.pref_display_title)
            ) {
                PreferenceCard {
                    SearchablePreferenceItem(
                        title = stringResource(R.string.pref_window_style),
                        summary = stringResource(
                            if (config.windowStyle == WindowStyle.CLASSIC) R.string.pref_window_style_classic else R.string.pref_window_style_gesture
                        ),
                        query = searchQuery,
                        onClick = { dialogState = DialogState.WindowStyle }
                    )
                    SearchablePreferenceItem(
                        title = stringResource(R.string.pref_density_dpi),
                        summary = config.densityDpi.toString(),
                        query = searchQuery,
                        onClick = { dialogState = DialogState.Dpi }
                    )
                    SearchablePreferenceItem(
                        title = stringResource(R.string.pref_default_window_size),
                        summary = "${config.defaultWindowWidth} x ${config.defaultWindowHeight}",
                        query = searchQuery,
                        onClick = { dialogState = DialogState.WindowSize }
                    )
                    SearchablePreferenceItem(
                        title = stringResource(R.string.pref_flags),
                        summary = config.flags.toString(),
                        query = searchQuery,
                        onClick = { dialogState = DialogState.Flags }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_colored_controller),
                        checked = config.coloredController,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateColoredController(it) }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_status_animations),
                        checked = config.enableStatusAnimations,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateEnableStatusAnimations(it) }
                    )
                }
            }

            // Behavior Settings
            SettingsSection(
                title = stringResource(R.string.pref_behavior_title)
            ) {
                PreferenceCard {
                    SearchablePreferenceItem(
                        title = stringResource(R.string.pref_windowfy),
                        summary = stringResource(
                            when (config.windowfy) {
                                0 -> R.string.pref_windowfy_0
                                1 -> R.string.pref_windowfy_1
                                else -> R.string.pref_windowfy_2
                            }
                        ),
                        query = searchQuery,
                        onClick = { dialogState = DialogState.Windowfy }
                    )
                }
            }

            // Rendering Settings
            SettingsSection(
                title = stringResource(R.string.pref_render_title)
            ) {
                PreferenceCard {
                    SearchablePreferenceItem(
                        title = stringResource(R.string.pref_surface_view),
                        summary = stringResource(
                            if (config.surfaceView == SurfaceType.TEXTURE) R.string.pref_surface_view_0 else R.string.pref_surface_view_1
                        ),
                        query = searchQuery,
                        onClick = { dialogState = DialogState.Surface }
                    )
                }
            }

            // Interaction Settings
            SettingsSection(
                title = stringResource(R.string.pref_interaction_title)
            ) {
                PreferenceCard {
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_haptic_feedback),
                        checked = config.hapticFeedback,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateHapticFeedback(it) }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_back_home),
                        checked = config.recentsBackHome,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateRecentsBackHome(it) }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_exclude_system_gesture),
                        checked = config.excludeSystemGesture,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateExcludeSystemGesture(it) }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_enable_flick_away),
                        checked = config.enableFlickAway,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateEnableFlickAway(it) }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_show_ime),
                        summary = stringResource(R.string.pref_show_ime_desc),
                        checked = config.showImeInWindow,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateShowImeInWindow(it) }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_force_show_ime),
                        summary = stringResource(R.string.pref_force_show_ime_desc),
                        checked = config.showForceShowIME,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateShowForceShowIME(it) }
                    )
                }
            }

            // Hook Launcher Settings
            SettingsSection(
                title = stringResource(R.string.pref_hook_launcher_title)
            ) {
                PreferenceCard {
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_hook_recents),
                        checked = config.hookLauncher.hookRecents,
                        query = searchQuery,
                        onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookRecents = value } }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_hook_taskbar),
                        checked = config.hookLauncher.hookTaskbar,
                        query = searchQuery,
                        onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookTaskbar = value } }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_hook_popup),
                        checked = config.hookLauncher.hookPopup,
                        query = searchQuery,
                        onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookPopup = value } }
                    )
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_hook_transient_taskbar),
                        checked = config.hookLauncher.hookTransientTaskbar,
                        query = searchQuery,
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

            // Others
            SettingsSection(
                title = stringResource(R.string.pref_other_title)
            ) {
                PreferenceCard {
                    SearchableSwitchPreference(
                        title = stringResource(R.string.pref_use_app_list),
                        checked = viewModel.useAppList,
                        query = searchQuery,
                        onCheckedChange = { viewModel.updateUseAppList(it) }
                    )
                }
            }
        }
    }

    // Dialogs
    when (dialogState) {
        DialogState.Dpi -> {
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
        DialogState.WindowSize -> {
            io.github.sterlingshell.yamff.manager.ui.components.WindowSizeDialog(
                initialWidth = config.defaultWindowWidth,
                initialHeight = config.defaultWindowHeight,
                onDismiss = { dialogState = null },
                onConfirm = { w, h ->
                    viewModel.updateDefaultWindowSize(w, h)
                    dialogState = null
                }
            )
        }
        DialogState.Flags -> {
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
        DialogState.Windowfy -> {
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
        DialogState.Surface -> {
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
        DialogState.WindowStyle -> {
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

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    PreferenceCategory(title = title)
    content()
}

@Composable
fun SearchablePreferenceItem(
    title: String,
    summary: String? = null,
    query: String,
    onClick: () -> Unit
) {
    if (shouldShowItem(title, summary, query)) {
        PreferenceItem(title = title, summary = summary, onClick = onClick)
    }
}

@Composable
fun SearchableSwitchPreference(
    title: String,
    summary: String? = null,
    checked: Boolean,
    query: String,
    onCheckedChange: (Boolean) -> Unit
) {
    if (shouldShowItem(title, summary, query)) {
        SwitchPreference(title = title, summary = summary, checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun shouldShowItem(title: String, summary: String?, query: String): Boolean {
    if (query.isEmpty()) return true
    if (title.contains(query, ignoreCase = true)) return true
    if (summary?.contains(query, ignoreCase = true) == true) return true
    return false
}
