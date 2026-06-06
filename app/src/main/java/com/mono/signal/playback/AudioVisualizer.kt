package com.mono.signal.playback

import android.media.audiofx.Visualizer
import android.util.Log
import com.mono.signal.data.ThemePreferences
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
class AudioVisualizer @Inject constructor(
    private val themePreferences: ThemePreferences,
) {

    private val _frames = MutableStateFlow(VisualizerFrame.empty())
    val frames: StateFlow<VisualizerFrame> = _frames.asStateFlow()

    /** True while a Visualizer is attached and capturing (i.e. the live graphs have data). */
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private var visualizer: Visualizer? = null
    private var currentSession = 0
    private var smoothedPeakBands = FloatArray(VisualizerDsp.DEFAULT_BANDS)
    private var smoothedRmsBands = FloatArray(VisualizerDsp.DEFAULT_BANDS)
    private var lastWaveform = FloatArray(VisualizerDsp.DEFAULT_WAVE_POINTS)

    @Synchronized
    fun start(sessionId: Int) {
        if (sessionId == 0 || sessionId == currentSession) return
        release()
        runCatching {
            visualizer = Visualizer(sessionId).apply {
                captureSize = validCaptureSize(themePreferences.config.value.fftBlockSize)
                val rate = Visualizer.getMaxCaptureRate()
                setDataCaptureListener(listener, rate, true, true)
                enabled = true
            }
            currentSession = sessionId
            _active.value = true
        }.onFailure {
            Log.w(TAG, "Visualizer unavailable for session $sessionId", it)
            _active.value = false
        }
    }

    @Synchronized
    fun release() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        currentSession = 0
        _active.value = false
        smoothedPeakBands = FloatArray(VisualizerDsp.DEFAULT_BANDS)
        smoothedRmsBands = FloatArray(VisualizerDsp.DEFAULT_BANDS)
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
            val target = VisualizerDsp.fftToPeakAndRmsBands(fft)
            smoothedPeakBands = VisualizerDsp.smooth(smoothedPeakBands, target.peak)
            smoothedRmsBands = VisualizerDsp.smooth(smoothedRmsBands, target.rms, attack = 0.42f, decay = 0.18f)
            emit()
        }
    }

    private fun emit() {
        _frames.value = VisualizerFrame(
            fftBands = smoothedPeakBands.copyOf(),
            waveform = lastWaveform.copyOf(),
            fftPeakBands = smoothedPeakBands.copyOf(),
            fftRmsBands = smoothedRmsBands.copyOf(),
        )
    }

    private fun validCaptureSize(requested: Int): Int {
        val range = Visualizer.getCaptureSizeRange()
        val min = range[0]
        val max = range[1]
        return listOf(512, 1024, 2048, 4096, 8192)
            .filter { it in min..max }
            .minByOrNull { kotlin.math.abs(it - requested) }
            ?: max
    }

    private companion object {
        const val TAG = "AudioVisualizer"
    }
}
