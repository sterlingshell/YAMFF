package io.github.sterlingshell.yamff.manager.ui.features.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.AdvancedScreen
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.AppearanceScreen
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.SettingsMainScreen
import io.github.sterlingshell.yamff.manager.ui.features.settings.screens.WindowBehaviorScreen

sealed class SettingsRoute(val route: String) {
    object Main : SettingsRoute("settings/main")
    object Appearance : SettingsRoute("settings/appearance")
    object Behavior : SettingsRoute("settings/behavior")
    object Advanced : SettingsRoute("settings/advanced")
}

@Composable
fun SettingsNavGraph(
    navController: NavHostController,
    viewModel: SettingsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route
    ) {
        composable(SettingsRoute.Main.route) {
            SettingsMainScreen(viewModel, navController)
        }
        composable(SettingsRoute.Appearance.route) {
            AppearanceScreen(viewModel)
        }
        composable(SettingsRoute.Behavior.route) {
            WindowBehaviorScreen(viewModel)
        }
        composable(SettingsRoute.Advanced.route) {
            AdvancedScreen(viewModel)
        }
    }
}
