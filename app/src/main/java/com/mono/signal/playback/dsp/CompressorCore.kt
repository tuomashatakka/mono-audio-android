package com.mono.signal.playback.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Feed-forward, stereo-linked, hard-knee compressor with no makeup gain — the same shape the old
 * DynamicsProcessing MBC band was configured to (kneeWidth = 0, pre/postGain = 0). The detector is
 * the per-sample peak of both channels run through a one-pole attack/release envelope; gain
 * reduction above threshold is `over * (1 - 1/ratio)` dB.
 */
class CompressorCore {

    private var attackCoef = 0f
    private var releaseCoef = 0f
    private var thresholdDb = 0f
    private var slope = 0f
    private var bypass = true

    private var envelope = 0f

    fun configure(sampleRate: Int, attackMs: Float, releaseMs: Float, thresholdDb: Float, ratio: Float) {
        attackCoef = envelopeCoef(sampleRate, attackMs)
        releaseCoef = envelopeCoef(sampleRate, releaseMs)
        this.thresholdDb = thresholdDb
        // The UI's minimum ratio is 1:1 — treat it as a true bypass instead of computing no-op gains.
        bypass = ratio <= 1.001f
        slope = if (bypass) 0f else 1f - 1f / ratio
    }

    /** Processes both channels with linked gain. Pass the same array twice for mono content. */
    fun processStereo(left: FloatArray, right: FloatArray, count: Int) {
        if (bypass) return
        val stereo = right !== left
        var env = envelope
        for (i in 0 until count) {
            val peak = max(abs(left[i]), abs(right[i]))
            val coef = if (peak > env) attackCoef else releaseCoef
            env = peak + coef * (env - peak)
            val overDb = 20f * log10(max(env, 1e-6f)) - thresholdDb
            if (overDb > 0f) {
                val gain = 10f.pow(-overDb * slope / 20f)
                left[i] *= gain
                if (stereo) right[i] *= gain
            }
        }
        envelope = env
    }

    fun reset() {
        envelope = 0f
    }
}

/**
 * One-pole smoothing coefficient for a [timeMs] attack/release at [sampleRate]. The floor keeps a
 * 0 ms setting meaning "effectively instant" rather than dividing by zero.
 */
internal fun envelopeCoef(sampleRate: Int, timeMs: Float): Float =
    exp(-1.0 / (max(timeMs, 0.02f) / 1000.0 * sampleRate)).toFloat()
