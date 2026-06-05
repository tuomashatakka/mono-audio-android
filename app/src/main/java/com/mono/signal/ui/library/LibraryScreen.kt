package com.mono.signal.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mono.signal.ui.components.EdgeGradientLine
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.components.TrackRow
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoTypography
import com.mono.signal.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onTrackClick: (Int) -> Unit,
    onRequestPermission: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        MonoLabel("— OVERVIEW")
        Spacer(Modifier.height(6.dp))
        Text("Library", style = MonoTypography.displaySmall, color = MonoColors.Fg1)
        Spacer(Modifier.height(12.dp))
        EdgeGradientLine()
        Spacer(Modifier.height(8.dp))

        when {
            !state.permissionGranted -> PermissionPrompt(onRequestPermission)
            state.tracks.isEmpty() && !state.loading -> EmptyState()
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isActive = track.mediaId == state.activeTrackId,
                        onClick = { onTrackClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    CenteredMessage(
        title = "AUDIO ACCESS NEEDED",
        body = "MONO Signal plays the music already on your device. Grant audio access to scan your library.",
    ) {
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = MonoColors.Turquoise,
                contentColor = MonoColors.Void,
            ),
        ) { Text("GRANT ACCESS") }
    }
}

@Composable
private fun EmptyState() {
    CenteredMessage(
        title = "NO TRACKS FOUND",
        body = "No local audio was found on this device. Add some music and pull to refresh.",
    )
}

@Composable
private fun CenteredMessage(
    title: String,
    body: String,
    action: @Composable () -> Unit = {},
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            MonoLabel("— $title")
            Text(
                body,
                style = MonoTypography.bodyMedium,
                color = MonoColors.Fg3,
                textAlign = TextAlign.Center,
            )
            action()
        }
    }
}
