package com.mono.signal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.signal.model.AccentOption
import com.mono.signal.model.BackgroundOption
import com.mono.signal.model.ThemeConfig
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoRadius
import com.mono.signal.ui.theme.MonoSpacing
import com.mono.signal.ui.theme.MonoTypography
import com.mono.signal.ui.theme.paletteFor

@Composable
fun SettingsScreen(
    theme: ThemeConfig,
    onBack: () -> Unit,
    onAccent: (AccentOption) -> Unit,
    onBackground: (BackgroundOption) -> Unit,
    onFftBlockSize: (Int) -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = MonoSpacing.xs, start = MonoSpacing.screenEdge, end = MonoSpacing.screenEdge),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = MonoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack)
            Spacer(Modifier.weight(1f))
            MonoLabel("— CONFIG", color = palette.accent)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(MonoSpacing.lg))
        Text("Settings", style = MonoTypography.displaySmall, color = MonoColors.Fg1)

        Spacer(Modifier.height(MonoSpacing.xxl))

        // ---- Appearance ----
        Section("APPEARANCE") {
            Text("ACCENT", style = MonoTypography.bodySmall.copy(letterSpacing = 2.sp), color = MonoColors.Fg3)
            Spacer(Modifier.height(MonoSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(MonoSpacing.sm)) {
                AccentOption.entries.forEach { option ->
                    AccentSwatch(
                        color = paletteFor(theme.copy(accent = option)).accent,
                        selected = theme.accent == option,
                        onClick = { onAccent(option) },
                    )
                }
            }
            Spacer(Modifier.height(MonoSpacing.lg))
            Text("BACKGROUND", style = MonoTypography.bodySmall.copy(letterSpacing = 2.sp), color = MonoColors.Fg3)
            Spacer(Modifier.height(MonoSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(MonoSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                BackgroundOption.entries.forEach { option ->
                    BackgroundTile(
                        option = option,
                        color = paletteFor(theme.copy(background = option)).background,
                        selected = theme.background == option,
                        accent = palette.accent,
                        onClick = { onBackground(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(MonoSpacing.xl))

        // ---- Reference config sections (display only for the MVP) ----
        Section("AUDIO") {
            ConfigRow("Sample rate", "48 kHz", palette.accent)
            Spacer(Modifier.height(MonoSpacing.sm))
            Text("FFT BLOCKS", style = MonoTypography.bodySmall.copy(letterSpacing = 2.sp), color = MonoColors.Fg3)
            Spacer(Modifier.height(MonoSpacing.sm))
            FftBlockToggle(theme.fftBlockSize, onFftBlockSize, palette.accent)
            Spacer(Modifier.height(MonoSpacing.md))
            ConfigRow("Gapless", "ON", palette.accent)
            ConfigRow("Replay-gain", "ON", palette.accent)
        }
        Spacer(Modifier.height(MonoSpacing.lg))
        Section("PLAYBACK") {
            ConfigRow("Crossfade", "0 s", palette.accent)
            ConfigRow("Default visualizer", "Match track", palette.accent)
            ConfigRow("Skip silent intros", "OFF", MonoColors.Fg3)
        }
        Spacer(Modifier.height(MonoSpacing.lg))
        Section("ACCOUNT") {
            ConfigRow("Display name", "K. NOVA", palette.accent)
            ConfigRow("Subscription", "Studio tier", palette.accent)
        }
        Spacer(Modifier.height(bottomInset + MonoSpacing.playerClearance))
    }
}

@Composable
private fun FftBlockToggle(current: Int, onSelect: (Int) -> Unit, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(MonoSpacing.xs), modifier = Modifier.fillMaxWidth()) {
        listOf(512, 1024, 2048, 4096, 8192).forEach { size ->
            val selected = current == size
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(MonoRadius.sm))
                    .background(if (selected) accent.copy(alpha = 0.22f) else LocalMonoPalette.current.panelElevated)
                    .border(1.dp, if (selected) accent else MonoColors.BorderSoft, RoundedCornerShape(MonoRadius.sm))
                    .clickable { onSelect(size) }
                    .padding(vertical = MonoSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    size.toString(),
                    style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (selected) accent else MonoColors.Fg2,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val palette = LocalMonoPalette.current
    MonoLabel("— $title", color = palette.accent)
    Spacer(Modifier.height(MonoSpacing.sm))
    content()
}

@Composable
private fun ConfigRow(label: String, value: String, valueColor: Color) {
    val palette = LocalMonoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MonoSpacing.xs)
            .clip(RoundedCornerShape(MonoRadius.sm))
            .background(palette.panelElevated)
            .padding(horizontal = MonoSpacing.md, vertical = MonoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MonoTypography.bodyMedium, color = MonoColors.Fg1, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MonoTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = valueColor,
        )
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(2.dp, MonoColors.Fg1, CircleShape) else Modifier)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun BackgroundTile(
    option: BackgroundOption,
    color: Color,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MonoRadius.sm))
            .background(color)
            .border(1.dp, if (selected) accent else MonoColors.BorderSoft, RoundedCornerShape(MonoRadius.sm))
            .clickable(onClick = onClick)
            .padding(MonoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MonoSpacing.lg),
    ) {
        Text(
            option.label,
            style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (selected) accent else MonoColors.Fg2,
        )
    }
}
