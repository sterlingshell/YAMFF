package io.github.sterlingshell.yamff.manager.ui

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
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.manager.ui.about.About
import io.github.sterlingshell.yamff.manager.ui.components.LocalHapticEnabled
import io.github.sterlingshell.yamff.manager.ui.home.Home
import io.github.sterlingshell.yamff.manager.ui.settings.Settings
import io.github.sterlingshell.yamff.manager.ui.settings.ViewModel

enum class Destination(
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
fun AppRoot(settingsViewModel: ViewModel = viewModel()) {
    val navController = rememberNavController()
    var currentDestination by remember { mutableStateOf(Destination.Home) }

    CompositionLocalProvider(
        LocalHapticEnabled provides settingsViewModel.config.hapticFeedback,
        io.github.sterlingshell.yamff.manager.ui.components.LocalSettingsViewModel provides settingsViewModel
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                Destination.entries.forEach { destination ->
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
                startDestination = Destination.Home.route
            ) {
                composable(Destination.Home.route) { Home() }
                composable(Destination.Settings.route) { Settings() }
                composable(Destination.About.route) { About() }
            }
        }
    }
}
