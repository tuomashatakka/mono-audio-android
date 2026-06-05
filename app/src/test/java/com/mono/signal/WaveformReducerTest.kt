package com.mono.signal

import com.mono.signal.data.waveform.WaveformReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformReducerTest {

    @Test
    fun normalize_scalesPeakToOne() {
        val out = WaveformReducer.normalize(floatArrayOf(0.1f, 0.2f, 0.5f))
        assertEquals(1f, out.max(), 1e-4f)
        assertTrue(out.all { it in 0f..1f })
    }

    @Test
    fun normalize_allZeroStaysZero() {
        val out = WaveformReducer.normalize(floatArrayOf(0f, 0f, 0f))
        assertTrue(out.all { it == 0f })
    }

    @Test
    fun accumulator_producesRequestedBucketCount_andTracksPeaks() {
        val frames = 1000L
        val acc = WaveformReducer.Accumulator(totalFrames = frames, buckets = 10)
        // Mono full-scale samples across the whole track.
        val chunk = ShortArray(1000) { Short.MAX_VALUE }
        acc.add(chunk, chunk.size, channels = 1)
        val env = acc.build()
        assertEquals(10, env.size)
        assertEquals(1f, env.max(), 1e-3f)
    }

    @Test
    fun accumulator_averagesStereoChannels() {
        val acc = WaveformReducer.Accumulator(totalFrames = 4, buckets = 4)
        // L=full, R=0 -> mono average is half scale.
        val stereo = shortArrayOf(
            Short.MAX_VALUE, 0,
            Short.MAX_VALUE, 0,
            Short.MAX_VALUE, 0,
            Short.MAX_VALUE, 0,
        )
        acc.add(stereo, stereo.size, channels = 2)
        val env = acc.build()
        // After normalization the (uniform) half-scale peaks become 1.0.
        assertTrue(env.all { it in 0.99f..1f })
    }
}
