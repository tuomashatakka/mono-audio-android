package com.mono.signal.playback

import com.mono.signal.model.VisualizerFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure, Android-free transforms for the raw byte buffers produced by
 * [android.media.audiofx.Visualizer]. Kept separate so the math is unit-testable.
 */
object VisualizerDsp {

    data class SpectrumBands(val peak: FloatArray, val rms: FloatArray)

    const val DEFAULT_BANDS = 48
    const val DEFAULT_WAVE_POINTS = 96


    /** True when a captured or synthesized frame has visible waveform/FFT energy. */
    fun hasSignal(frame: VisualizerFrame, threshold: Float = 0.001f): Boolean =
        frame.fftBands.any { it > threshold } || frame.waveform.any { abs(it) > threshold }

    /**
     * Build a deterministic visualizer frame from the decoded track envelope. This is used as
     * a graceful fallback on devices/paths where Android's live [android.media.audiofx.Visualizer]
     * attaches successfully but only emits silence (for example some offloaded playback routes).
     * The waveform is a short bipolar slice around [progress], while the bars fold nearby
     * envelope energy into a spectrum-like display so the Now Playing graphics never collapse
     * to all-zero values for a non-silent track.
     */
    fun frameFromEnvelope(
        envelope: FloatArray,
        progress: Float,
        bands: Int = DEFAULT_BANDS,
        points: Int = DEFAULT_WAVE_POINTS,
    ): VisualizerFrame {
        if (envelope.isEmpty() || envelope.all { it <= 0f }) {
            return VisualizerFrame.empty(bands, points)
        }

        val safeProgress = progress.coerceIn(0f, 1f)
        val waveform = waveformFromEnvelope(envelope, safeProgress, points)
        val fftBands = bandsFromEnvelope(envelope, safeProgress, bands)
        return VisualizerFrame(fftBands, waveform)
    }

    private fun waveformFromEnvelope(envelope: FloatArray, progress: Float, points: Int): FloatArray {
        val out = FloatArray(points)
        if (points <= 0) return out
        val window = 0.10f
        val phaseOffset = progress * PI.toFloat() * 12f
        for (i in 0 until points) {
            val fraction = if (points == 1) 0.5f else i / (points - 1f)
            val sampleProgress = (progress + (fraction - 0.5f) * window).coerceIn(0f, 1f)
            val amp = sampleEnvelope(envelope, sampleProgress)
            val carrier = sin(fraction * PI.toFloat() * 8f + phaseOffset)
            out[i] = (amp * carrier).coerceIn(-1f, 1f)
        }
        return out
    }

    private fun bandsFromEnvelope(envelope: FloatArray, progress: Float, bands: Int): FloatArray {
        val out = FloatArray(bands)
        if (bands <= 0) return out
        val window = 0.18f
        for (b in 0 until bands) {
            val t = if (bands == 1) 0.5f else b / (bands - 1f)
            val logT = logLerp(1.0, 16.0, t.toDouble()).toFloat() / 16f
            val sampleProgress = (progress + (logT - 0.5f) * window).coerceIn(0f, 1f)
            val local = sampleEnvelope(envelope, sampleProgress)
            val rolloff = (1f - t * 0.55f).coerceIn(0.35f, 1f)
            val ripple = 0.82f + 0.18f * sin((progress * 10f + b * 0.37f) * PI.toFloat()).let { abs(it) }
            out[b] = (local.pow(0.72f) * rolloff * ripple).coerceIn(0f, 1f)
        }
        return normalizeUnit(out)
    }

    private fun sampleEnvelope(envelope: FloatArray, progress: Float): Float {
        if (envelope.isEmpty()) return 0f
        val x = progress.coerceIn(0f, 1f) * (envelope.size - 1)
        val left = x.toInt().coerceIn(0, envelope.size - 1)
        val right = (left + 1).coerceIn(0, envelope.size - 1)
        val mix = x - left
        return (envelope[left] + (envelope[right] - envelope[left]) * mix).coerceIn(0f, 1f)
    }

    private fun normalizeUnit(values: FloatArray): FloatArray {
        var peak = 0f
        for (value in values) if (value > peak) peak = value
        if (peak <= 0f) return values
        for (i in values.indices) values[i] = (values[i] / peak).coerceIn(0f, 1f)
        return values
    }

    /**
     * Convert a Visualizer FFT buffer to normalized (0..1) log-spaced band magnitudes.
     *
     * Buffer layout (per the Android Visualizer contract): index 0 is the DC real part,
     * index 1 is the Nyquist real part, and thereafter pairs (2k, 2k+1) are the real and
     * imaginary parts of bin k.
     */
    fun fftToBands(fft: ByteArray, bands: Int = DEFAULT_BANDS): FloatArray = fftToPeakAndRmsBands(fft, bands).peak

    /** Convert a Visualizer FFT buffer into separate peak and RMS band traces. */
    fun fftToPeakAndRmsBands(fft: ByteArray, bands: Int = DEFAULT_BANDS): SpectrumBands {
        val peakOut = FloatArray(bands)
        val rmsOut = FloatArray(bands)
        if (fft.size < 4) return SpectrumBands(peakOut, rmsOut)

        val binCount = fft.size / 2
        val magnitudes = FloatArray(binCount)
        for (k in 1 until binCount) {
            val re = fft[2 * k].toFloat()
            val im = fft[2 * k + 1].toFloat()
            magnitudes[k] = hypot(re, im)
        }

        // Log-spaced band edges across usable bins [1, binCount).
        val minBin = 1.0
        val maxBin = binCount.toDouble()
        for (b in 0 until bands) {
            val lo = logLerp(minBin, maxBin, b.toDouble() / bands)
            val hi = logLerp(minBin, maxBin, (b + 1.0) / bands)
            val from = lo.toInt().coerceIn(1, binCount - 1)
            val to = hi.toInt().coerceIn(from + 1, binCount)
            var peak = 0f
            var sumSquares = 0f
            var count = 0
            for (k in from until to) {
                val magnitude = magnitudes[k]
                if (magnitude > peak) peak = magnitude
                sumSquares += magnitude * magnitude
                count++
            }
            val rms = kotlin.math.sqrt(sumSquares / count.coerceAtLeast(1))
            peakOut[b] = (ln(1f + peak) / LOG_SCALE).coerceIn(0f, 1f)
            rmsOut[b] = (ln(1f + rms) / LOG_SCALE).coerceIn(0f, 1f)
        }
        return SpectrumBands(peakOut, rmsOut)
    }

    /**
     * Convert a Visualizer waveform buffer (unsigned 8-bit PCM, centered at 128) to
     * normalized (-1..1) samples downsampled to [points] values.
     */
    fun waveformToFloats(waveform: ByteArray, points: Int = DEFAULT_WAVE_POINTS): FloatArray {
        val out = FloatArray(points)
        if (waveform.isEmpty()) return out
        val step = max(1, waveform.size / points)
        for (i in 0 until points) {
            val src = (i * step).coerceIn(0, waveform.size - 1)
            // Bytes arrive signed; the Visualizer treats them as unsigned PCM8.
            val unsigned = waveform[src].toInt() and 0xFF
            out[i] = (unsigned - 128) / 128f
        }
        return out
    }

    /** Per-band smoothing: fast attack on rising values, slower decay on falling ones. */
    fun smooth(previous: FloatArray, target: FloatArray, attack: Float = 0.5f, decay: Float = 0.12f): FloatArray {
        if (previous.size != target.size) return target.copyOf()
        val out = FloatArray(target.size)
        for (i in target.indices) {
            val p = previous[i]
            val t = target[i]
            val rate = if (t > p) attack else decay
            out[i] = p + (t - p) * rate
        }
        return out
    }

    private fun logLerp(lo: Double, hi: Double, t: Double): Double =
        lo * (hi / lo).pow(t)

    private const val LOG_SCALE = 5.5f
}
