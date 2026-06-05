package com.mono.signal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mono.signal.model.RepeatMode
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.theme.LocalMonoPalette

/** Transport row from the mockup: shuffle · prev · [neon play] · next · repeat. */
@Composable
fun TransportControls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoIconButton(
            MonoGlyph.SHUFFLE, "Shuffle", onShuffle,
            bordered = true, active = shuffle, accent = palette.accent,
        )
        MonoIconButton(MonoGlyph.PREV, "Previous", onPrevious, bordered = true, accent = palette.accent)
        MonoPlayButton(isPlaying = isPlaying, onClick = onPlayPause, accent = palette.accent)
        MonoIconButton(MonoGlyph.NEXT, "Next", onNext, bordered = true, accent = palette.accent)
        MonoIconButton(
            if (repeatMode == RepeatMode.ONE) MonoGlyph.REPEAT_ONE else MonoGlyph.REPEAT,
            "Repeat", onRepeat,
            bordered = true, active = repeatMode != RepeatMode.OFF, accent = palette.accent,
        )
    }
}
