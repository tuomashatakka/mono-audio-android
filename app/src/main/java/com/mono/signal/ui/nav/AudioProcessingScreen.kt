package com.mono.signal.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.model.DspConfig
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography
import kotlin.math.roundToInt

private val EQ_LABELS = listOf(
    "35", "65", "85", "120", "240", "480", "960", "1.8k", "3.5k", "7k", "12k", "16k",
)

@Composable
fun AudioProcessingScreen(
    config: DspConfig,
    onEnabled: (Boolean) -> Unit,
    onEqBand: (Int, Float) -> Unit,
    onPreset: (List<Float>) -> Unit,
    onFlat: () -> Unit,
    onCompressor: (DspConfig.Compressor) -> Unit,
    onLimiter: (DspConfig.Limiter) -> Unit,
    onBack: () -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(top = 8.dp, start = 32.dp, end = 32.dp, bottom = bottomInset + 112.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack)
            Spacer(Modifier.weight(1f)); MonoLabel("— AUDIO DSP", color = palette.accent); Spacer(Modifier.weight(1f)); Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Audio processing", style = MonoTypography.displaySmall, color = MonoColors.Fg1, modifier = Modifier.weight(1f))
            Switch(
                checked = config.enabled,
                onCheckedChange = onEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.accent,
                    checkedTrackColor = palette.accent.copy(alpha = 0.4f),
                ),
            )
        }
        Text(
            if (config.enabled) "Processing active" else "Bypassed",
            style = MonoTypography.bodySmall, color = if (config.enabled) palette.accent else MonoColors.Fg3,
        )
        Spacer(Modifier.height(28.dp))

        EqBank(config = config, onEqBand = onEqBand, onPreset = onPreset, onFlat = onFlat)

        Spacer(Modifier.height(24.dp))
        DspSection("COMPRESSOR") {
            DspSlider("Attack", "ms", 0f, 100f, config.compressor.attackMs) { onCompressor(config.compressor.copy(attackMs = it)) }
            DspSlider("Release", "ms", 20f, 800f, config.compressor.releaseMs) { onCompressor(config.compressor.copy(releaseMs = it)) }
            DspSlider("Threshold", "dB", -60f, 0f, config.compressor.thresholdDb) { onCompressor(config.compressor.copy(thresholdDb = it)) }
            DspSlider("Compression", ":1", 1f, 80f, config.compressor.ratio) { onCompressor(config.compressor.copy(ratio = it)) }
        }
        Spacer(Modifier.height(24.dp))
        DspSection("LIMITER") {
            DspSlider("Threshold", "dB", -24f, 0f, config.limiter.thresholdDb) { onLimiter(config.limiter.copy(thresholdDb = it)) }
            DspSlider("Gain", "dB", -12f, 12f, config.limiter.gainDb) { onLimiter(config.limiter.copy(gainDb = it)) }
            DspSlider("Release", "ms", 10f, 1000f, config.limiter.releaseMs) { onLimiter(config.limiter.copy(releaseMs = it)) }
        }
    }
}

@Composable
private fun EqBank(
    config: DspConfig,
    onEqBand: (Int, Float) -> Unit,
    onPreset: (List<Float>) -> Unit,
    onFlat: () -> Unit,
) {
    val palette = LocalMonoPalette.current
    var presetsOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MonoLabel("— 12-BAND EQ", color = palette.accent)
        Spacer(Modifier.weight(1f))
        PillButton("FLAT", onClick = onFlat)
        Spacer(Modifier.width(8.dp))
        Box {
            PillButton("PRESETS", onClick = { presetsOpen = true })
            DropdownMenu(expanded = presetsOpen, onDismissRequest = { presetsOpen = false }) {
                DspConfig.PRESETS.forEach { (name, gains) ->
                    DropdownMenuItem(
                        text = { Text(name, color = MonoColors.Fg1) },
                        onClick = { onPreset(gains); presetsOpen = false },
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
            .background(palette.panelElevated).padding(vertical = 16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EQ_LABELS.forEachIndexed { index, label ->
                val gain = config.eqBands.getOrElse(index) { 0f }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
                    Text(
                        "${if (gain > 0) "+" else ""}${gain.roundToInt()}",
                        style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (gain == 0f) MonoColors.Fg3 else palette.accent,
                    )
                    Spacer(Modifier.height(6.dp))
                    VerticalSlider(
                        value = gain,
                        onValueChange = { onEqBand(index, it) },
                        valueRange = DspConfig.EQ_MIN_DB..DspConfig.EQ_MAX_DB,
                        accent = palette.accent,
                        modifier = Modifier.height(150.dp).width(36.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MonoColors.Fg3,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** A Material [Slider] rotated 270° so it runs bottom-to-top, sized via a layout swap. */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = MonoColors.BorderSoft,
        ),
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                    ),
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            },
    )
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val palette = LocalMonoPalette.current
    Box(
        Modifier.clip(RoundedCornerShape(2.dp))
            .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = palette.accent)
    }
}

@Composable private fun DspSection(title: String, content: @Composable () -> Unit) {
    MonoLabel("— $title", color = LocalMonoPalette.current.accent)
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(LocalMonoPalette.current.panelElevated).padding(16.dp)) { content() }
}

@Composable private fun DspSlider(label: String, unit: String, min: Float, max: Float, value: Float, onChange: (Float) -> Unit) {
    val palette = LocalMonoPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MonoTypography.bodySmall, color = MonoColors.Fg2, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(2.dp)).border(1.dp, MonoColors.BorderSoft, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("${value.toInt()} $unit", style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = palette.accent)
        }
    }
    Slider(value = value, onValueChange = onChange, valueRange = min..max, colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent))
}
