package com.mono.signal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mono.signal.R

/**
 * MONO Signal typography, wired to the design system's shipped fonts.
 *
 * Mirrors the canonical tokens in the design canvas `system/colors_and_type.css`:
 *   --font-display / --font-body : "Sofia Pro", "Montserrat"
 *   --font-sans                  : "Montserrat"
 *   weights 100 / 300 / 400 / 900, --text-* scale (10..88px), tracking/leading.
 *
 * JetBrains Mono (`--font-mono`) is intentionally dropped — the only families we ship are
 * Sofia Pro + Montserrat. Any monospace readout falls back to system [FontFamily.Monospace].
 *
 * Family split (deliberate, differs slightly from the CSS): Sofia Pro ships only in Light, which
 * reads beautifully as a cinematic *display* face but is too thin for small body copy, so display
 * slots use Sofia Pro and every title/body/label slot uses Montserrat (the canonical --font-sans).
 */
val MontserratFamily = FontFamily(
    Font(R.font.montserrat_hairline, FontWeight.Thin),    // --weight-hairline: 100
    Font(R.font.montserrat_light, FontWeight.Light),      // --weight-light: 300
    Font(R.font.montserrat_regular, FontWeight.Normal),   // --weight-regular: 400
    Font(R.font.montserrat_black, FontWeight.Black),      // --weight-black: 900
)

val SofiaProFamily = FontFamily(
    Font(R.font.sofia_pro_light, FontWeight.Light),
)

val MonoTypography = Typography(
    // ── DISPLAY (Sofia Pro Light, cinematic light headers) ────────────────
    displayLarge = TextStyle(
        fontFamily = SofiaProFamily,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,       // --text-4xl
        lineHeight = 62.sp,     // --leading-tight 1.1
        letterSpacing = (-1).sp,// --tracking-tight
    ),
    displayMedium = TextStyle(
        fontFamily = SofiaProFamily,
        fontWeight = FontWeight.Light,
        fontSize = 40.sp,       // --text-3xl
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SofiaProFamily,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,       // --text-2xl — screen titles
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    // ── HEADLINE / TITLE (Montserrat) ─────────────────────────────────────
    headlineSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Light,
        fontSize = 20.sp,       // --text-xl
        lineHeight = 25.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,       // --text-xl
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,       // --text-lg
        lineHeight = 20.sp,
    ),
    // ── BODY (Montserrat) ─────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,       // --text-lg
        lineHeight = 24.sp,     // --leading-normal 1.5
    ),
    bodyMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,       // --text-base
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,       // --text-sm
        lineHeight = 18.sp,
        color = MonoColors.Fg3,
    ),
    // ── LABEL (Montserrat) ────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,       // --text-sm
    ),
    labelSmall = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,       // --text-xs
    ),
)

/**
 * The tracked, uppercase "eyebrow" label used for every section heading (callers pass
 * `text.uppercase()`). Re-expressed off the dropped JetBrains/monospace look — now Montserrat
 * Light with the widest tracking (--tracking-widest 0.24em), so the whole app rides on just the
 * two shipped families.
 *
 * TODO(design — your call): two valid treatments, pick one and tweak the 3 fields below.
 *   (a) [current default] Montserrat Light + wide tracking — tight to the Sofia/Montserrat
 *       system, fewest families. Set fontFamily = MontserratFamily, fontWeight = Light.
 *   (b) Keep the "instrument readout" character: fontFamily = FontFamily.Monospace (system
 *       mono — NOT JetBrains, so still within scope), fontWeight = Medium.
 * Trade-off: (a) is more cohesive/minimal; (b) preserves the technical eyebrow vibe the mockup
 * leans on. letterSpacing ~2.4.sp ≈ 0.24em at this size — nudge to taste.
 */
val MonoLabelStyle = TextStyle(
    fontFamily = MontserratFamily,
    fontWeight = FontWeight.Light,
    fontSize = 11.sp,           // --text-xs
    letterSpacing = 2.4.sp,     // --tracking-widest
)
