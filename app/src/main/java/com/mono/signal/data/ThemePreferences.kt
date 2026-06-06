package com.mono.signal.data

import android.content.Context
import com.mono.signal.model.AccentOption
import com.mono.signal.model.BackgroundOption
import com.mono.signal.model.ThemeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's appearance choices in SharedPreferences and exposes them as a
 * [StateFlow] so the whole app recomposes when the theme changes. Read synchronously on
 * construction so the very first frame already uses the saved theme.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("mono_theme", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(load())
    val config: StateFlow<ThemeConfig> = _config.asStateFlow()

    fun setAccent(accent: AccentOption) = update { it.copy(accent = accent) }

    fun setBackground(background: BackgroundOption) = update { it.copy(background = background) }

    fun setFftBlockSize(blockSize: Int) = update { it.copy(fftBlockSize = blockSize.coerceIn(512, 8192)) }

    private inline fun update(transform: (ThemeConfig) -> ThemeConfig) {
        val next = transform(_config.value)
        _config.value = next
        prefs.edit()
            .putString(KEY_ACCENT, next.accent.name)
            .putString(KEY_BACKGROUND, next.background.name)
            .putInt(KEY_FFT_BLOCK_SIZE, next.fftBlockSize)
            .apply()
    }

    private fun load(): ThemeConfig {
        val accent = prefs.getString(KEY_ACCENT, null)
            ?.let { runCatching { AccentOption.valueOf(it) }.getOrNull() }
            ?: AccentOption.TURQUOISE
        val background = prefs.getString(KEY_BACKGROUND, null)
            ?.let { runCatching { BackgroundOption.valueOf(it) }.getOrNull() }
            ?: BackgroundOption.VOID
        val fftBlockSize = prefs.getInt(KEY_FFT_BLOCK_SIZE, 2048).takeIf { it in FftBlockSizes } ?: 2048
        return ThemeConfig(accent, background, fftBlockSize)
    }

    private companion object {
        const val KEY_ACCENT = "accent"
        const val KEY_BACKGROUND = "background"
        const val KEY_FFT_BLOCK_SIZE = "fft_block_size"
        val FftBlockSizes = setOf(512, 1024, 2048, 4096, 8192)
    }
}
