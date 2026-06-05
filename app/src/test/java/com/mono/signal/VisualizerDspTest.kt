package com.mono.signal

import com.mono.signal.playback.VisualizerDsp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerDspTest {

    @Test
    fun fftToBands_returnsRequestedSize_andNormalizedRange() {
        val fft = ByteArray(512) { ((it * 7) % 64 - 32).toByte() }
        val bands = VisualizerDsp.fftToBands(fft, bands = 32)
        assertEquals(32, bands.size)
        assertTrue(bands.all { it in 0f..1f })
    }

    @Test
    fun fftToBands_tinyBufferIsSafe() {
        val bands = VisualizerDsp.fftToBands(ByteArray(2), bands = 16)
        assertEquals(16, bands.size)
        assertTrue(bands.all { it == 0f })
    }

    @Test
    fun fftToPeakAndRmsBands_returnsSeparateNormalizedTraces() {
        val fft = ByteArray(512) { ((it * 11) % 96 - 48).toByte() }
        val spectrum = VisualizerDsp.fftToPeakAndRmsBands(fft, bands = 24)
        assertEquals(24, spectrum.peak.size)
        assertEquals(24, spectrum.rms.size)
        assertTrue(spectrum.peak.all { it in 0f..1f })
        assertTrue(spectrum.rms.all { it in 0f..1f })
    }

    @Test
    fun waveformToFloats_centersUnsignedPcmAroundZero() {
        // 128 is the unsigned PCM midpoint -> 0f.
        val mid = ByteArray(64) { 128.toByte() }
        val out = VisualizerDsp.waveformToFloats(mid, points = 64)
        assertEquals(64, out.size)
        assertTrue(out.all { it == 0f })

        // 255 -> near +1, 0 -> -1.
        val high = ByteArray(64) { 255.toByte() }
        assertTrue(VisualizerDsp.waveformToFloats(high, 64).all { it > 0.9f })
        val low = ByteArray(64) { 0.toByte() }
        assertTrue(VisualizerDsp.waveformToFloats(low, 64).all { it == -1f })
    }

    @Test
    fun frameFromEnvelope_createsVisibleFallbackFrame() {
        val envelope = FloatArray(256) { i -> if (i % 16 < 8) 0.85f else 0.25f }

        val frame = VisualizerDsp.frameFromEnvelope(envelope, progress = 0.4f, bands = 24, points = 32)

        assertEquals(24, frame.fftBands.size)
        assertEquals(32, frame.waveform.size)
        assertTrue(frame.fftBands.any { it > 0f })
        assertTrue(frame.waveform.any { it > 0f })
        assertTrue(frame.waveform.any { it < 0f })
        assertTrue(VisualizerDsp.hasSignal(frame))
    }

    @Test
    fun frameFromEnvelope_keepsSilentEnvelopeAtZero() {
        val frame = VisualizerDsp.frameFromEnvelope(FloatArray(64), progress = 0.5f, bands = 12, points = 8)

        assertEquals(12, frame.fftBands.size)
        assertEquals(8, frame.waveform.size)
        assertFalse(VisualizerDsp.hasSignal(frame))
    }

    @Test
    fun magnitudeSpectrum_peaksAtTheToneBin() {
        val n = 1024
        val bin = 64
        val samples = FloatArray(n) { i -> kotlin.math.sin(2.0 * Math.PI * bin * i / n).toFloat() }

        val mags = VisualizerDsp.magnitudeSpectrum(samples, fftSize = n)

        assertEquals(n / 2, mags.size)
        var argmax = 0
        for (k in mags.indices) if (mags[k] > mags[argmax]) argmax = k
        // Hann leakage can nudge the peak by a bin either way.
        assertTrue("peak at $argmax, expected ~$bin", kotlin.math.abs(argmax - bin) <= 1)
    }

    @Test
    fun magnitudeSpectrum_truncatesToPowerOfTwo() {
        // 700 samples -> largest power of two <= 700 is 512 -> 256 bins.
        val mags = VisualizerDsp.magnitudeSpectrum(FloatArray(700) { 0f }, fftSize = 700)
        assertEquals(256, mags.size)
        assertTrue(mags.all { it == 0f })
    }

    @Test
    fun spectrumBands_returnsNormalizedTracesAndRespondsToATone() {
        val n = 1024
        val samples = FloatArray(n) { i -> kotlin.math.sin(2.0 * Math.PI * 64 * i / n).toFloat() }

        val spectrum = VisualizerDsp.spectrumBands(samples, fftSize = n, bands = 32)

        assertEquals(32, spectrum.peak.size)
        assertEquals(32, spectrum.rms.size)
        assertTrue(spectrum.peak.all { it in 0f..1f })
        assertTrue(spectrum.rms.all { it in 0f..1f })
        assertTrue("a loud tone should light up at least one band", spectrum.peak.any { it > 0.2f })
    }

    @Test
    fun spectrumBands_silenceStaysZero() {
        val spectrum = VisualizerDsp.spectrumBands(FloatArray(1024), fftSize = 1024, bands = 24)
        assertEquals(24, spectrum.peak.size)
        assertTrue(spectrum.peak.all { it == 0f })
        assertTrue(spectrum.rms.all { it == 0f })
    }

    @Test
    fun downsampleWaveform_keepsRangeAndEndpoints() {
        val ramp = FloatArray(100) { i -> -1f + 2f * i / 99f }
        val out = VisualizerDsp.downsampleWaveform(ramp, points = 10)

        assertEquals(10, out.size)
        assertTrue(out.all { it in -1f..1f })
        assertTrue(out.first() < -0.9f)
        assertTrue(out.last() > 0.9f)
    }

    @Test
    fun smooth_movesTowardTarget() {
        val prev = FloatArray(4) { 0f }
        val target = FloatArray(4) { 1f }
        val out = VisualizerDsp.smooth(prev, target, attack = 0.5f, decay = 0.1f)
        assertTrue(out.all { it in 0.49f..0.51f }) // halfway on attack
    }
}
