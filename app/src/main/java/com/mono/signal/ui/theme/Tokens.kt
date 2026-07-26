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
 * Semantic spacing scale — a 4dp-based t-shirt ramp that is the single source of truth for
 * every layout gap, padding, and inset. Components reference `MonoSpacing.md` etc. instead of
 * inlining `dp` literals, so the whole app's rhythm can be re-tuned in one place.
 *
 * The steps [md]…[xxxl] climb in even 8dp increments (16 · 24 · 32 · 40 · 48), which covers the
 * app's real large gaps exactly; [xxs]…[sm] fill in the finer 4dp increments below.
 */
object MonoSpacing {
    val none = 0.dp
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp
    val huge = 64.dp

    // ── Semantic layout roles (named by intent, composed from the scale) ──────
    /** Default horizontal margin framing a full screen. */
    val screenEdge = xl
    /** Bottom gutter a scrolling page adds (on top of the nav-bar height) so its last row
     *  clears the mini-player floating above the bottom nav. */
    val playerClearance = huge + xxl
}

/**
 * Corner radii. MONO is sharp by default, so the whole ramp stays near zero — surfaces read as
 * flat, framed panels rather than rounded cards.
 */
object MonoRadius {
    val none = 0.dp
    val xs = 1.dp
    val sm = 2.dp
    val md = 3.dp
}

/**
 * The Android mirror of the MONO design canvas tokens (system/colors_and_type.css),
 * organised as one parametrized source of truth. Components read these instead of
 * inlining magic numbers, so the system can be re-tuned in a single place.
 *
 * Colour primitives live in [MonoColors]; the live, theme-driven palette in [MonoPalette];
 * spacing in [MonoSpacing] and radii in [MonoRadius]. This object carries everything else:
 * gradients, motion, elevation, tracking, sizing, and the derived [MonoShapes].
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
    extraSmall = RoundedCornerShape(MonoRadius.none),
    small = RoundedCornerShape(MonoRadius.xs),
    medium = RoundedCornerShape(MonoRadius.sm),
    large = RoundedCornerShape(MonoRadius.sm),
    extraLarge = RoundedCornerShape(MonoRadius.sm),
)