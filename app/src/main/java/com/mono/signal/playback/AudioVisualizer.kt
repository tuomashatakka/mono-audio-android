package com.mono.signal.playback

import com.mono.signal.model.VisualizerFrame
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-facing source of live [VisualizerFrame]s. Historically this wrapped Android's
 * [android.media.audiofx.Visualizer]; it now simply re-exposes the [PcmAudioTap], which reads the
 * player's real stereo PCM output. That removes the `RECORD_AUDIO` requirement and lifts the
 * Visualizer's ~20Hz / mono / 8-bit limits, so the live graphs get true L/R waveforms, a real
 * 1024-point FFT, and a 60Hz refresh.
 *
 * The session-based [start]/[release] hooks are retained as no-ops so existing wiring keeps
 * compiling — the tap is attached to the player's audio sink in [PlaybackService] instead.
 */
@Singleton
class AudioVisualizer @Inject constructor(private val tap: PcmAudioTap) {

    /** Live frames (smoothed FFT bands + mono/L/R waveform points). */
    val frames: StateFlow<VisualizerFrame> = tap.frames

    /** True while audio is flowing through the tap (i.e. the live graphs have fresh data). */
    val active: StateFlow<Boolean> = tap.active

    /** No-op: the tap is wired into the player's audio sink, not attached per session. */
    fun start(sessionId: Int) = Unit

    /** No-op: the tap lives for the process; nothing to release here. */
    fun release() = Unit
}
