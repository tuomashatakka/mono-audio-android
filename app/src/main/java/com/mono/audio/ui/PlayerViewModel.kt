package com.mono.audio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mono.audio.data.AudioTrack
import com.mono.audio.data.LocalAudioRepository
import com.mono.audio.player.PlayerController
import com.mono.audio.player.VisualizerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalAudioRepository(application)
    private val player = PlayerController(application)
    private val visualizer = VisualizerEngine()
    private var visualizerJob: Job? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            player.playbackEvents().collect {
                _uiState.update { it.copy(isPlaying = player.isPlaying()) }
                startVisualizer()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(positionMs = player.positionMs) }
                delay(250)
            }
        }
    }

    fun onPermissionsChanged(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
        if (granted) refreshLibrary()
    }

    fun refreshLibrary() {
        val tracks = repository.loadTracks()
        _uiState.update { state ->
            state.copy(
                tracks = tracks,
                selectedTrack = state.selectedTrack ?: tracks.firstOrNull(),
            )
        }
    }

    fun play(track: AudioTrack) {
        _uiState.update { it.copy(selectedTrack = track, positionMs = 0L, isPlaying = true) }
        player.setTrack(track)
    }

    fun togglePlayPause() {
        val selected = _uiState.value.selectedTrack
        if (selected != null && player.audioSessionId == 0) {
            play(selected)
        } else {
            player.toggle()
        }
        _uiState.update { it.copy(isPlaying = player.isPlaying()) }
    }

    fun seekTo(progress: Float) {
        val duration = _uiState.value.selectedTrack?.durationMs ?: return
        player.seekTo((duration * progress.coerceIn(0f, 1f)).toLong())
        _uiState.update { it.copy(positionMs = player.positionMs) }
    }

    private fun startVisualizer() {
        val session = player.audioSessionId
        if (session == 0 || visualizerJob?.isActive == true) return
        visualizerJob = viewModelScope.launch {
            visualizer.fft(session).collectLatest { bands ->
                if (bands.isNotEmpty()) _uiState.update { it.copy(fftBands = bands) }
            }
        }
    }

    override fun onCleared() {
        visualizerJob?.cancel()
        player.release()
        super.onCleared()
    }
}
