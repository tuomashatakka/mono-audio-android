package com.mono.signal.playback.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Feed-forward, stereo-linked, hard-knee compressor with optional makeup gain — the same shape the
 * old DynamicsProcessing MBC band was configured to (kneeWidth = 0, preGain = 0). The detector is
 * the per-sample peak of both channels run through a one-pole attack/release envelope; gain
 * reduction above threshold is `over * (1 - 1/ratio)` dB, and the makeup stage is a flat postGain
 * applied to every sample after it.
 */
class CompressorCore {

    private var attackCoef = 0f
    private var releaseCoef = 0f
    private var thresholdDb = 0f
    private var slope = 0f
    private var compressing = false
    private var makeupGain = 1f
    private var bypass = true

    private var envelope = 0f

    fun configure(
        sampleRate: Int,
        attackMs: Float,
        releaseMs: Float,
        thresholdDb: Float,
        ratio: Float,
        makeupGainDb: Float = 0f,
    ) {
        attackCoef = envelopeCoef(sampleRate, attackMs)
        releaseCoef = envelopeCoef(sampleRate, releaseMs)
        this.thresholdDb = thresholdDb
        // The UI's minimum ratio is 1:1 — treat it as "detector off" instead of computing no-op gains.
        compressing = ratio > 1.001f
        slope = if (compressing) 1f - 1f / ratio else 0f
        makeupGain = if (makeupGainDb == 0f) 1f else 10f.pow(makeupGainDb / 20f)
        // Nothing to do at all only when neither stage would change a sample.
        bypass = !compressing && makeupGain == 1f
    }

    /** Processes both channels with linked gain. Pass the same array twice for mono content. */
    fun processStereo(left: FloatArray, right: FloatArray, count: Int) {
        if (bypass) return
        val stereo = right !== left
        var env = envelope
        for (i in 0 until count) {
            var gain = makeupGain
            if (compressing) {
                val peak = max(abs(left[i]), abs(right[i]))
                val coef = if (peak > env) attackCoef else releaseCoef
                env = peak + coef * (env - peak)
                val overDb = 20f * log10(max(env, 1e-6f)) - thresholdDb
                if (overDb > 0f) gain *= 10f.pow(-overDb * slope / 20f)
            }
            if (gain != 1f) {
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
