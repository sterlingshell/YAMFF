package io.github.duzhaokun123.yamf.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import io.github.duzhaokun123.yamf.manager.ui.theme.YAMFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            YAMFTheme {
                YAMFAppRoot()
            }
        }
    }
}
