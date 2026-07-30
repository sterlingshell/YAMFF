package io.github.sterlingshell.yamff.manager.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.AdvancedScreen
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.AppearanceScreen
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.SettingsMainScreen
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.WindowBehaviorScreen
import org.koin.compose.viewmodel.koinViewModel

enum class SettingsRoute {
    Main, Appearance, Behavior, Advanced
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                SettingsMainScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { route ->
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, route)
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val content = navigator.currentDestination?.content
                if (content != null) {
                    Scaffold(
                        topBar = {
                            if (navigator.canNavigateBack()) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            when (content) {
                                                SettingsRoute.Appearance -> stringResource(R.string.pref_display_title)
                                                SettingsRoute.Behavior -> stringResource(R.string.pref_behavior_title)
                                                SettingsRoute.Advanced -> stringResource(R.string.pref_advanced_title)
                                                else -> ""
                                            }
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { navigator.navigateBack() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                        }
                                    }
                                )
                            }
                        }
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            when (content) {
                                SettingsRoute.Appearance -> AppearanceScreen(viewModel)
                                SettingsRoute.Behavior -> WindowBehaviorScreen(viewModel)
                                SettingsRoute.Advanced -> AdvancedScreen(viewModel)
                                else -> {}
                            }
                        }
                    }
                } else {
                    // Empty state for detail pane on large screens
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.pref_select_category_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}
