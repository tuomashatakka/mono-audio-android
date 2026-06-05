package com.mono.audio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Void = Color(0xFF050507)
val Panel = Color(0xFF111118)
val Ink = Color(0xFFE9E7F3)
val Muted = Color(0xFF8A879E)
val Cyan = Color(0xFF16D8CF)
val Magenta = Color(0xFFFF2C87)
val Violet = Color(0xFFB8A7F2)

private val MonoColors = darkColorScheme(
    background = Void,
    surface = Panel,
    primary = Cyan,
    secondary = Magenta,
    tertiary = Violet,
    onBackground = Ink,
    onSurface = Ink,
    onPrimary = Void,
)

@Composable
fun MonoAudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MonoColors, content = content)
}
