package com.mono.signal.model

/** Repeat modes mirrored from Media3, kept UI-agnostic. */
enum class RepeatMode { OFF, ONE, ALL }

/**
 * Immutable snapshot of everything the UI needs to render transport + progress.
 * Produced by [com.mono.signal.playback.PlayerController]; consumed read-only by Composables.
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
) {
    val hasTrack: Boolean get() = currentTrack != null

    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    companion object {
        val Empty = PlaybackState()
    }
}
