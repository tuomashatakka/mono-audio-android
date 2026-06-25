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
import com.mono.signal.ui.theme.MonoTypography
import com.mono.signal.ui.theme.paletteFor

@Composable
fun SettingsScreen(
    theme: ThemeConfig,
    onBack: () -> Unit,
    onAccent: (AccentOption) -> Unit,
    onBackground: (BackgroundOption) -> Unit,
    onFftBlockSize: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, start = 28.dp, end = 28.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack)
            Spacer(Modifier.weight(1f))
            MonoLabel("— CONFIG", color = palette.accent)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Settings", style = MonoTypography.displaySmall, color = MonoColors.Fg1)

        Spacer(Modifier.height(36.dp))

        // ---- Appearance ----
        Section("APPEARANCE") {
            Text("ACCENT", style = MonoTypography.bodySmall.copy(letterSpacing = 2.sp), color = MonoColors.Fg3)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentOption.entries.forEach { option ->
                    AccentSwatch(
                        color = paletteFor(theme.copy(accent = option)).accent,
                        selected = theme.accent == option,
                        onClick = { onAccent(option) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("BACKGROUND", style = MonoTypography.bodySmall.copy(letterSpacing = 2.sp), color = MonoColors.Fg3)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
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

        Spacer(Modifier.height(32.dp))

        // ---- Reference config sections (display only for the MVP) ----
        Section("AUDIO") {
            ConfigRow("Sample rate", "48 kHz", palette.accent)
            Spacer(Modifier.height(12.dp))
            Text("FFT BLOCKS", style = MonoTypography.bodySmall.copy(letterSpacing = 2.sp), color = MonoColors.Fg3)
            Spacer(Modifier.height(12.dp))
            FftBlockToggle(theme.fftBlockSize, onFftBlockSize, palette.accent)
            Spacer(Modifier.height(18.dp))
            ConfigRow("Gapless", "ON", palette.accent)
            ConfigRow("Replay-gain", "ON", palette.accent)
        }
        Spacer(Modifier.height(24.dp))
        Section("PLAYBACK") {
            ConfigRow("Crossfade", "0 s", palette.accent)
            ConfigRow("Default visualizer", "Match track", palette.accent)
            ConfigRow("Skip silent intros", "OFF", MonoColors.Fg3)
        }
        Spacer(Modifier.height(24.dp))
        Section("ACCOUNT") {
            ConfigRow("Display name", "K. NOVA", palette.accent)
            ConfigRow("Subscription", "Studio tier", palette.accent)
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun FftBlockToggle(current: Int, onSelect: (Int) -> Unit, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(512, 1024, 2048, 4096, 8192).forEach { size ->
            val selected = current == size
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) accent.copy(alpha = 0.22f) else LocalMonoPalette.current.panelElevated)
                    .border(1.dp, if (selected) accent else MonoColors.BorderSoft, RoundedCornerShape(2.dp))
                    .clickable { onSelect(size) }
                    .padding(vertical = 12.dp),
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
    Spacer(Modifier.height(14.dp))
    content()
}

@Composable
private fun ConfigRow(label: String, value: String, valueColor: Color) {
    val palette = LocalMonoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(palette.panelElevated)
            .padding(horizontal = 18.dp, vertical = 18.dp),
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
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(1.dp, if (selected) accent else MonoColors.BorderSoft, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            option.label,
            style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (selected) accent else MonoColors.Fg2,
        )
    }
}
