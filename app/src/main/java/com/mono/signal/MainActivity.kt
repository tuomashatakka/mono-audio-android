package com.mono.signal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mono.signal.ui.nav.AppNav
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MonoTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MonoColors.Void),
                    color = MonoColors.Void,
                ) {
                    AppNav()
                }
            }
        }
    }
}
