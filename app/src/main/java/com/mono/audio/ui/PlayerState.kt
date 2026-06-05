package com.mono.audio.ui

import com.mono.audio.data.AudioTrack

data class PlayerUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val selectedTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val fftBands: List<Float> = List(36) { 0.08f },
    val hasAudioPermission: Boolean = false,
) {
    val progress: Float
        get() = selectedTrack?.durationMs?.takeIf { it > 0 }?.let { (positionMs.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
}
