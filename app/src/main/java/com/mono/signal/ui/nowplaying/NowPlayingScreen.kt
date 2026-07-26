package com.mono.signal.ui.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mono.signal.model.NowPlayingGraphic
import com.mono.signal.ui.components.AlbumArt
import com.mono.signal.ui.components.FftGraph
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.components.StereoWidthGraph
import com.mono.signal.ui.components.TransportControls
import com.mono.signal.ui.components.Waveform3DGraph
import com.mono.signal.ui.components.WaveformSeekBar
import com.mono.signal.ui.components.formatDuration
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.icons.MonoIcon
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography
import com.mono.signal.viewmodel.NowPlayingUiState

@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    audioGranted: Boolean,
    onBack: () -> Unit,
    onCycleGraphic: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onScrub: (Float, Float) -> Unit,
    onScrubEnd: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onEnableVisualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    val playback = state.playback
    var showTags by remember { mutableStateOf(false) }
    // Opaque background so the full-screen overlay fully hides the pager and bottom nav behind it.
    Box(modifier = modifier.fillMaxSize().background(palette.background)) {
        AlbumArt(
            track = playback.currentTrack,
            modifier = Modifier
                .fillMaxSize()
                .blur(54.dp)
                .alpha(0.14f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 8.dp, start = 32.dp, end = 32.dp),
        ) {
        // Header: caret back · NOW PLAYING · more
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoIconButton(MonoGlyph.CARET_LEFT, "Back", onBack, bordered = true, accent = palette.accent)
            Spacer(Modifier.weight(1f))
            MonoLabel("— NOW PLAYING", color = palette.accent)
            Spacer(Modifier.weight(1f))
            MonoIconButton(MonoGlyph.MORE, "More", { showTags = true }, bordered = true, accent = palette.accent)
        }

        Spacer(Modifier.height(40.dp))

        GraphicArea(
            state = state,
            audioGranted = audioGranted,
            onTap = onCycleGraphic,
            onPrevious = onPrevious,
            onNext = onNext,
            onEnableVisualizer = onEnableVisualizer,
            modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f).align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(32.dp))

        // Title + favourite
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = playback.currentTrack?.title ?: "Nothing playing",
                    style = MonoTypography.displaySmall,
                    color = MonoColors.Fg1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = playback.currentTrack?.artist ?: "—",
                    style = MonoTypography.bodyMedium,
                    color = MonoColors.Fg3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            var favourite by remember { mutableStateOf(false) }
            MonoIconButton(
                if (favourite) MonoGlyph.HEART_FILLED else MonoGlyph.HEART,
                "Favourite", { favourite = !favourite },
                bordered = true, active = favourite, accent = palette.accent,
            )
        }

        Spacer(Modifier.height(28.dp))

        WaveformSeekBar(
            envelope = state.waveformEnvelope,
            progress = playback.progress,
            onSeek = onSeek,
            onScrub = onScrub,
            onScrubEnd = onScrubEnd,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playback.positionMs), style = MonoTypography.bodySmall, color = palette.accent)
            val remaining = (playback.durationMs - playback.positionMs).coerceAtLeast(0L)
            Text("-${formatDuration(remaining)}", style = MonoTypography.bodySmall, color = MonoColors.Fg3)
        }

        Spacer(Modifier.weight(1f))

            TransportControls(
            isPlaying = playback.isPlaying,
            shuffle = playback.shuffle,
            repeatMode = playback.repeatMode,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onShuffle = onShuffle,
            onRepeat = onRepeat,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        )
        }
        if (showTags) {
            FileTagsDialog(
                title = playback.currentTrack?.title.orEmpty(),
                artist = playback.currentTrack?.artist.orEmpty(),
                album = playback.currentTrack?.album.orEmpty(),
                path = playback.currentTrack?.filePath.orEmpty(),
                onDismiss = { showTags = false },
            )
        }
    }
}

@Composable
private fun GraphicArea(
    state: NowPlayingUiState,
    audioGranted: Boolean,
    onTap: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEnableVisualizer: () -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    Box(
        modifier = modifier
            .background(palette.panel.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
    ) {
        Crossfade(targetState = state.graphic, animationSpec = tween(320), label = "graphic") { graphic ->
            when (graphic) {
                NowPlayingGraphic.ALBUM_ART -> {
                    var totalDrag by remember(state.playback.currentTrack?.id) { mutableStateOf(0f) }
                    AlbumArt(
                        track = state.playback.currentTrack,
                        // Horizontal-only draggable: a clean tap still reaches the parent's
                        // tap-to-cycle handler; a horizontal swipe goes prev/next without misfiring.
                        modifier = Modifier
                            .fillMaxSize()
                            .draggable(
                                state = rememberDraggableState { delta -> totalDrag += delta },
                                orientation = Orientation.Horizontal,
                                onDragStarted = { totalDrag = 0f },
                                onDragStopped = {
                                    when {
                                        totalDrag < -80f -> onNext()
                                        totalDrag > 80f -> onPrevious()
                                    }
                                },
                            ),
                    )
                }
                NowPlayingGraphic.WAVE_3D -> Waveform3DGraph(
                    waveformLeft = state.frame.waveformLeft,
                    waveformRight = state.frame.waveformRight,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
                NowPlayingGraphic.FFT -> FftGraph(
                    peakBands = state.frame.fftPeakBands,
                    rmsBands = state.frame.fftRmsBands,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
                NowPlayingGraphic.STEREO_WIDTH -> StereoWidthGraph(
                    waveform = state.frame.waveform,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                )
            }
        }

        // Live visuals are read straight from the player's PCM output (no mic permission needed),
        // so the only time there's nothing to show is when no track is loaded.
        if (state.graphic != NowPlayingGraphic.ALBUM_ART && state.playback.currentTrack == null) {
            VisualizerHint(Modifier.align(Alignment.Center))
        }

        MonoLabel(
            text = "— ${state.graphic.label}",
            color = palette.accent,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )
    }
}

@Composable
private fun VisualizerHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MonoIcon(MonoGlyph.NAV_SCN, tint = MonoColors.Fg3, size = 28.dp)
        Text(
            "Play a track to see the live waveform and spectrum.",
            style = MonoTypography.bodySmall,
            color = MonoColors.Fg3,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FileTagsDialog(
    title: String,
    artist: String,
    album: String,
    path: String,
    onDismiss: () -> Unit,
) {
    var editTitle by remember(title) { mutableStateOf(title) }
    var editArtist by remember(artist) { mutableStateOf(artist) }
    var editAlbum by remember(album) { mutableStateOf(album) }
    val allTags by produceState(initialValue = emptyList<Pair<String, String>>(), path) {
        value = readAllTags(path).ifEmpty {
            listOf("title" to title, "artist" to artist, "album" to album, "path" to path).filter { it.second.isNotBlank() }
        }
    }
    Box(
        Modifier.fillMaxSize().background(MonoColors.Void.copy(alpha = 0.78f)).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(3.dp))
                .background(LocalMonoPalette.current.panelElevated)
                .border(1.dp, LocalMonoPalette.current.accent.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MonoLabel("— __ALL__ META TAGS", color = LocalMonoPalette.current.accent)
            OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") })
            OutlinedTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text("Artist") })
            OutlinedTextField(value = editAlbum, onValueChange = { editAlbum = it }, label = { Text("Album") })
            Column(Modifier.height(190.dp).verticalScroll(rememberScrollState())) {
                allTags.forEach { (key, value) ->
                    Text("$key = $value", style = MonoTypography.bodySmall, color = MonoColors.Fg2)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("CANCEL") }
                TextButton(onClick = onDismiss) { Text("SAVE") }
            }
        }
    }
}

private fun readAllTags(path: String): List<Pair<String, String>> {
    if (path.isBlank()) return emptyList()
    val keys = listOf(
        MediaMetadataRetriever.METADATA_KEY_TITLE to "title",
        MediaMetadataRetriever.METADATA_KEY_ARTIST to "artist",
        MediaMetadataRetriever.METADATA_KEY_ALBUM to "album",
        MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST to "album_artist",
        MediaMetadataRetriever.METADATA_KEY_AUTHOR to "author",
        MediaMetadataRetriever.METADATA_KEY_COMPOSER to "composer",
        MediaMetadataRetriever.METADATA_KEY_GENRE to "genre",
        MediaMetadataRetriever.METADATA_KEY_YEAR to "year",
        MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER to "track_number",
        MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER to "disc_number",
        MediaMetadataRetriever.METADATA_KEY_DURATION to "duration_ms",
        MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "mime_type",
        MediaMetadataRetriever.METADATA_KEY_BITRATE to "bitrate",
        MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS to "num_tracks",
        MediaMetadataRetriever.METADATA_KEY_WRITER to "writer",
        MediaMetadataRetriever.METADATA_KEY_COMPILATION to "compilation",
        MediaMetadataRetriever.METADATA_KEY_DATE to "date",
    )
    return runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(path)
            keys.mapNotNull { (key, label) -> retriever.extractMetadata(key)?.takeIf { it.isNotBlank() }?.let { label to it } }
        }
    }.getOrDefault(emptyList())
}
