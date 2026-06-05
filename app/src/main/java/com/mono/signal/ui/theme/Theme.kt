package com.mono.signal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MonoColorScheme = darkColorScheme(
    primary = MonoColors.Turquoise,
    onPrimary = MonoColors.Void,
    secondary = MonoColors.Violet,
    tertiary = MonoColors.Crimson,
    background = MonoColors.Void,
    onBackground = MonoColors.Fg1,
    surface = MonoColors.Onyx,
    onSurface = MonoColors.Fg1,
    surfaceVariant = MonoColors.Graphite,
    onSurfaceVariant = MonoColors.Fg3,
    error = MonoColors.Crimson,
    outline = MonoColors.BorderSoft,
)

@Composable
fun MonoTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // MONO Signal is dark-only by design.
    MaterialTheme(
        colorScheme = MonoColorScheme,
        typography = MonoTypography,
        content = content,
    )
}
