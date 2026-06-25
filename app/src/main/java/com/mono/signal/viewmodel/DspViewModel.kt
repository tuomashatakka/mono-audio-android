package com.mono.signal.viewmodel

import androidx.lifecycle.ViewModel
import com.mono.signal.data.DspPreferences
import com.mono.signal.model.DspConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DspViewModel @Inject constructor(
    private val dspPreferences: DspPreferences,
) : ViewModel() {

    val config: StateFlow<DspConfig> = dspPreferences.config

    fun setEnabled(enabled: Boolean) = dspPreferences.setEnabled(enabled)
    fun setEqBand(index: Int, gainDb: Float) = dspPreferences.setEqBand(index, gainDb)
    fun setEqBands(gains: List<Float>) = dspPreferences.setEqBands(gains)
    fun resetEq() = dspPreferences.resetEq()
    fun setCompressor(compressor: DspConfig.Compressor) = dspPreferences.setCompressor(compressor)
    fun setLimiter(limiter: DspConfig.Limiter) = dspPreferences.setLimiter(limiter)
}
