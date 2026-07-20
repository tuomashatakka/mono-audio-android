package com.mono.signal.playback.dsp

import com.mono.signal.model.DspConfig

/**
 * The full in-process effect chain in fixed order: EQ → compressor → limiter → clamp to ±1.
 * The final clamp is required before PCM16 re-quantization anyway and doubles as the cap on the
 * limiter's post-limiting makeup gain.
 *
 * Pure Kotlin (only [DspConfig] as input) — this is the unit the JVM tests exercise.
 */
class DspChain(private val channelCount: Int) {

    private val equalizer = EqualizerCore(channelCount)
    private val compressor = CompressorCore()
    private val limiter = LimiterCore()

    fun configure(sampleRate: Int, config: DspConfig) {
        equalizer.configure(sampleRate, config.eqBands)
        val comp = config.compressor
        compressor.configure(sampleRate, comp.attackMs, comp.releaseMs, comp.thresholdDb, comp.ratio)
        val lim = config.limiter
        limiter.configure(sampleRate, lim.thresholdDb, lim.gainDb, lim.releaseMs)
    }

    /** Processes [count] samples of each channel in [channels] (size [channelCount]) in place. */
    fun process(channels: Array<FloatArray>, count: Int) {
        for (ch in 0 until channelCount) equalizer.process(ch, channels[ch], count)

        // The dynamics cores are stereo-linked; mono content just runs the same array once.
        val left = channels[0]
        val right = if (channelCount > 1) channels[1] else left
        compressor.processStereo(left, right, count)
        limiter.processStereo(left, right, count)

        for (ch in 0 until channelCount) {
            val samples = channels[ch]
            for (i in 0 until count) samples[i] = samples[i].coerceIn(-1f, 1f)
        }
    }

    fun reset() {
        equalizer.reset()
        compressor.reset()
        limiter.reset()
    }
}
