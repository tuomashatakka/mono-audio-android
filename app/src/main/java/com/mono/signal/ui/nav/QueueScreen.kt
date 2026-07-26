package com.mono.signal.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.model.PlaybackState
import com.mono.signal.ui.components.AlbumArt
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.components.formatDuration
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography

@Composable
fun QueueScreen(
    playback: PlaybackState,
    onBack: () -> Unit,
    onRemove: (Int) -> Unit,
    onOpenTrack: () -> Unit = {},
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Column(modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp, start = 32.dp, end = 32.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack)
            Spacer(Modifier.weight(1f)); MonoLabel("— QUEUE", color = palette.accent); Spacer(Modifier.weight(1f)); Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("Queue", style = MonoTypography.displaySmall, color = MonoColors.Fg1)
        Spacer(Modifier.height(24.dp))
        if (playback.queue.isEmpty()) {
            Text("No tracks queued yet. Swipe a library row to add music.", style = MonoTypography.bodyMedium, color = MonoColors.Fg3)
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset + 112.dp)) {
                itemsIndexed(playback.queue, key = { _, track -> track.id }) { index, track ->
                    val active = index == playback.queueIndex
                    Row(Modifier.fillMaxWidth().clickable(onClick = onOpenTrack).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(if (active) "▶" else (index + 1).toString().padStart(2, '0'), style = MonoTypography.bodySmall, color = if (active) palette.accent else MonoColors.Fg3)
                        AlbumArt(track, Modifier.size(44.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = MonoTypography.bodyMedium, color = if (active) palette.accent else MonoColors.Fg1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${track.artist} • ${formatDuration(track.durationMs)}", style = MonoTypography.bodySmall, color = MonoColors.Fg3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        MonoIconButton(MonoGlyph.MORE, "Remove", { onRemove(index) }, accent = palette.accent)
                    }
                }
            }
        }
    }
}
