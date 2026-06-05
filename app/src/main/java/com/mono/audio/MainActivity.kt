package com.mono.audio

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mono.audio.data.AudioTrack
import com.mono.audio.ui.PlayerUiState
import com.mono.audio.ui.PlayerViewModel
import com.mono.audio.ui.components.FftGraph
import com.mono.audio.ui.components.WaveformProgressBar
import com.mono.audio.ui.theme.Cyan
import com.mono.audio.ui.theme.Ink
import com.mono.audio.ui.theme.Magenta
import com.mono.audio.ui.theme.MonoAudioTheme
import com.mono.audio.ui.theme.Muted
import com.mono.audio.ui.theme.Panel
import com.mono.audio.ui.theme.Violet
import com.mono.audio.ui.theme.Void

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MonoAudioTheme {
                val viewModel: PlayerViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                PermissionGate(viewModel)
                MonoPlayerScreen(
                    state = state,
                    onTrackClick = viewModel::play,
                    onToggle = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                )
            }
        }
    }
}

@Composable
private fun PermissionGate(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissions = listOf(storagePermission, Manifest.permission.RECORD_AUDIO)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        viewModel.onPermissionsChanged(grants.values.all { it })
    }

    LaunchedEffect(Unit) {
        val granted = permissions.all { ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED }
        viewModel.onPermissionsChanged(granted)
        if (!granted) launcher.launch(permissions.toTypedArray())
    }
}

@Composable
private fun MonoPlayerScreen(
    state: PlayerUiState,
    onTrackClick: (AudioTrack) -> Unit,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.13f), Color.Transparent), radius = 780f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 26.dp),
        ) {
            Header()
            Spacer(Modifier.height(22.dp))
            NowPlayingCard(state, onToggle, onSeek)
            Spacer(Modifier.height(22.dp))
            LibraryList(state, onTrackClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("△", color = Cyan, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text("MONO", color = Ink, letterSpacing = 7.sp, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("  |  LOCAL", color = Muted, letterSpacing = 3.sp, fontSize = 12.sp)
    }
}

@Composable
private fun NowPlayingCard(state: PlayerUiState, onToggle: () -> Unit, onSeek: (Float) -> Unit) {
    NeonPanel {
        Text("– NOW PLAYING", color = Cyan, letterSpacing = 5.sp, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .border(1.dp, Cyan.copy(alpha = 0.35f)),
        ) {
            FftGraph(state.fftBands, Modifier.align(Alignment.Center).padding(18.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            state.selectedTrack?.title ?: "Select a local track",
            color = Ink,
            fontSize = 34.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            state.selectedTrack?.artist ?: if (state.hasAudioPermission) "Library ready" else "Audio permissions required",
            color = Muted,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        WaveformProgressBar(progress = state.progress, onSeek = onSeek)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = Cyan, fontSize = 12.sp, letterSpacing = 2.sp)
            Text("-${formatTime(((state.selectedTrack?.durationMs ?: 0L) - state.positionMs).coerceAtLeast(0L))}", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ControlChip("↢")
            Spacer(Modifier.width(18.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Void),
            ) { Text(if (state.isPlaying) "Ⅱ" else "▶", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(18.dp))
            ControlChip("↣")
        }
    }
}

@Composable
private fun LibraryList(state: PlayerUiState, onTrackClick: (AudioTrack) -> Unit, modifier: Modifier = Modifier) {
    NeonPanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Library", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(12.dp))
            Text("TRACKS · ${state.tracks.size}", color = Cyan, letterSpacing = 4.sp, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        if (!state.hasAudioPermission) {
            Text("Grant audio + visualizer permissions to index local files and animate the live FFT.", color = Muted)
        } else if (state.tracks.isEmpty()) {
            Text("No local music found in MediaStore yet.", color = Muted)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.tracks, key = { it.id }) { track ->
                    TrackRow(track, state.selectedTrack?.id == track.id, onTrackClick)
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: AudioTrack, selected: Boolean, onClick: (AudioTrack) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .background(if (selected) Cyan.copy(alpha = 0.09f) else Panel.copy(alpha = 0.48f))
            .clickable { onClick(track) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color.Black)
                .border(1.dp, if (selected) Cyan else Color.White.copy(alpha = 0.05f)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = Ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 1.sp)
        }
        Text(formatTime(track.durationMs), color = Muted, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun NeonPanel(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Brush.horizontalGradient(listOf(Cyan, Violet, Magenta))))
            .background(Brush.verticalGradient(listOf(Panel, Void))),
        color = Color.Transparent,
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun ControlChip(label: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Ink, fontSize = 22.sp) }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
