package io.github.sterlingshell.yamff.manager.ui.extensions

import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.sterlingshell.yamff.R
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(viewModel: ExtensionsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val extensions by viewModel.extensions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_extensions)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(extensions) { extension ->
                ExtensionItem(
                    metadata = extension,
                    onToggleAuthorization = { viewModel.toggleAuthorization(extension.packageName, it) },
                    onOpenSettings = {
                        extension.settingsComponent?.let { componentName ->
                            val intent = Intent().apply {
                                component = ComponentName(extension.packageName, componentName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ExtensionItem(
    metadata: io.github.sterlingshell.yamff.xposed.sys.ExtensionMetadata,
    onToggleAuthorization: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = metadata.label, style = MaterialTheme.typography.titleMedium)
            Text(text = metadata.packageName, style = MaterialTheme.typography.bodySmall)
        }
        
        if (metadata.settingsComponent != null) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
        
        Switch(
            checked = metadata.isAuthorized,
            onCheckedChange = onToggleAuthorization
        )
    }
}
