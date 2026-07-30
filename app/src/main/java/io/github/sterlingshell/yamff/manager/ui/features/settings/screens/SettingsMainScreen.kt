package io.github.sterlingshell.yamff.manager.ui.features.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sterlingshell.yamff.R
import io.github.sterlingshell.yamff.manager.ui.components.cards.PreferenceCard
import io.github.sterlingshell.yamff.manager.ui.components.items.PreferenceItem
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsRoute
import io.github.sterlingshell.yamff.manager.ui.features.settings.SettingsViewModel

@Composable
fun SettingsMainScreen(
    viewModel: SettingsViewModel,
    onNavigateToDetail: (SettingsRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        PreferenceCard {
            PreferenceItem(
                title = stringResource(R.string.pref_display_title),
                summary = stringResource(R.string.pref_appearance_summary),
                onClick = { onNavigateToDetail(SettingsRoute.Appearance) }
            )
            PreferenceItem(
                title = stringResource(R.string.pref_behavior_title),
                summary = stringResource(R.string.pref_behavior_summary),
                onClick = { onNavigateToDetail(SettingsRoute.Behavior) }
            )
            PreferenceItem(
                title = stringResource(R.string.pref_advanced_title),
                summary = stringResource(R.string.pref_advanced_summary),
                onClick = { onNavigateToDetail(SettingsRoute.Advanced) }
            )
        }
    }
}
