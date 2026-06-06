package com.mono.signal.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography

@Composable
fun AudioProcessingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalMonoPalette.current
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(top = 68.dp, start = 28.dp, end = 28.dp, bottom = 130.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack)
            Spacer(Modifier.weight(1f)); MonoLabel("— AUDIO DSP", color = palette.accent); Spacer(Modifier.weight(1f)); Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Audio processing", style = MonoTypography.displaySmall, color = MonoColors.Fg1)
        Spacer(Modifier.height(28.dp))
        DspSection("COMPRESSOR") {
            DspSlider("Attack", "ms", 0f, 100f, 24f)
            DspSlider("Release", "ms", 20f, 800f, 180f)
            DspSlider("Threshold", "dB", -60f, 0f, -18f)
            DspSlider("Compression", ":1", 1f, 80f, 4f)
        }
        Spacer(Modifier.height(24.dp))
        DspSection("12-BAND EQ") {
            listOf("35 Hz", "65 Hz", "85 Hz", "120 Hz", "240 Hz", "480 Hz", "960 Hz", "1.8 kHz", "3.5 kHz", "7 kHz", "12 kHz", "16 kHz").forEach {
                DspSlider(it, "dB", -12f, 12f, 0f)
            }
        }
        Spacer(Modifier.height(24.dp))
        DspSection("LIMITER") {
            DspSlider("Threshold", "dB", -24f, 0f, -1f)
            DspSlider("Gain", "dB", -12f, 12f, 0f)
            DspSlider("Release", "ms", 10f, 1000f, 120f)
        }
    }
}

@Composable private fun DspSection(title: String, content: @Composable () -> Unit) {
    MonoLabel("— $title", color = LocalMonoPalette.current.accent)
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(LocalMonoPalette.current.panelElevated).padding(16.dp)) { content() }
}

@Composable private fun DspSlider(label: String, unit: String, min: Float, max: Float, initial: Float) {
    val palette = LocalMonoPalette.current
    var value by remember { mutableStateOf(initial) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MonoTypography.bodySmall, color = MonoColors.Fg2, modifier = Modifier.weight(1f))
        Box(Modifier.clip(RoundedCornerShape(2.dp)).border(1.dp, MonoColors.BorderSoft, RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("${value.toInt()} $unit", style = MonoTypography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = palette.accent)
        }
    }
    Slider(value = value, onValueChange = { value = it }, valueRange = min..max, colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent))
}
