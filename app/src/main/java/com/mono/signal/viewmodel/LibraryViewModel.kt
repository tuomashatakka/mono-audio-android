package com.mono.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mono.signal.data.MusicRepository
import com.mono.signal.model.Track
import com.mono.signal.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val activeTrackId: String? = null,
    val permissionGranted: Boolean = false,
    val loading: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val permissionGranted = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val loading = kotlinx.coroutines.flow.MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.tracks,
        playerController.state,
        permissionGranted,
        loading,
    ) { tracks, playback, granted, isLoading ->
        LibraryUiState(
            tracks = tracks,
            activeTrackId = playback.currentTrack?.mediaId,
            permissionGranted = granted,
            loading = isLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        playerController.connect()
    }

    /** Called once the audio-read permission is confirmed granted. */
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

    fun onTrackClick(index: Int) {
        val tracks = repository.tracks.value
        if (index in tracks.indices) playerController.play(tracks, index)
    }
}
