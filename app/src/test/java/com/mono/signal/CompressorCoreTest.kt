package com.mono.signal

import com.mono.signal.playback.dsp.CompressorCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

class CompressorCoreTest {

    private val sampleRate = 48_000

    private fun sine(freqHz: Float, samples: Int, amplitudeDb: Float): FloatArray {
        val amplitude = 10f.pow(amplitudeDb / 20f)
        return FloatArray(samples) { i -> amplitude * sin(2.0 * PI * freqHz * i / sampleRate).toFloat() }
    }

    /** Peak level in dBFS of the last fifth of [samples] (past the attack transient). */
    private fun steadyPeakDb(samples: FloatArray): Double {
        var peak = 0f
        for (i in samples.size * 4 / 5 until samples.size) peak = maxOf(peak, abs(samples[i]))
        return 20.0 * log10(peak.toDouble())
    }

    private fun compress(
        input: FloatArray,
        thresholdDb: Float,
        ratio: Float,
        attackMs: Float = 5f,
        releaseMs: Float = 50f,
    ): FloatArray {
        val comp = CompressorCore()
        comp.configure(sampleRate, attackMs, releaseMs, thresholdDb, ratio)
        val left = input.copyOf()
        val right = input.copyOf()
        comp.processStereo(left, right, left.size)
        return left
    }

    @Test
    fun signalOverThreshold_isReducedBySlope() {
        // -6 dBFS input, threshold -18, ratio 4 -> 12 dB over becomes 3 dB over: -15 dBFS out.
        val input = sine(1000f, sampleRate, amplitudeDb = -6f)
        val output = compress(input, thresholdDb = -18f, ratio = 4f)
        assertEquals(-15.0, steadyPeakDb(output), 1.0)
    }

    @Test
    fun signalBelowThreshold_isUntouched() {
        val input = sine(1000f, sampleRate, amplitudeDb = -26f)
        val output = compress(input, thresholdDb = -18f, ratio = 4f)
        for (i in input.indices) {
            assertEquals(input[i], output[i], 1e-6f)
        }
    }

    @Test
    fun unityRatio_isBitExactBypass() {
        val input = sine(1000f, sampleRate, amplitudeDb = -6f)
        val output = compress(input, thresholdDb = -18f, ratio = 1f)
        for (i in input.indices) {
            assertEquals(input[i], output[i], 0f)
        }
    }

    @Test
    fun higherRatio_compressesMore() {
        // Near-instant attack so the envelope tracks the sine crests and the ceiling is exact;
        // a musical attack lets crests through by design.
        val input = sine(1000f, sampleRate, amplitudeDb = -6f)
        val gentle = steadyPeakDb(compress(input, thresholdDb = -18f, ratio = 4f, attackMs = 0f))
        val brickish = steadyPeakDb(compress(input, thresholdDb = -18f, ratio = 80f, attackMs = 0f))
        assertTrue("expected ratio 80 ($brickish dB) below ratio 4 ($gentle dB)", brickish < gentle)
        // 80:1 leaves the level essentially at threshold.
        assertEquals(-18.0, brickish, 1.0)
    }

    @Test
    fun stereoChannels_shareLinkedGain() {
        val comp = CompressorCore()
        comp.configure(sampleRate, 5f, 50f, -18f, 4f)
        val loud = sine(1000f, sampleRate, amplitudeDb = -6f)
        val quiet = sine(1000f, sampleRate, amplitudeDb = -30f)
        comp.processStereo(loud, quiet, loud.size)
        // The quiet channel is pulled down by the loud channel's detector.
        assertTrue(steadyPeakDb(quiet) < -30.5)
    }
}
