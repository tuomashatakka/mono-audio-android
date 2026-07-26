package com.mono.signal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.icons.MonoIcon
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoLabelStyle

enum class NavTab(val label: String, val glyph: MonoGlyph, val enabled: Boolean) {
    LIB("LIB", MonoGlyph.NAV_LIB, true),
    DSP("DSP", MonoGlyph.NAV_SCN, true),
    QUE("QUE", MonoGlyph.NAV_QUE, true),
    CFG("CFG", MonoGlyph.NAV_CFG, true),
}

/** The section tab bar. Each slot maps to a page of the swipeable pager in [AppNav]. */
@Composable
fun MonoBottomNav(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Column(modifier = modifier.fillMaxWidth().background(palette.panel)) {
        EdgeGradientLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavTab.entries.forEach { tab ->
                val active = tab == selected
                val tint = when {
                    !tab.enabled -> MonoColors.Fg4
                    active -> palette.accent
                    else -> MonoColors.Fg3
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = tab.enabled,
                            onClick = { onSelect(tab) },
                        )
                        .padding(horizontal = 8.dp),
                ) {
                    MonoIcon(tab.glyph, tint = tint, size = 22.dp)
                    Text(
                        tab.label,
                        style = MonoLabelStyle.copy(fontSize = 9.sp, letterSpacing = 2.sp),
                        color = tint,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(0.dp))
    }
}
