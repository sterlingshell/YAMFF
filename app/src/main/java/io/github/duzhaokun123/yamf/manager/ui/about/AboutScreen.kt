package io.github.duzhaokun123.yamf.manager.ui.about

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.duzhaokun123.yamf.BuildConfig
import io.github.duzhaokun123.yamf.R
import io.github.duzhaokun123.yamf.manager.ui.components.PreferenceCategory
import io.github.duzhaokun123.yamf.manager.ui.components.PreferenceItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_about)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageResource(R.mipmap.ic_launcher)
                    }
                },
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.size(32.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val buildTime = dateFormat.format(Date(BuildConfig.BUILD_TIME))
                    
                    Text(text = stringResource(R.string.about_build_time), style = MaterialTheme.typography.labelLarge)
                    Text(text = buildTime, style = MaterialTheme.typography.bodyLarge)
                    
                    Spacer(modifier = Modifier.size(16.dp))
                    
                    Text(text = stringResource(R.string.build_type), style = MaterialTheme.typography.labelLarge)
                    Text(text = BuildConfig.BUILD_TYPE, style = MaterialTheme.typography.bodyLarge)
                }
            }

            PreferenceCategory(title = stringResource(R.string.about_links))
            PreferenceItem(
                title = stringResource(R.string.about_github),
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/duzhaokun123/YAMF".toUri()))
                }
            )
            PreferenceItem(
                title = stringResource(R.string.about_telegram),
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/YAMF_channel".toUri()))
                }
            )
            PreferenceItem(
                title = stringResource(R.string.donate),
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://duzhaokun123.github.io/donate.html".toUri()))
                }
            )
        }
    }
}