package com.mono.signal.viewmodel

import androidx.lifecycle.ViewModel
import com.mono.signal.data.ThemePreferences
import com.mono.signal.model.AccentOption
import com.mono.signal.model.BackgroundOption
import com.mono.signal.model.ThemeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val theme: StateFlow<ThemeConfig> = themePreferences.config

    fun setAccent(accent: AccentOption) = themePreferences.setAccent(accent)
    fun setBackground(background: BackgroundOption) = themePreferences.setBackground(background)
    fun setFftBlockSize(blockSize: Int) = themePreferences.setFftBlockSize(blockSize)
}
