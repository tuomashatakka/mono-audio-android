package com.mono.signal.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.mono.signal.model.GroupBy
import com.mono.signal.model.SortBy
import com.mono.signal.model.Track
import com.mono.signal.ui.components.EdgeGradientLine
import com.mono.signal.ui.components.MonoIconButton
import com.mono.signal.ui.components.MonoLabel
import com.mono.signal.ui.components.TrackRow
import com.mono.signal.ui.icons.MonoGlyph
import com.mono.signal.ui.theme.LocalMonoPalette
import com.mono.signal.ui.theme.MonoColors
import com.mono.signal.ui.theme.MonoLabelStyle
import com.mono.signal.ui.theme.MonoRadius
import com.mono.signal.ui.theme.MonoSpacing
import com.mono.signal.ui.theme.MonoTypography
import com.mono.signal.viewmodel.LibraryUiState

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onTrackClick: (Track) -> Unit,
    onRequestPermission: () -> Unit,
    onSortBy: (SortBy) -> Unit,
    onGroupBy: (GroupBy) -> Unit,
    onSearch: (String) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val palette = LocalMonoPalette.current
    val collapsed = remember(state.groupBy) { mutableStateListOf<String>() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = MonoSpacing.screenEdge),
    ) {
        Spacer(Modifier.height(MonoSpacing.sm))

        // Header row: overview label + sort / group / settings on the right edge.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            MonoLabel("— OVERVIEW", color = palette.accent)
            Spacer(Modifier.weight(1f))
            SortMenu(state.sortBy, onSortBy, palette.accent)
            GroupMenu(state.groupBy, onGroupBy, palette.accent)
            MonoIconButton(MonoGlyph.SETTINGS, "Settings", onOpenSettings, accent = palette.accent)
        }
        Spacer(Modifier.height(MonoSpacing.xs))
        Text("Library", style = MonoTypography.displaySmall, color = MonoColors.Fg1)
        Spacer(Modifier.height(MonoSpacing.lg))

        SearchField(state.search, onSearch, palette.accent)
        Spacer(Modifier.height(MonoSpacing.lg))
        EdgeGradientLine()

        when {
            !state.permissionGranted -> PermissionPrompt(onRequestPermission, palette.accent)
            state.flatTracks.isEmpty() && !state.loading -> EmptyState()
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                state.groups.forEach { group ->
                    val header = group.header
                    val isCollapsed = header != null && collapsed.contains(header)
                    header?.let { title ->
                        item(key = "h_$title") {
                            GroupHeader(
                                title = title,
                                count = group.tracks.size,
                                collapsed = isCollapsed,
                                accent = palette.accent,
                                onClick = {
                                    if (collapsed.contains(title)) collapsed.remove(title) else collapsed.add(title)
                                },
                            )
                        }
                    }
                    if (!isCollapsed) {
                        items(group.tracks, key = { it.id }) { track ->
                            SwipeableTrackRow(
                                track = track,
                                isActive = track.mediaId == state.activeTrackId,
                                onClick = { onTrackClick(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onPlayNext = { onPlayNext(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableTrackRow(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
) {
    val palette = LocalMonoPalette.current
    var offsetX by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(MonoRadius.sm))) {
        Row(
            Modifier.matchParentSize().background(if (offsetX >= 0) palette.accent.copy(alpha = 0.18f) else MonoColors.Fg3.copy(alpha = 0.12f)).padding(horizontal = MonoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offsetX >= 0) Arrangement.Start else Arrangement.End,
        ) {
            Text(
                if (offsetX >= 0) "PLAY NEXT" else "ADD TO QUEUE",
                style = MonoLabelStyle,
                color = if (offsetX >= 0) palette.accent else MonoColors.Fg2,
            )
        }
        TrackRow(
            track = track,
            isActive = isActive,
            onClick = onClick,
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.roundToInt(), 0) }
                .background(LocalMonoPalette.current.background)
                // Single-axis horizontal draggable cooperates with the LazyColumn's vertical
                // scroll (orientation-locked slop), so the list no longer sticks on diagonal drags.
                .draggable(
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-180f, 180f)
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        when {
                            offsetX > 96f -> onPlayNext()
                            offsetX < -96f -> onAddToQueue()
                        }
                        offsetX = 0f
                    },
                ),
        )
    }
}

@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    collapsed: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MonoSpacing.xxs, vertical = MonoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MonoSpacing.md),
    ) {
        MonoIconButton(
            glyph = if (collapsed) MonoGlyph.CARET_RIGHT else MonoGlyph.CARET_LEFT,
            contentDescription = if (collapsed) "Expand group" else "Collapse group",
            onClick = onClick,
            size = 48.dp,
            iconSize = 18.dp,
            bordered = true,
            accent = accent,
        )
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MonoLabelStyle, color = accent, maxLines = 1)
            Text("$count TRACKS", style = MonoTypography.bodySmall, color = MonoColors.Fg3, maxLines = 1)
        }
    }
}

@Composable
private fun SortMenu(current: SortBy, onSelect: (SortBy) -> Unit, accent: Color) {
    var open by remember { mutableStateOf(false) }
    Box {
        MonoIconButton(MonoGlyph.SORT, "Sort by", { open = true }, accent = accent)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortBy.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, color = if (option == current) accent else MonoColors.Fg1) },
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}

@Composable
private fun GroupMenu(current: GroupBy, onSelect: (GroupBy) -> Unit, accent: Color) {
    var open by remember { mutableStateOf(false) }
    Box {
        MonoIconButton(MonoGlyph.GROUP, "Group by", { open = true }, accent = accent)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            GroupBy.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("Group: ${option.label}", color = if (option == current) accent else MonoColors.Fg1) },
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, accent: Color) {
    val palette = LocalMonoPalette.current
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("Search…", color = MonoColors.Fg4) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(MonoRadius.sm)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = palette.panelElevated,
            unfocusedContainerColor = palette.panelElevated,
            focusedIndicatorColor = accent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = accent,
            focusedTextColor = MonoColors.Fg1,
            unfocusedTextColor = MonoColors.Fg1,
        ),
    )
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit, accent: Color) {
    CenteredMessage(
        title = "AUDIO ACCESS NEEDED",
        body = "MONO Signal plays the music already on your device. Grant audio access to scan your library.",
    ) {
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MonoColors.Void),
        ) { Text("GRANT ACCESS") }
    }
}

@Composable
private fun EmptyState() {
    CenteredMessage(
        title = "NO TRACKS FOUND",
        body = "No local audio matched. Add some music or clear the search.",
    )
}

@Composable
private fun CenteredMessage(title: String, body: String, action: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MonoSpacing.sm),
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            MonoLabel("— $title")
            Text(body, style = MonoTypography.bodyMedium, color = MonoColors.Fg3, textAlign = TextAlign.Center)
            action()
        }
    }
}
