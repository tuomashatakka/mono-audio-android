package com.mono.signal.playback

import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * Pure, Android-free transforms for the raw byte buffers produced by
 * [android.media.audiofx.Visualizer]. Kept separate so the math is unit-testable.
 */
object VisualizerDsp {

    const val DEFAULT_BANDS = 48
    const val DEFAULT_WAVE_POINTS = 64

    /**
     * Convert a Visualizer FFT buffer to normalized (0..1) log-spaced band magnitudes.
     *
     * Buffer layout (per the Android Visualizer contract): index 0 is the DC real part,
     * index 1 is the Nyquist real part, and thereafter pairs (2k, 2k+1) are the real and
     * imaginary parts of bin k.
     */
    fun fftToBands(fft: ByteArray, bands: Int = DEFAULT_BANDS): FloatArray {
        val out = FloatArray(bands)
        if (fft.size < 4) return out

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
            for (k in from until to) if (magnitudes[k] > peak) peak = magnitudes[k]
            // Log-compress magnitude into a pleasant 0..1 range.
            out[b] = (ln(1f + peak) / LOG_SCALE).coerceIn(0f, 1f)
        }
        return out
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
