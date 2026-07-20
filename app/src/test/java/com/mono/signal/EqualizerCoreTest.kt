package com.mono.signal

import com.mono.signal.model.DspConfig
import com.mono.signal.playback.dsp.EqualizerCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class EqualizerCoreTest {

    private val sampleRate = 48_000
    private val bandCount = DspConfig.EQ_FREQUENCIES.size

    private fun sine(freqHz: Float, samples: Int, amplitude: Float = 0.5f): FloatArray =
        FloatArray(samples) { i -> amplitude * sin(2.0 * PI * freqHz * i / sampleRate).toFloat() }

    /** RMS of the tail of [samples], skipping the filter's settling transient. */
    private fun steadyRms(samples: FloatArray, skip: Int = 4096): Double {
        var sum = 0.0
        for (i in skip until samples.size) sum += samples[i].toDouble() * samples[i]
        return sqrt(sum / (samples.size - skip))
    }

    private fun gains(vararg bandToDb: Pair<Int, Float>): List<Float> =
        MutableList(bandCount) { 0f }.also { for ((band, db) in bandToDb) it[band] = db }

    @Test
    fun flatGains_passThroughUnchanged() {
        val eq = EqualizerCore(channelCount = 1)
        eq.configure(sampleRate, gains())
        val input = sine(440f, 8192)
        val output = input.copyOf()
        eq.process(0, output, output.size)
        for (i in input.indices) {
            assertEquals(input[i], output[i], 1e-4f)
        }
    }

    @Test
    fun boostedBand_raisesItsCenterFrequencyByTheBandGain() {
        val bandIndex = DspConfig.EQ_FREQUENCIES.indexOfFirst { it == 960f }
        val eq = EqualizerCore(channelCount = 1)
        eq.configure(sampleRate, gains(bandIndex to 12f))

        val input = sine(960f, 48_000, amplitude = 0.1f)
        val output = input.copyOf()
        eq.process(0, output, output.size)

        val gainDb = 20 * log10(steadyRms(output) / steadyRms(input))
        assertEquals(12.0, gainDb, 0.5)
    }

    @Test
    fun boostedBassBand_leavesDistantTrebleAlone() {
        val eq = EqualizerCore(channelCount = 1)
        eq.configure(sampleRate, gains(0 to 12f)) // 35 Hz band

        val input = sine(7000f, 48_000, amplitude = 0.1f)
        val output = input.copyOf()
        eq.process(0, output, output.size)

        val gainDb = 20 * log10(steadyRms(output) / steadyRms(input))
        assertTrue("expected < 1 dB change at 7 kHz, got $gainDb", abs(gainDb) < 1.0)
    }

    @Test
    fun cutMirrorsBoost() {
        val bandIndex = DspConfig.EQ_FREQUENCIES.indexOfFirst { it == 960f }
        val input = sine(960f, 48_000, amplitude = 0.1f)

        val boosted = input.copyOf()
        EqualizerCore(channelCount = 1).apply {
            configure(sampleRate, gains(bandIndex to 12f))
            process(0, boosted, boosted.size)
        }
        val cut = input.copyOf()
        EqualizerCore(channelCount = 1).apply {
            configure(sampleRate, gains(bandIndex to -12f))
            process(0, cut, cut.size)
        }

        val boostDb = 20 * log10(steadyRms(boosted) / steadyRms(input))
        val cutDb = 20 * log10(steadyRms(cut) / steadyRms(input))
        assertEquals(boostDb, -cutDb, 0.5)
    }

    @Test
    fun channelsAreIndependent() {
        val bandIndex = DspConfig.EQ_FREQUENCIES.indexOfFirst { it == 960f }
        val eq = EqualizerCore(channelCount = 2)
        eq.configure(sampleRate, gains(bandIndex to 12f))

        val input = sine(960f, 48_000, amplitude = 0.1f)
        val left = input.copyOf()
        val right = input.copyOf()
        eq.process(0, left, left.size)
        eq.process(1, right, right.size)

        // Same signal through both channels' independent state gives the same result.
        for (i in input.indices) {
            assertEquals(left[i], right[i], 1e-6f)
        }
    }
}
