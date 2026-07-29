package io.github.sterlingshell.yamff.manager.ui.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import io.github.sterlingshell.yamff.R
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(SettingsRoute.Main.route) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            currentRoute = entry.destination.route ?: SettingsRoute.Main.route
        }
    }

    Scaffold(
        topBar = {
            if (currentRoute != SettingsRoute.Main.route) {
                TopAppBar(
                    title = {
                        val title = when (currentRoute) {
                            SettingsRoute.Appearance.route -> stringResource(R.string.pref_display_title)
                            SettingsRoute.Behavior.route -> "Window Behavior"
                            SettingsRoute.Advanced.route -> "Advanced Settings"
                            else -> stringResource(R.string.nav_settings)
                        }
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SettingsNavGraph(navController, viewModel)
        }
    }
}
