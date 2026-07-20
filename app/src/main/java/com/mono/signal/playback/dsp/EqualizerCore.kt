package com.mono.signal.playback.dsp

import com.mono.signal.model.DspConfig
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The 12-band peaking-EQ bank behind the DSP screen's faders. Band centers come from
 * [DspConfig.EQ_FREQUENCIES]; each band's Q is derived from the spacing to its neighbors (band
 * edges at the geometric means of adjacent centers), so the irregular layout — narrow steps in the
 * bass, octave-wide steps up top — gets sensible bandwidths without bands fighting each other.
 *
 * Pure Kotlin so it can be unit tested on the JVM without Android or Media3 on the classpath.
 */
class EqualizerCore(private val channelCount: Int) {

    private val filters = Array(channelCount) { Array(BAND_COUNT) { Biquad() } }
    private val bandActive = BooleanArray(BAND_COUNT)

    /** Recomputes all band coefficients for [sampleRate]. Bands within ±[MIN_ACTIVE_DB] are skipped. */
    fun configure(sampleRate: Int, gainsDb: List<Float>) {
        for (band in 0 until BAND_COUNT) {
            val gain = gainsDb.getOrElse(band) { 0f }
            val wasActive = bandActive[band]
            val active = abs(gain) >= MIN_ACTIVE_DB
            bandActive[band] = active
            if (!active) continue
            val q = bandQ(band)
            for (ch in 0 until channelCount) {
                // A band re-entering the chain must not ring with state from its previous stint.
                if (!wasActive) filters[ch][band].reset()
                filters[ch][band].setPeaking(sampleRate, DspConfig.EQ_FREQUENCIES[band], gain, q)
            }
        }
    }

    fun process(channel: Int, samples: FloatArray, count: Int) {
        val bank = filters[channel]
        for (band in 0 until BAND_COUNT) {
            if (bandActive[band]) bank[band].processInPlace(samples, count)
        }
    }

    fun reset() {
        for (bank in filters) for (filter in bank) filter.reset()
    }

    private fun bandQ(band: Int): Float {
        val freqs = DspConfig.EQ_FREQUENCIES
        val fc = freqs[band]
        // Band edges sit at the geometric means of adjacent centers, mirrored at the extremes.
        val fHi = if (band < freqs.lastIndex) sqrt(fc * freqs[band + 1]) else fc * fc / sqrt(freqs[band - 1] * fc)
        val fLo = if (band > 0) sqrt(freqs[band - 1] * fc) else fc * fc / fHi
        return (fc / (fHi - fLo)).coerceIn(MIN_Q, MAX_Q)
    }

    companion object {
        val BAND_COUNT = DspConfig.EQ_FREQUENCIES.size
        const val MIN_ACTIVE_DB = 0.05f
        const val MIN_Q = 0.5f
        const val MAX_Q = 4f
    }
}
