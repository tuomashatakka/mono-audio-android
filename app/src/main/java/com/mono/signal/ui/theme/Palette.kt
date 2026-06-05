package com.mono.signal.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mono.signal.model.AccentOption
import com.mono.signal.model.BackgroundOption
import com.mono.signal.model.ThemeConfig

/**
 * The live, theme-driven palette. Components read [LocalMonoPalette] instead of the static
 * [MonoColors] so the selected accent + background apply everywhere at once.
 */
data class MonoPalette(
    val accent: Color,
    val accentGlow: Color,
    val background: Color,
    val panel: Color,        // framed surfaces (graphic area, search field)
    val panelElevated: Color,// rows, mini-player
    val sweep: List<Color>,  // signature accent gradient (waveform / edges)
) {
    val sweepBrush: Brush get() = Brush.horizontalGradient(sweep)
}

val LocalMonoPalette = staticCompositionLocalOf {
    paletteFor(ThemeConfig())
}

fun paletteFor(config: ThemeConfig): MonoPalette {
    val accent = when (config.accent) {
        AccentOption.TURQUOISE -> MonoColors.Turquoise
        AccentOption.CRIMSON -> MonoColors.Crimson
        AccentOption.VIOLET -> MonoColors.Violet
        AccentOption.BLUE -> MonoColors.Info
        AccentOption.AMBER -> MonoColors.Amber
        AccentOption.LIME -> MonoColors.Lime
        AccentOption.ROSE -> MonoColors.Rose
    }
    val accentGlow = when (config.accent) {
        AccentOption.TURQUOISE -> MonoColors.TurquoiseGlow
        AccentOption.CRIMSON -> MonoColors.CrimsonGlow
        AccentOption.VIOLET -> MonoColors.Violet
        AccentOption.BLUE -> MonoColors.Info
        AccentOption.AMBER -> MonoColors.Amber
        AccentOption.LIME -> MonoColors.Lime
        AccentOption.ROSE -> MonoColors.Rose
    }
    // Signature sweep always reads accent → violet → a complementary, so the gradient stays rich.
    val sweep = when (config.accent) {
        AccentOption.TURQUOISE -> listOf(MonoColors.Turquoise, MonoColors.Violet, MonoColors.Crimson)
        AccentOption.CRIMSON -> listOf(MonoColors.Crimson, MonoColors.Violet, MonoColors.Turquoise)
        AccentOption.VIOLET -> listOf(MonoColors.Violet, MonoColors.Info, MonoColors.Turquoise)
        AccentOption.BLUE -> listOf(MonoColors.Info, MonoColors.Violet, MonoColors.Turquoise)
        AccentOption.AMBER -> listOf(MonoColors.Amber, MonoColors.Crimson, MonoColors.Violet)
        AccentOption.LIME -> listOf(MonoColors.Lime, MonoColors.Turquoise, MonoColors.Info)
        AccentOption.ROSE -> listOf(MonoColors.Rose, MonoColors.Crimson, MonoColors.Amber)
    }
    return when (config.background) {
        BackgroundOption.VOID -> MonoPalette(
            accent = accent,
            accentGlow = accentGlow,
            background = MonoColors.Void,
            panel = MonoColors.Ink,
            panelElevated = MonoColors.Onyx,
            sweep = sweep,
        )
        BackgroundOption.SLATE -> MonoPalette(
            accent = accent,
            accentGlow = accentGlow,
            background = MonoColors.Onyx,
            panel = MonoColors.Graphite,
            panelElevated = MonoColors.Slate,
            sweep = sweep,
        )
    }
}
