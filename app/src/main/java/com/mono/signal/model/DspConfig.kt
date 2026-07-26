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
        /** Whether the post-compression makeup stage runs at all. */
        val makeupGainEnabled: Boolean = false,
        val makeupGainDb: Float = 6f,
    ) {
        /** The dB the chain actually applies — the dialed-in value only counts while enabled. */
        val effectiveMakeupGainDb: Float get() = if (makeupGainEnabled) makeupGainDb else 0f

        /** Clamps every field into the range the screen exposes, so prefs written by an older
         *  build (whose attack floor was 0 ms and release floor 20 ms) can't sit off-slider. */
        fun coerced(): Compressor = copy(
            attackMs = attackMs.coerceIn(ATTACK_MIN_MS, ATTACK_MAX_MS),
            releaseMs = releaseMs.coerceIn(RELEASE_MIN_MS, RELEASE_MAX_MS),
            thresholdDb = thresholdDb.coerceIn(COMP_THRESHOLD_MIN_DB, COMP_THRESHOLD_MAX_DB),
            ratio = ratio.coerceIn(RATIO_MIN, RATIO_MAX),
            makeupGainDb = makeupGainDb.coerceIn(MAKEUP_MIN_DB, MAKEUP_MAX_DB),
        )
    }

    data class Limiter(
        val thresholdDb: Float = -1f,
        val gainDb: Float = 0f,
        val releaseMs: Float = 120f,
    ) {
        /** See [Compressor.coerced]. */
        fun coerced(): Limiter = copy(
            thresholdDb = thresholdDb.coerceIn(LIM_THRESHOLD_MIN_DB, LIM_THRESHOLD_MAX_DB),
            gainDb = gainDb.coerceIn(LIM_GAIN_MIN_DB, LIM_GAIN_MAX_DB),
            releaseMs = releaseMs.coerceIn(LIM_RELEASE_MIN_MS, LIM_RELEASE_MAX_MS),
        )
    }

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

        /**
         * The dynamics ranges — the single source of truth shared by the screen's sliders and the
         * clamps in [Compressor.coerced] / [Limiter.coerced].
         *
         * The attack and release floors sit at or below a millisecond so the fast, clamping
         * settings are actually reachable; the screen maps those three onto logarithmic sliders,
         * because on a linear track everything under ~10 ms collapses into the first few pixels.
         */
        const val ATTACK_MIN_MS = 0.02f
        const val ATTACK_MAX_MS = 200f
        const val RELEASE_MIN_MS = 0.5f
        const val RELEASE_MAX_MS = 2000f
        const val COMP_THRESHOLD_MIN_DB = -60f
        const val COMP_THRESHOLD_MAX_DB = 0f
        const val RATIO_MIN = 1f
        const val RATIO_MAX = 80f
        const val MAKEUP_MIN_DB = 0f
        const val MAKEUP_MAX_DB = 24f

        const val LIM_THRESHOLD_MIN_DB = -24f
        const val LIM_THRESHOLD_MAX_DB = 0f
        const val LIM_GAIN_MIN_DB = -12f
        const val LIM_GAIN_MAX_DB = 12f
        const val LIM_RELEASE_MIN_MS = 0.5f
        const val LIM_RELEASE_MAX_MS = 1000f

        /** A few quick starting points for the preset dropdown. */
        val PRESETS: Map<String, List<Float>> = linkedMapOf(
            "Flat" to List(EQ_FREQUENCIES.size) { 0f },
            "Bass" to listOf(7f, 6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 1f, 2f),
            "Vocal" to listOf(-3f, -2f, -1f, 0f, 1f, 3f, 4f, 4f, 3f, 1f, 0f, -1f),
            "Treble" to listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 6f),
        )
    }
}
