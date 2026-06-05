package com.mono.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mono.signal.data.waveform.WaveformExtractor
import com.mono.signal.data.waveform.WaveformReducer
import com.mono.signal.model.NowPlayingGraphic
import com.mono.signal.model.PlaybackState
import com.mono.signal.model.VisualizerFrame
import com.mono.signal.playback.AudioVisualizer
import com.mono.signal.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowPlayingUiState(
    val playback: PlaybackState = PlaybackState.Empty,
    val frame: VisualizerFrame = VisualizerFrame.empty(),
    val waveformEnvelope: FloatArray = FloatArray(WaveformReducer.DEFAULT_BUCKETS),
    val graphic: NowPlayingGraphic = NowPlayingGraphic.ALBUM_ART,
    val visualizerActive: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NowPlayingUiState) return false
        return playback == other.playback && frame == other.frame &&
            waveformEnvelope.contentEquals(other.waveformEnvelope) && graphic == other.graphic &&
            visualizerActive == other.visualizerActive
    }

    override fun hashCode(): Int {
        var result = playback.hashCode()
        result = 31 * result + frame.hashCode()
        result = 31 * result + waveformEnvelope.contentHashCode()
        result = 31 * result + graphic.hashCode()
        result = 31 * result + visualizerActive.hashCode()
        return result
    }
}

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val visualizer: AudioVisualizer,
    private val waveformExtractor: WaveformExtractor,
) : ViewModel() {

    private val graphic = MutableStateFlow(NowPlayingGraphic.ALBUM_ART)
    private val envelope = MutableStateFlow(FloatArray(WaveformReducer.DEFAULT_BUCKETS))

    val uiState: StateFlow<NowPlayingUiState> = combine(
        playerController.state,
        visualizer.frames,
        envelope,
        graphic,
        visualizer.active,
    ) { playback, frame, env, gfx, active ->
        NowPlayingUiState(playback, frame, env, gfx, active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

    init {
        // Decode the real amplitude envelope whenever the active track changes.
        viewModelScope.launch {
            playerController.state
                .map { it.currentTrack }
                .distinctUntilChanged { a, b -> a?.id == b?.id }
                .collect { track ->
                    envelope.value = FloatArray(WaveformReducer.DEFAULT_BUCKETS)
                    if (track != null) {
                        envelope.value = waveformExtractor.envelopeFor(track)
                    }
                }
        }
    }

    fun cycleGraphic() { graphic.value = graphic.value.next() }

    /** Called after the user grants microphone access so the live graphs start capturing. */
    fun retryVisualizer() = playerController.retryVisualizer()

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seek(fraction: Float) = playerController.seekToFraction(fraction)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeat() = playerController.cycleRepeat()
}
