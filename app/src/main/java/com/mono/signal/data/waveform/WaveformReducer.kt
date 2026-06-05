package com.mono.signal.data.waveform

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure, Android-free DSP helpers for turning decoded PCM into a compact amplitude
 * envelope. Factored out so it can be unit-tested on the plain JVM.
 */
object WaveformReducer {

    /** Default number of buckets (bars) in a rendered envelope. */
    const val DEFAULT_BUCKETS = 256

    /**
     * Streaming accumulator: feed it 16-bit PCM chunks as they are decoded, then
     * call [build] once. Keeps one running peak per bucket so memory stays flat
     * regardless of track length.
     */
    class Accumulator(
        private val totalFrames: Long,
        private val buckets: Int = DEFAULT_BUCKETS,
    ) {
        private val peaks = FloatArray(buckets)
        private val safeTotal = max(1L, totalFrames)
        private var frameIndex = 0L

        /** @param samples interleaved (or mono) 16-bit samples for this chunk. */
        fun add(samples: ShortArray, length: Int, channels: Int) {
            val step = max(1, channels)
            var i = 0
            while (i < length) {
                // Average channels to mono so stereo files don't double-count.
                var sum = 0
                var c = 0
                while (c < step && i + c < length) {
                    sum += samples[i + c].toInt()
                    c++
                }
                val mono = sum / step
                val bucket = ((frameIndex * buckets) / safeTotal)
                    .toInt().coerceIn(0, buckets - 1)
                val amp = abs(mono) / 32768f
                if (amp > peaks[bucket]) peaks[bucket] = amp
                frameIndex++
                i += step
            }
        }

        fun build(): FloatArray = normalize(peaks)
    }

    /** Scale peaks so the loudest bucket reaches 1.0; leaves silence as zeros. */
    fun normalize(peaks: FloatArray): FloatArray {
        var maxPeak = 0f
        for (p in peaks) if (p > maxPeak) maxPeak = p
        if (maxPeak <= 0f) return peaks
        val out = FloatArray(peaks.size)
        for (i in peaks.indices) out[i] = (peaks[i] / maxPeak).coerceIn(0f, 1f)
        return out
    }
}
