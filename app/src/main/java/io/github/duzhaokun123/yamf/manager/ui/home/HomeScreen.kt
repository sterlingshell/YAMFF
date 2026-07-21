package io.github.duzhaokun123.yamf.manager.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.manager.ui.components.PreferenceCard
import io.github.duzhaokun123.yamf.manager.ui.components.SwitchPreference
import io.github.duzhaokun123.yamf.manager.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val openCount by viewModel.openCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_home)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(viewModel)

            PreferenceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.open_count),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = openCount.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = stringResource(R.string.home_quick_settings),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            PreferenceCard {
                SwitchPreference(
                    title = stringResource(R.string.pref_colored_controller),
                    checked = settingsViewModel.config.coloredController,
                    onCheckedChange = { settingsViewModel.updateColoredController(it) }
                )
                SwitchPreference(
                    title = stringResource(R.string.pref_back_home),
                    checked = settingsViewModel.config.recentsBackHome,
                    onCheckedChange = { settingsViewModel.updateRecentsBackHome(it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.createWindow() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.RocketLaunch, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.new_window))
                }
                Button(
                    onClick = { viewModel.openAppList() },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.open_app_list))
                }
            }

            PreferenceCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_recent_logs),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.home_log_window_reset),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Button(
                onClick = { viewModel.resetAllWindow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.reset_all_window))
            }
        }
    }
}

@Composable
fun StatusCard(viewModel: MainViewModel) {
    val activated = viewModel.buildTimeSystem == viewModel.buildTimeModule
    val notActivated = viewModel.buildTimeSystem == 0L
    val needReboot = !activated && !notActivated

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val containerColor = when {
        notActivated -> MaterialTheme.colorScheme.errorContainer
        needReboot -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    
    val contentColor = when {
        notActivated -> MaterialTheme.colorScheme.onErrorContainer
        needReboot -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .scale(if (activated) scale else 1f),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    notActivated -> Icons.Default.Error
                    needReboot -> Icons.Default.Warning
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = stringResource(
                        when {
                            notActivated -> R.string.not_activated
                            needReboot -> R.string.need_reboot
                            else -> R.string.activated
                        }
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (activated) {
                    Text(
                        text = "${viewModel.versionName} (${viewModel.versionCode})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (needReboot) {
                    Text(
                        text = "System: ${viewModel.versionName}\nModule: ${io.github.duzhaokun123.yamf.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
