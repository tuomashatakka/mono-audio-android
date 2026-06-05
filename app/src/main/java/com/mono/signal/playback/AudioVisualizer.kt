package com.mono.signal.playback

import android.media.audiofx.Visualizer
import android.util.Log
import com.mono.signal.model.VisualizerFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over [android.media.audiofx.Visualizer]. Attaches to the player's audio
 * session and exposes live [VisualizerFrame]s (smoothed FFT bands + waveform points).
 *
 * Requires the RECORD_AUDIO permission. Fails soft: if the Visualizer can't be created
 * (no permission, unsupported device), the frame flow simply stays empty.
 */
@Singleton
class AudioVisualizer @Inject constructor() {

    private val _frames = MutableStateFlow(VisualizerFrame.empty())
    val frames: StateFlow<VisualizerFrame> = _frames.asStateFlow()

    private var visualizer: Visualizer? = null
    private var currentSession = 0
    private var smoothedBands = FloatArray(VisualizerDsp.DEFAULT_BANDS)
    private var lastWaveform = FloatArray(VisualizerDsp.DEFAULT_WAVE_POINTS)

    @Synchronized
    fun start(sessionId: Int) {
        if (sessionId == 0 || sessionId == currentSession) return
        release()
        runCatching {
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                val rate = Visualizer.getMaxCaptureRate()
                setDataCaptureListener(listener, rate, true, true)
                enabled = true
            }
            currentSession = sessionId
        }.onFailure { Log.w(TAG, "Visualizer unavailable for session $sessionId", it) }
    }

    @Synchronized
    fun release() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        currentSession = 0
        smoothedBands = FloatArray(VisualizerDsp.DEFAULT_BANDS)
        lastWaveform = FloatArray(VisualizerDsp.DEFAULT_WAVE_POINTS)
        _frames.value = VisualizerFrame.empty()
    }

    private val listener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
            waveform ?: return
            lastWaveform = VisualizerDsp.waveformToFloats(waveform)
            emit()
        }

        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
            fft ?: return
            val target = VisualizerDsp.fftToBands(fft)
            smoothedBands = VisualizerDsp.smooth(smoothedBands, target)
            emit()
        }
    }

    private fun emit() {
        _frames.value = VisualizerFrame(smoothedBands.copyOf(), lastWaveform.copyOf())
    }

    private companion object {
        const val TAG = "AudioVisualizer"
    }
}
