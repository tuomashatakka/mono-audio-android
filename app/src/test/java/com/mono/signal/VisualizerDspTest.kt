package com.mono.signal

import com.mono.signal.playback.VisualizerDsp
import org.junit.Assert.assertEquals
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
    fun smooth_movesTowardTarget() {
        val prev = FloatArray(4) { 0f }
        val target = FloatArray(4) { 1f }
        val out = VisualizerDsp.smooth(prev, target, attack = 0.5f, decay = 0.1f)
        assertTrue(out.all { it in 0.49f..0.51f }) // halfway on attack
    }
}
