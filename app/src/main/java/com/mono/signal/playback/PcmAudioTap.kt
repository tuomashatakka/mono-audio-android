package com.mono.signal.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.mono.signal.model.VisualizerFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Taps the player's real PCM output as a [TeeAudioProcessor.AudioBufferSink] and turns it into a
 * live [VisualizerFrame] stream. Reading the player's own decoded samples (rather than Android's
 * 8-bit, mono-downmixed [android.media.audiofx.Visualizer]) gives us three things the framework
 * Visualizer can't:
 *
 *  - true stereo, so the left/right waveforms — and their phase difference — can be drawn apart,
 *  - a choosable FFT size (1024) for real frequency resolution instead of an 8-bit byte spectrum,
 *  - an update rate bounded only by the audio buffer cadence, well above the Visualizer's ~20Hz.
 *
 * The audio callback ([handleBuffer]) only copies samples into a ring buffer — cheap and
 * allocation-free — while a separate coroutine renders frames at ~60Hz off the audio thread, so
 * the FFT never risks an audio glitch. It also means no `RECORD_AUDIO` permission is required.
 */
@Singleton
class PcmAudioTap @Inject constructor() : TeeAudioProcessor.AudioBufferSink {

    private val _frames = MutableStateFlow(VisualizerFrame.empty())
    val frames: StateFlow<VisualizerFrame> = _frames.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    // Stereo ring buffers. Capacity is a couple of FFT windows so the render loop always has a
    // full, recent slice to analyse regardless of how the decoder chunks its output.
    private val capacity = WINDOW * 2
    private val left = FloatArray(capacity)
    private val right = FloatArray(capacity)
    private var writeIndex = 0
    private var filled = 0

    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT
    @Volatile private var lastBufferNanos = 0L

    // Band smoothing lives across frames (fast attack, slow decay) to keep the spectrum readable.
    private var smoothedPeak = FloatArray(VisualizerDsp.DEFAULT_BANDS)
    private var smoothedRms = FloatArray(VisualizerDsp.DEFAULT_BANDS)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            while (isActive) {
                val sinceBuffer = System.nanoTime() - lastBufferNanos
                if (sinceBuffer < IDLE_NANOS && filled > 0) {
                    _active.value = true
                    _frames.value = renderFrame()
                    delay(FRAME_MILLIS)
                } else {
                    if (_active.value) _active.value = false
                    delay(IDLE_FRAME_MILLIS)
                }
            }
        }
    }

    @Synchronized
    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        writeIndex = 0
        filled = 0
        left.fill(0f)
        right.fill(0f)
    }

    @Synchronized
    override fun handleBuffer(buffer: ByteBuffer) {
        val buf = buffer.order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            C.ENCODING_PCM_16BIT -> readPcm16(buf)
            C.ENCODING_PCM_FLOAT -> readPcmFloat(buf)
            else -> return // 8/24/32-bit are rare for music output; leave the last frame in place.
        }
        lastBufferNanos = System.nanoTime()
    }

    private fun readPcm16(buf: ByteBuffer) {
        val ch = channelCount
        while (buf.remaining() >= 2 * ch) {
            val l = buf.getShort().toInt() / 32768f
            var r = l
            if (ch >= 2) {
                r = buf.getShort().toInt() / 32768f
                for (c in 2 until ch) buf.getShort() // fold any extra channels away
            }
            push(l, r)
        }
    }

    private fun readPcmFloat(buf: ByteBuffer) {
        val ch = channelCount
        while (buf.remaining() >= 4 * ch) {
            val l = buf.getFloat().coerceIn(-1f, 1f)
            var r = l
            if (ch >= 2) {
                r = buf.getFloat().coerceIn(-1f, 1f)
                for (c in 2 until ch) buf.getFloat()
            }
            push(l, r)
        }
    }

    private fun push(l: Float, r: Float) {
        left[writeIndex] = l
        right[writeIndex] = r
        writeIndex = (writeIndex + 1) % capacity
        if (filled < capacity) filled++
    }

    /** Snapshot the most recent [WINDOW] samples of both channels in chronological order. */
    @Synchronized
    private fun snapshot(outLeft: FloatArray, outRight: FloatArray) {
        val n = minOf(WINDOW, filled)
        // Pad the front with silence when we don't yet have a full window.
        val pad = WINDOW - n
        for (i in 0 until pad) {
            outLeft[i] = 0f
            outRight[i] = 0f
        }
        val start = ((writeIndex - n) % capacity + capacity) % capacity
        for (i in 0 until n) {
            val idx = (start + i) % capacity
            outLeft[pad + i] = left[idx]
            outRight[pad + i] = right[idx]
        }
    }

    private val snapLeft = FloatArray(WINDOW)
    private val snapRight = FloatArray(WINDOW)
    private val mono = FloatArray(WINDOW)

    private fun renderFrame(): VisualizerFrame {
        snapshot(snapLeft, snapRight)
        for (i in 0 until WINDOW) mono[i] = (snapLeft[i] + snapRight[i]) * 0.5f

        val spectrum = VisualizerDsp.spectrumBands(mono, WINDOW, VisualizerDsp.DEFAULT_BANDS)
        smoothedPeak = VisualizerDsp.smooth(smoothedPeak, spectrum.peak)
        smoothedRms = VisualizerDsp.smooth(smoothedRms, spectrum.rms, attack = 0.42f, decay = 0.18f)

        val waveLeft = VisualizerDsp.downsampleWaveform(snapLeft, WAVE_POINTS)
        val waveRight = VisualizerDsp.downsampleWaveform(snapRight, WAVE_POINTS)
        val waveMono = VisualizerDsp.downsampleWaveform(mono, WAVE_POINTS)

        return VisualizerFrame(
            fftBands = smoothedPeak.copyOf(),
            waveform = waveMono,
            fftPeakBands = smoothedPeak.copyOf(),
            fftRmsBands = smoothedRms.copyOf(),
            waveformLeft = waveLeft,
            waveformRight = waveRight,
        )
    }

    private companion object {
        const val WINDOW = VisualizerDsp.DEFAULT_FFT_SIZE // analysis + waveform window (samples)
        const val WAVE_POINTS = VisualizerDsp.DEFAULT_WAVE_POINTS
        const val FRAME_MILLIS = 16L // ~60Hz render while audio is flowing
        const val IDLE_FRAME_MILLIS = 200L // back off when nothing is playing
        const val IDLE_NANOS = 300_000_000L // treat the tap as inactive after 300ms of silence
    }
}
