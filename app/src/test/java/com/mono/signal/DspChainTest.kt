package com.mono.signal

import com.mono.signal.model.DspConfig
import com.mono.signal.playback.dsp.DspChain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class DspChainTest {

    private fun sine(freqHz: Float, samples: Int, sampleRate: Int, amplitude: Float): FloatArray =
        FloatArray(samples) { i -> amplitude * sin(2.0 * PI * freqHz * i / sampleRate).toFloat() }

    private fun steadyRms(samples: FloatArray, skip: Int = 4096): Double {
        var sum = 0.0
        for (i in skip until samples.size) sum += samples[i].toDouble() * samples[i]
        return sqrt(sum / (samples.size - skip))
    }

    /** Settings whose processing is audibly a no-op: flat EQ, 1:1 ratio, 0 dB limiter ceiling. */
    private fun neutralConfig() = DspConfig(
        enabled = true,
        compressor = DspConfig.Compressor(ratio = 1f),
        limiter = DspConfig.Limiter(thresholdDb = 0f, gainDb = 0f),
    )

    private fun boostedConfig(bandIndex: Int, gainDb: Float) = neutralConfig().withEqBand(bandIndex, gainDb)

    @Test
    fun neutralSettings_passStereoThroughUnchanged() {
        val chain = DspChain(channelCount = 2)
        chain.configure(48_000, neutralConfig())

        val input = sine(440f, 8192, 48_000, amplitude = 0.5f)
        val left = input.copyOf()
        val right = input.copyOf()
        chain.process(arrayOf(left, right), input.size)

        for (i in input.indices) {
            assertEquals(input[i], left[i], 1e-4f)
            assertEquals(input[i], right[i], 1e-4f)
        }
    }

    @Test
    fun eqBoost_holdsAcrossSampleRateReconfiguration() {
        val bandIndex = DspConfig.EQ_FREQUENCIES.indexOfFirst { it == 960f }
        for (sampleRate in intArrayOf(48_000, 44_100)) {
            val chain = DspChain(channelCount = 1)
            chain.configure(sampleRate, boostedConfig(bandIndex, 12f))

            val input = sine(960f, sampleRate, sampleRate, amplitude = 0.02f)
            val output = input.copyOf()
            chain.process(arrayOf(output), output.size)

            val gainDb = 20 * log10(steadyRms(output) / steadyRms(input))
            assertEquals("at $sampleRate Hz", 12.0, gainDb, 0.5)
        }
    }

    @Test
    fun monoContent_getsTheSameDynamicsAsStereo() {
        // -6 dBFS mono sine, threshold -18, ratio 4 -> -15 dBFS. A double-applied gain (the mono
        // array run through both "channels" of the linked cores) would land near -24 instead.
        val chain = DspChain(channelCount = 1)
        chain.configure(
            48_000,
            neutralConfig().copy(
                compressor = DspConfig.Compressor(attackMs = 5f, releaseMs = 50f, thresholdDb = -18f, ratio = 4f),
            ),
        )
        val samples = sine(1000f, 48_000, 48_000, amplitude = 0.5012f)
        chain.process(arrayOf(samples), samples.size)

        var peak = 0f
        for (i in samples.size * 4 / 5 until samples.size) peak = maxOf(peak, abs(samples[i]))
        assertEquals(-15.0, 20.0 * log10(peak.toDouble()), 1.0)
    }

    @Test
    fun outputIsClampedToFullScale_evenWithHotMakeupGain() {
        val chain = DspChain(channelCount = 2)
        chain.configure(48_000, neutralConfig().copy(limiter = DspConfig.Limiter(thresholdDb = 0f, gainDb = 12f)))

        val input = sine(440f, 8192, 48_000, amplitude = 1f)
        val left = input.copyOf()
        val right = input.copyOf()
        chain.process(arrayOf(left, right), input.size)

        assertTrue(left.all { it in -1f..1f })
        assertTrue(right.all { it in -1f..1f })
        // The clamp actually engaged (signal was driven past full scale).
        assertTrue(left.count { abs(it) == 1f } > 100)
    }

    @Test
    fun compressorMakeupGain_onlyAppliesWhileToggledOn() {
        val input = sine(440f, 8192, 48_000, amplitude = 0.05f)

        fun runWith(enabled: Boolean): FloatArray {
            val chain = DspChain(channelCount = 1)
            chain.configure(
                48_000,
                neutralConfig().copy(
                    compressor = DspConfig.Compressor(
                        ratio = 1f,
                        makeupGainEnabled = enabled,
                        makeupGainDb = 12f,
                    ),
                ),
            )
            return input.copyOf().also { chain.process(arrayOf(it), it.size) }
        }

        // Toggled off, the dialed-in 12 dB is inert; toggled on it lands exactly.
        assertEquals(0.0, 20 * log10(steadyRms(runWith(false)) / steadyRms(input)), 0.05)
        assertEquals(12.0, 20 * log10(steadyRms(runWith(true)) / steadyRms(input)), 0.05)
    }

    @Test
    fun reset_clearsAllStateSoSilenceStaysSilent() {
        val bandIndex = DspConfig.EQ_FREQUENCIES.indexOfFirst { it == 960f }
        val chain = DspChain(channelCount = 1)
        chain.configure(48_000, boostedConfig(bandIndex, 12f))

        val burst = sine(960f, 4800, 48_000, amplitude = 0.5f)
        chain.process(arrayOf(burst), burst.size)

        chain.reset()

        val silence = FloatArray(4800)
        chain.process(arrayOf(silence), silence.size)
        assertTrue(silence.all { it == 0f })
    }
}
