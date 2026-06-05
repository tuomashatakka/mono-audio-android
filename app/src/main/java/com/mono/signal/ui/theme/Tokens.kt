package com.mono.signal.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Reusable gradient brushes + spacing, matching the mockup's `--grad-*` and `--sp-*`. */
object MonoTokens {

    /** turquoise → violet → crimson, horizontal. The signature accent sweep. */
    val gradPrimary = Brush.horizontalGradient(
        listOf(MonoColors.Turquoise, MonoColors.Violet, MonoColors.Crimson),
    )

    /** transparent → turquoise → crimson → transparent — the neon edge highlight. */
    val gradEdge = Brush.horizontalGradient(
        0.0f to Color.Transparent,
        0.25f to MonoColors.Turquoise,
        0.75f to MonoColors.Crimson,
        1.0f to Color.Transparent,
    )

    /** Vertical accent used for spectrum bars (bright base, cool top). */
    val gradSpectrum = Brush.verticalGradient(
        listOf(MonoColors.Violet, MonoColors.Turquoise),
    )

    /** Aurora wash for empty artwork / backdrops. */
    fun aurora(): Brush = Brush.linearGradient(
        listOf(
            MonoColors.Turquoise.copy(alpha = 0.18f),
            MonoColors.Void,
            MonoColors.Crimson.copy(alpha = 0.16f),
        ),
    )

    // Spacing scale (subset of --sp-*)
    val sp1 = 4.dp
    val sp2 = 8.dp
    val sp3 = 12.dp
    val sp4 = 16.dp
    val sp5 = 20.dp
    val sp6 = 24.dp
    val sp8 = 32.dp
}
