package io.github.duzhaokun123.yamf.manager.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.manager.ui.about.AboutScreen
import io.github.duzhaokun123.yamf.manager.ui.components.LocalHapticEnabled
import io.github.duzhaokun123.yamf.manager.ui.home.HomeScreen
import io.github.duzhaokun123.yamf.manager.ui.settings.SettingsScreen
import io.github.duzhaokun123.yamf.manager.ui.settings.SettingsViewModel

enum class YAMFDestination(
    val route: String,
    val icon: ImageVector
) {
    Home("home", Icons.Default.Home),
    Settings("settings", Icons.Default.Settings),
    About("about", Icons.Default.Info);

    val labelRes: Int
        get() = when (this) {
            Home -> R.string.nav_home
            Settings -> R.string.nav_settings
            About -> R.string.nav_about
        }
}

@Composable
fun YAMFAppRoot(settingsViewModel: SettingsViewModel = viewModel()) {
    val navController = rememberNavController()
    var currentDestination by remember { mutableStateOf(YAMFDestination.Home) }

    CompositionLocalProvider(LocalHapticEnabled provides settingsViewModel.config.hapticFeedback) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                YAMFDestination.entries.forEach { destination ->
                    item(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = currentDestination == destination,
                        onClick = {
                            currentDestination = destination
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = YAMFDestination.Home.route
            ) {
                composable(YAMFDestination.Home.route) { HomeScreen() }
                composable(YAMFDestination.Settings.route) { SettingsScreen() }
                composable(YAMFDestination.About.route) { AboutScreen() }
            }
        }
    }
}
