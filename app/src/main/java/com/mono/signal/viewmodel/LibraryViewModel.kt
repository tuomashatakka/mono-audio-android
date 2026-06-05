package com.mono.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mono.signal.data.MusicRepository
import com.mono.signal.model.GroupBy
import com.mono.signal.model.SortBy
import com.mono.signal.model.Track
import com.mono.signal.model.TrackGroup
import com.mono.signal.model.arrange
import com.mono.signal.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mono.signal.model.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val groups: List<TrackGroup> = emptyList(),
    val flatTracks: List<Track> = emptyList(),
    val activeTrackId: String? = null,
    val permissionGranted: Boolean = false,
    val loading: Boolean = false,
    val sortBy: SortBy = SortBy.TITLE,
    val groupBy: GroupBy = GroupBy.NONE,
    val search: String = "",
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val permissionGranted = MutableStateFlow(false)
    private val loading = MutableStateFlow(false)
    private val sortBy = MutableStateFlow(SortBy.TITLE)
    private val groupBy = MutableStateFlow(GroupBy.NONE)
    private val search = MutableStateFlow("")

    private data class Query(
        val sort: SortBy,
        val group: GroupBy,
        val granted: Boolean,
        val loading: Boolean,
        val search: String,
    )

    private val query = combine(sortBy, groupBy, permissionGranted, loading, search, ::Query)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.tracks,
        playerController.state,
        query,
    ) { tracks: List<Track>, playback: PlaybackState, q: Query ->
        val filtered = if (q.search.isBlank()) tracks else tracks.filter {
            it.title.contains(q.search, true) || it.artist.contains(q.search, true) ||
                it.album.contains(q.search, true)
        }
        val groups = filtered.arrange(q.sort, q.group)
        LibraryUiState(
            groups = groups,
            flatTracks = groups.flatMap { it.tracks },
            activeTrackId = playback.currentTrack?.mediaId,
            permissionGranted = q.granted,
            loading = q.loading,
            sortBy = q.sort,
            groupBy = q.group,
            search = q.search,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        playerController.connect()
    }

    fun onPermissionResult(granted: Boolean) {
        permissionGranted.value = granted
        if (granted) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            repository.refresh()
            loading.value = false
        }
    }

    fun setSortBy(value: SortBy) { sortBy.value = value }
    fun setGroupBy(value: GroupBy) { groupBy.value = value }
    fun setSearch(value: String) { search.value = value }

    /** Plays from the currently displayed (arranged) ordering. */
    fun onTrackClick(track: Track) {
        val ordered = uiState.value.flatTracks
        val index = ordered.indexOfFirst { it.id == track.id }
        if (index >= 0) playerController.play(ordered, index)
    }
}
