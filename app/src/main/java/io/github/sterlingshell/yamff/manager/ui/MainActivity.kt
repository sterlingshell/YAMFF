package io.github.sterlingshell.yamff.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import io.github.sterlingshell.yamff.manager.ui.theme.YAMFFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            YAMFFTheme {
                YAMFFAppRoot()
            }
        }
    }
}
