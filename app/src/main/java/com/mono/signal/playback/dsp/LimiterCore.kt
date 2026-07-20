package com.mono.signal.playback.dsp

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * True instant-attack peak limiter: gain drops immediately so no sample exceeds the threshold,
 * then recovers toward unity with an exponential release. [gainDb] is makeup gain applied *after*
 * limiting (the old DynamicsProcessing `postGain` mapping), so output may intentionally exceed the
 * threshold by the makeup amount — the chain's final clamp caps it at full scale.
 */
class LimiterCore {

    private var ceiling = 1f
    private var makeup = 1f
    private var releaseCoef = 0f

    private var gain = 1f

    fun configure(sampleRate: Int, thresholdDb: Float, gainDb: Float, releaseMs: Float) {
        ceiling = 10f.pow(thresholdDb / 20f)
        makeup = 10f.pow(gainDb / 20f)
        releaseCoef = envelopeCoef(sampleRate, releaseMs)
    }

    /** Processes both channels with linked gain. Pass the same array twice for mono content. */
    fun processStereo(left: FloatArray, right: FloatArray, count: Int) {
        val stereo = right !== left
        var g = gain
        for (i in 0 until count) {
            val peak = max(abs(left[i]), abs(right[i]))
            val needed = if (peak > ceiling) ceiling / peak else 1f
            // Instant attack: clamp down immediately; otherwise release toward unity, but never
            // above what the current sample allows.
            g = if (needed < g) needed else min(needed, 1f + releaseCoef * (g - 1f))
            left[i] *= g * makeup
            if (stereo) right[i] *= g * makeup
        }
        gain = g
    }

    fun reset() {
        gain = 1f
    }
}
