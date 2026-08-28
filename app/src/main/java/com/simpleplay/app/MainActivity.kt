package com.simpleplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleplay.app.data.ThemeMode
import com.simpleplay.app.ui.SimplePlayApp
import com.simpleplay.app.ui.theme.SimplePlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )

        setContent {
            val viewModel: MainViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SimplePlayTheme(
                accentTheme = preferences.accentTheme,
                customAccentArgb = preferences.customAccentArgb,
                darkTheme = darkTheme,
            ) {
                val navigationBarColor = MaterialTheme.colorScheme.background.toArgb()
                DisposableEffect(darkTheme, navigationBarColor) {
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = navigationBarColor
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                    onDispose { }
                }
                SimplePlayApp(viewModel = viewModel)
            }
        }
    }
}
