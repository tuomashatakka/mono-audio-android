package com.mono.signal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The Android mirror of the MONO design canvas tokens (system/colors_and_type.css),
 * organised as one parametrized source of truth. Components read these instead of
 * inlining magic numbers, so the system can be re-tuned in a single place.
 *
 * Colour primitives live in [MonoColors]; the live, theme-driven palette in [MonoPalette].
 * This object carries everything else: gradients, spacing, radii, motion, elevation,
 * tracking, and the derived [MonoShapes] handed to MaterialTheme.
 */
object MonoTokens {

    // ── GRADIENTS ────────────────────────────────────────────────────────────
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

    // ── SPACING (4px base, --sp-*) ───────────────────────────────────────────
    val sp1 = 4.dp
    val sp2 = 8.dp
    val sp3 = 12.dp
    val sp4 = 16.dp
    val sp5 = 20.dp
    val sp6 = 24.dp
    val sp8 = 32.dp
    val sp10 = 40.dp
    val sp12 = 48.dp
    val sp16 = 64.dp
    val sp20 = 80.dp

    // ── RADII — MONO is sharp by default (--radius-*) ─────────────────────────
    val radius0 = 0.dp
    val radius1 = 1.dp
    val radius2 = 2.dp

    // ── TRACKING (letter-spacing, --tracking-*) ──────────────────────────────
    val trackingTight = (-0.02).em
    val trackingWide = 0.08.em
    val trackingWidest = 0.24.em

    // ── MOTION (ms, --duration-*) ─────────────────────────────────────────────
    const val durInstant = 80
    const val durFast = 180
    const val durBase = 320
    const val durSlow = 600
    const val durCinematic = 4200

    // ── ELEVATION / GLOW (dp) ─────────────────────────────────────────────────
    val elevLow = 4.dp
    val elevMid = 12.dp
    val elevHigh = 32.dp
    val glowRadius = 24.dp

    // ── SIZING (--touch-target / --control-height) ───────────────────────────
    val touchTarget = 44.dp
    val controlHeight = 40.dp
    val controlHeightSm = 28.dp
    val controlHeightLg = 56.dp
}

/**
 * MONO is sharp. Every Material3 shape slot collapses to a near-zero corner so
 * surfaces read as flat, framed panels rather than rounded cards.
 */
val MonoShapes = Shapes(
    extraSmall = RoundedCornerShape(MonoTokens.radius0),
    small = RoundedCornerShape(MonoTokens.radius1),
    medium = RoundedCornerShape(MonoTokens.radius2),
    large = RoundedCornerShape(MonoTokens.radius2),
    extraLarge = RoundedCornerShape(MonoTokens.radius2),
)