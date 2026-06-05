package com.mono.signal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mono.signal.data.ThemePreferences
import com.mono.signal.ui.nav.AppNav
import com.mono.signal.ui.theme.MonoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by themePreferences.config.collectAsStateWithLifecycle()
            MonoTheme(config = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize().background(MaterialThemeBackground()),
                    color = MaterialThemeBackground(),
                ) {
                    AppNav()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MaterialThemeBackground() = androidx.compose.material3.MaterialTheme.colorScheme.background
