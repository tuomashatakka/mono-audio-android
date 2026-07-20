package com.mono.signal.playback.dsp

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * One second-order peaking-EQ section (RBJ Audio EQ Cookbook), processed in transposed direct
 * form II. Holds per-channel filter state, so each channel of a band needs its own instance.
 */
class Biquad {

    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    private var z1 = 0f
    private var z2 = 0f

    /**
     * Configures the section as a peaking EQ centered on [freqHz] with [gainDb] boost/cut. The
     * center frequency is clamped below Nyquist so the top band stays stable at low sample rates.
     */
    fun setPeaking(sampleRate: Int, freqHz: Float, gainDb: Float, q: Float) {
        val fc = minOf(freqHz, 0.45f * sampleRate).toDouble()
        val amp = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * fc / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val a0 = 1.0 + alpha / amp
        b0 = ((1.0 + alpha * amp) / a0).toFloat()
        b1 = (-2.0 * cosW0 / a0).toFloat()
        b2 = ((1.0 - alpha * amp) / a0).toFloat()
        a1 = (-2.0 * cosW0 / a0).toFloat()
        a2 = ((1.0 - alpha / amp) / a0).toFloat()
    }

    fun processInPlace(samples: FloatArray, count: Int) {
        var s1 = z1
        var s2 = z2
        for (i in 0 until count) {
            val x = samples[i]
            val y = b0 * x + s1
            s1 = b1 * x - a1 * y + s2
            s2 = b2 * x - a2 * y
            samples[i] = y
        }
        z1 = s1
        z2 = s2
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
    }
}
