package com.mono.signal.data

import android.content.Context
import com.mono.signal.model.DspConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the audio-processing chain ([DspConfig]) in SharedPreferences and exposes it as a
 * [StateFlow] so both the UI and [com.mono.signal.playback.dsp.MonoDspAudioProcessor] react to
 * every change.
 * Mirrors [ThemePreferences]: read synchronously on construction, write granularly.
 */
@Singleton
class DspPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("mono_dsp", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(load())
    val config: StateFlow<DspConfig> = _config.asStateFlow()

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setEqBand(index: Int, gainDb: Float) = update {
        it.withEqBand(index, gainDb.coerceIn(DspConfig.EQ_MIN_DB, DspConfig.EQ_MAX_DB))
    }

    fun setEqBands(gains: List<Float>) = update {
        it.copy(eqBands = gains.map { g -> g.coerceIn(DspConfig.EQ_MIN_DB, DspConfig.EQ_MAX_DB) })
    }

    fun resetEq() = update { it.flattenedEq() }

    fun setCompressor(compressor: DspConfig.Compressor) = update { it.copy(compressor = compressor) }

    fun setLimiter(limiter: DspConfig.Limiter) = update { it.copy(limiter = limiter) }

    private inline fun update(transform: (DspConfig) -> DspConfig) {
        val next = transform(_config.value)
        _config.value = next
        prefs.edit()
            .putBoolean(KEY_ENABLED, next.enabled)
            .putString(KEY_EQ, next.eqBands.joinToString(",") { it.toString() })
            .putFloat(KEY_C_ATTACK, next.compressor.attackMs)
            .putFloat(KEY_C_RELEASE, next.compressor.releaseMs)
            .putFloat(KEY_C_THRESHOLD, next.compressor.thresholdDb)
            .putFloat(KEY_C_RATIO, next.compressor.ratio)
            .putFloat(KEY_L_THRESHOLD, next.limiter.thresholdDb)
            .putFloat(KEY_L_GAIN, next.limiter.gainDb)
            .putFloat(KEY_L_RELEASE, next.limiter.releaseMs)
            .apply()
    }

    private fun load(): DspConfig {
        val bandCount = DspConfig.EQ_FREQUENCIES.size
        val eq = prefs.getString(KEY_EQ, null)
            ?.split(",")
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == bandCount }
            ?: List(bandCount) { 0f }
        val defaults = DspConfig()
        return DspConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            eqBands = eq,
            compressor = DspConfig.Compressor(
                attackMs = prefs.getFloat(KEY_C_ATTACK, defaults.compressor.attackMs),
                releaseMs = prefs.getFloat(KEY_C_RELEASE, defaults.compressor.releaseMs),
                thresholdDb = prefs.getFloat(KEY_C_THRESHOLD, defaults.compressor.thresholdDb),
                ratio = prefs.getFloat(KEY_C_RATIO, defaults.compressor.ratio),
            ),
            limiter = DspConfig.Limiter(
                thresholdDb = prefs.getFloat(KEY_L_THRESHOLD, defaults.limiter.thresholdDb),
                gainDb = prefs.getFloat(KEY_L_GAIN, defaults.limiter.gainDb),
                releaseMs = prefs.getFloat(KEY_L_RELEASE, defaults.limiter.releaseMs),
            ),
        )
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_EQ = "eq_bands"
        const val KEY_C_ATTACK = "comp_attack"
        const val KEY_C_RELEASE = "comp_release"
        const val KEY_C_THRESHOLD = "comp_threshold"
        const val KEY_C_RATIO = "comp_ratio"
        const val KEY_L_THRESHOLD = "lim_threshold"
        const val KEY_L_GAIN = "lim_gain"
        const val KEY_L_RELEASE = "lim_release"
    }
}
