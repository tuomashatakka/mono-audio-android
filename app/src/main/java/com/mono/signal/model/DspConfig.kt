package com.mono.signal.model

/**
 * Immutable snapshot of the audio-processing chain the user has dialed in on the DSP screen.
 *
 * The values drive the in-process effect chain the audio sink runs on every PCM buffer (see
 * `playback/dsp/MonoDspAudioProcessor.kt`): [eqBands] a 12-band peaking-EQ bank, [compressor] a
 * stereo-linked compressor, and [limiter] the output limiter. Gains are in dB.
 */
data class DspConfig(
    val enabled: Boolean = false,
    /** 12 EQ band gains in dB, index-aligned with [EQ_FREQUENCIES]. */
    val eqBands: List<Float> = List(EQ_FREQUENCIES.size) { 0f },
    val compressor: Compressor = Compressor(),
    val limiter: Limiter = Limiter(),
) {
    data class Compressor(
        val attackMs: Float = 24f,
        val releaseMs: Float = 180f,
        val thresholdDb: Float = -18f,
        val ratio: Float = 4f,
    )

    data class Limiter(
        val thresholdDb: Float = -1f,
        val gainDb: Float = 0f,
        val releaseMs: Float = 120f,
    )

    /** Returns a copy with a single EQ band changed (no-op if [index] is out of range). */
    fun withEqBand(index: Int, gainDb: Float): DspConfig {
        if (index !in eqBands.indices) return this
        return copy(eqBands = eqBands.toMutableList().also { it[index] = gainDb })
    }

    fun flattenedEq(): DspConfig = copy(eqBands = List(EQ_FREQUENCIES.size) { 0f })

    companion object {
        /** Center frequencies (Hz) for the 12-band EQ — must match the screen's band labels. */
        val EQ_FREQUENCIES = floatArrayOf(
            35f, 65f, 85f, 120f, 240f, 480f, 960f, 1800f, 3500f, 7000f, 12000f, 16000f,
        )

        const val EQ_MIN_DB = -12f
        const val EQ_MAX_DB = 12f

        /** A few quick starting points for the preset dropdown. */
        val PRESETS: Map<String, List<Float>> = linkedMapOf(
            "Flat" to List(EQ_FREQUENCIES.size) { 0f },
            "Bass" to listOf(7f, 6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 1f, 2f),
            "Vocal" to listOf(-3f, -2f, -1f, 0f, 1f, 3f, 4f, 4f, 3f, 1f, 0f, -1f),
            "Treble" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 6f),
        )
    }
}
