package com.mono.signal

import com.mono.signal.playback.dsp.LimiterCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

class LimiterCoreTest {

    private val sampleRate = 48_000

    private fun sine(freqHz: Float, samples: Int, amplitude: Float): FloatArray =
        FloatArray(samples) { i -> amplitude * sin(2.0 * PI * freqHz * i / sampleRate).toFloat() }

    private fun limit(
        input: FloatArray,
        thresholdDb: Float,
        gainDb: Float = 0f,
        releaseMs: Float = 120f,
    ): FloatArray {
        val limiter = LimiterCore()
        limiter.configure(sampleRate, thresholdDb, gainDb, releaseMs)
        val left = input.copyOf()
        val right = input.copyOf()
        limiter.processStereo(left, right, left.size)
        return left
    }

    private fun maxAbs(samples: FloatArray): Float {
        var peak = 0f
        for (s in samples) peak = maxOf(peak, abs(s))
        return peak
    }

    @Test
    fun outputNeverExceedsThreshold_fromTheVeryFirstSample() {
        val ceiling = 10f.pow(-6f / 20f)
        val output = limit(sine(1000f, sampleRate, amplitude = 1f), thresholdDb = -6f)
        // Instant attack: no overshoot anywhere, including the first over-threshold samples.
        assertTrue(maxAbs(output) <= ceiling * 1.001f)
        // And the limiter actually engages rather than muting everything.
        assertEquals(ceiling, maxAbs(output), 0.01f)
    }

    @Test
    fun makeupGain_isAppliedAfterLimiting() {
        val output = limit(sine(1000f, sampleRate, amplitude = 1f), thresholdDb = -12f, gainDb = 6f)
        // Limited to -12 dB, then +6 dB makeup -> peak at -6 dBFS.
        assertEquals(10f.pow(-6f / 20f), maxAbs(output), 0.01f)
    }

    @Test
    fun signalBelowThreshold_isUntouched() {
        val input = sine(1000f, sampleRate, amplitude = 0.2f)
        val output = limit(input, thresholdDb = -6f)
        for (i in input.indices) {
            assertEquals(input[i], output[i], 1e-6f)
        }
    }

    @Test
    fun gainRecoversAfterALoudBurst() {
        val limiter = LimiterCore()
        limiter.configure(sampleRate, thresholdDb = -12f, gainDb = 0f, releaseMs = 20f)

        val burst = sine(1000f, 4800, amplitude = 1f)
        limiter.processStereo(burst, burst.copyOf(), burst.size)

        // After well over the release time of quiet signal, gain is back near unity.
        val quiet = sine(1000f, sampleRate, amplitude = 0.05f)
        val out = quiet.copyOf()
        limiter.processStereo(out, out.copyOf(), out.size)
        val tailStart = out.size * 4 / 5
        for (i in tailStart until out.size) {
            assertEquals(quiet[i], out[i], 0.002f)
        }
    }
}
