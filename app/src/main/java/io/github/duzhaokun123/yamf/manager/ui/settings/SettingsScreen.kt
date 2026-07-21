package io.github.duzhaokun123.yamf.manager.ui.settings

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
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.common.model.SurfaceType
import io.github.duzhaokun123.yamf.common.model.WindowStyle
import io.github.duzhaokun123.yamf.manager.ui.components.EditTextDialog
import io.github.duzhaokun123.yamf.manager.ui.components.ListPreferenceDialog
import io.github.duzhaokun123.yamf.manager.ui.components.MultiChoiceDialog
import io.github.duzhaokun123.yamf.manager.ui.components.PreferenceCard
import io.github.duzhaokun123.yamf.manager.ui.components.PreferenceCategory
import io.github.duzhaokun123.yamf.manager.ui.components.PreferenceItem
import io.github.duzhaokun123.yamf.manager.ui.components.SwitchPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val config = viewModel.config
    var searchQuery by remember { mutableStateOf("") }
    
    var showDpiDialog by remember { mutableStateOf(false) }
    var showWidthDialog by remember { mutableStateOf(false) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var showWindowStyleDialog by remember { mutableStateOf(false) }
    var showFlagsDialog by remember { mutableStateOf(false) }
    var showWindowfyDialog by remember { mutableStateOf(false) }
    var showSurfaceDialog by remember { mutableStateOf(false) }

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
            if (shouldShow("Display", searchQuery)) {
                PreferenceCategory(title = stringResource(R.string.pref_display_title))
                PreferenceCard {
                    PreferenceItem(
                        title = stringResource(R.string.pref_window_style),
                        summary = stringResource(
                            if (config.windowStyle == WindowStyle.CLASSIC) R.string.pref_window_style_classic else R.string.pref_window_style_gesture
                        ),
                        onClick = { showWindowStyleDialog = true }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.pref_density_dpi),
                        summary = config.densityDpi.toString(),
                        onClick = { showDpiDialog = true }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.pref_default_window_size),
                        summary = "${config.defaultWindowWidth} x ${config.defaultWindowHeight}",
                        onClick = { showWidthDialog = true }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.pref_flags),
                        summary = config.flags.toString(),
                        onClick = { showFlagsDialog = true }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_colored_controller),
                        checked = config.coloredController,
                        onCheckedChange = { viewModel.updateColoredController(it) }
                    )
                }
            }

            // Behavior Settings
            if (shouldShow("Behavior", searchQuery)) {
                PreferenceCategory(title = stringResource(R.string.pref_behavior_title))
                PreferenceCard {
                    PreferenceItem(
                        title = stringResource(R.string.pref_windowfy),
                        summary = stringResource(
                            when (config.windowfy) {
                                0 -> R.string.pref_windowfy_0
                                1 -> R.string.pref_windowfy_1
                                else -> R.string.pref_windowfy_2
                            }
                        ),
                        onClick = { showWindowfyDialog = true }
                    )
                }
            }

            // Rendering Settings
            if (shouldShow("Rendering", searchQuery)) {
                PreferenceCategory(title = stringResource(R.string.pref_render_title))
                PreferenceCard {
                    PreferenceItem(
                        title = stringResource(R.string.pref_surface_view),
                        summary = stringResource(
                            if (config.surfaceView == SurfaceType.TEXTURE) R.string.pref_surface_view_0 else R.string.pref_surface_view_1
                        ),
                        onClick = { showSurfaceDialog = true }
                    )
                }
            }

            // Interaction Settings
            if (shouldShow("Interaction", searchQuery)) {
                PreferenceCategory(title = stringResource(R.string.pref_interaction_title))
                PreferenceCard {
                    SwitchPreference(
                        title = stringResource(R.string.pref_haptic_feedback),
                        checked = config.hapticFeedback,
                        onCheckedChange = { viewModel.updateHapticFeedback(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_back_home),
                        checked = config.recentsBackHome,
                        onCheckedChange = { viewModel.updateRecentsBackHome(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_exclude_system_gesture),
                        checked = config.excludeSystemGesture,
                        onCheckedChange = { viewModel.updateExcludeSystemGesture(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_enable_flick_away),
                        checked = config.enableFlickAway,
                        onCheckedChange = { viewModel.updateEnableFlickAway(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_show_ime),
                        summary = stringResource(R.string.pref_show_ime_desc),
                        checked = config.showImeInWindow,
                        onCheckedChange = { viewModel.updateShowImeInWindow(it) }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_force_show_ime),
                        summary = stringResource(R.string.pref_force_show_ime_desc),
                        checked = config.showForceShowIME,
                        onCheckedChange = { viewModel.updateShowForceShowIME(it) }
                    )
                }
            }

            // Hook Launcher Settings
            if (shouldShow("Hook", searchQuery)) {
                PreferenceCategory(title = stringResource(R.string.pref_hook_launcher_title))
                PreferenceCard {
                    SwitchPreference(
                        title = stringResource(R.string.pref_hook_recents),
                        checked = config.hookLauncher.hookRecents,
                        onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookRecents = value } }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_hook_taskbar),
                        checked = config.hookLauncher.hookTaskbar,
                        onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookTaskbar = value } }
                    )
                    SwitchPreference(
                        title = stringResource(R.string.pref_hook_popup),
                        checked = config.hookLauncher.hookPopup,
                        onCheckedChange = { value -> viewModel.updateHookLauncher { it.hookPopup = value } }
                    )
                    SwitchPreference(
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

            // Others
            if (shouldShow("Other", searchQuery)) {
                PreferenceCategory(title = stringResource(R.string.pref_other_title))
                PreferenceCard {
                    SwitchPreference(
                        title = stringResource(R.string.pref_use_app_list),
                        checked = viewModel.useAppList,
                        onCheckedChange = { viewModel.updateUseAppList(it) }
                    )
                }
            }
        }
    }

    // Dialogs with Preview logic
    if (showDpiDialog) {
        EditTextDialog(
            title = stringResource(R.string.pref_density_dpi),
            initialValue = config.densityDpi.toString(),
            onDismiss = { showDpiDialog = false },
            onConfirm = { it.toIntOrNull()?.let { viewModel.updateDensityDpi(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            showPreview = true,
            previewRatio = 1f // DPI doesn't change ratio, just scale, but we can show it
        )
    }

    if (showWidthDialog) {
        EditTextDialog(
            title = stringResource(R.string.dialog_width),
            initialValue = config.defaultWindowWidth.toString(),
            onDismiss = { showWidthDialog = false },
            onConfirm = { it.toIntOrNull()?.let { viewModel.updateDefaultWindowSize(it, config.defaultWindowHeight) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            showPreview = true,
            previewRatio = config.defaultWindowWidth.toFloat() / config.defaultWindowHeight.toFloat()
        )
    }

    if (showHeightDialog) {
        EditTextDialog(
            title = stringResource(R.string.dialog_height),
            initialValue = config.defaultWindowHeight.toString(),
            onDismiss = { showHeightDialog = false },
            onConfirm = { it.toIntOrNull()?.let { viewModel.updateDefaultWindowSize(config.defaultWindowWidth, it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            showPreview = true,
            previewRatio = config.defaultWindowWidth.toFloat() / config.defaultWindowHeight.toFloat()
        )
    }

    if (showFlagsDialog) {
        val flags = listOf(
            "VIRTUAL_DISPLAY_FLAG_PUBLIC",
            "VIRTUAL_DISPLAY_FLAG_PRESENTATION",
            "VIRTUAL_DISPLAY_FLAG_SECURE",
            "VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY",
            "VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR",
            "VIRTUAL_DISPLAY_FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD",
            "VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH",
            "VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT",
            "VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL",
            "VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS",
            "VIRTUAL_DISPLAY_FLAG_TRUSTED",
            "VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP",
            "VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED",
            "VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED",
        )
        val checkedStates = List(flags.size) { index ->
            (config.flags and (1 shl index)) != 0
        }
        MultiChoiceDialog(
            title = stringResource(R.string.pref_flags),
            options = flags,
            checkedStates = checkedStates,
            onDismiss = { showFlagsDialog = false },
            onConfirm = { states ->
                val newFlags = states.foldIndexed(0) { index, acc, checked ->
                    if (checked) acc or (1 shl index) else acc
                }
                viewModel.updateFlags(newFlags)
            }
        )
    }

    if (showWindowfyDialog) {
        ListPreferenceDialog(
            title = stringResource(R.string.pref_windowfy),
            options = listOf(
                stringResource(R.string.pref_windowfy_0),
                stringResource(R.string.pref_windowfy_1),
                stringResource(R.string.pref_windowfy_2)
            ),
            selectedIndex = config.windowfy,
            onDismiss = { showWindowfyDialog = false },
            onSelect = { viewModel.updateWindowfy(it) }
        )
    }

    if (showSurfaceDialog) {
        ListPreferenceDialog(
            title = stringResource(R.string.pref_surface_view),
            options = listOf(
                stringResource(R.string.pref_surface_view_0),
                stringResource(R.string.pref_surface_view_1)
            ),
            selectedIndex = config.surfaceView.value,
            onDismiss = { showSurfaceDialog = false },
            onSelect = { viewModel.updateSurfaceView(it) }
        )
    }

    if (showWindowStyleDialog) {
        ListPreferenceDialog(
            title = stringResource(R.string.pref_window_style),
            options = listOf(
                stringResource(R.string.pref_window_style_classic),
                stringResource(R.string.pref_window_style_gesture)
            ),
            selectedIndex = config.windowStyle.value,
            onDismiss = { showWindowStyleDialog = false },
            onSelect = { viewModel.updateWindowStyle(it) }
        )
    }
}

private fun shouldShow(category: String, query: String): Boolean {
    if (query.isEmpty()) return true
    return category.contains(query, ignoreCase = true)
}
