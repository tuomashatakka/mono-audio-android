package com.mono.signal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.mono.signal.model.ThemeConfig

/**
 * MONO Signal is dark-only by design. The selected [ThemeConfig] drives both the Material
 * color scheme and the richer [LocalMonoPalette] that bespoke components read.
 */
@Composable
fun MonoTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val palette = remember(config) { paletteFor(config) }

    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = MonoColors.Void,
        secondary = MonoColors.Violet,
        tertiary = MonoColors.Crimson,
        background = palette.background,
        onBackground = MonoColors.Fg1,
        surface = palette.panel,
        onSurface = MonoColors.Fg1,
        surfaceVariant = palette.panelElevated,
        onSurfaceVariant = MonoColors.Fg3,
        error = MonoColors.Crimson,
        outline = MonoColors.BorderSoft,
    )

    CompositionLocalProvider(LocalMonoPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MonoTypography,
            shapes = MonoShapes,
            content = content,
        )
    }
}
