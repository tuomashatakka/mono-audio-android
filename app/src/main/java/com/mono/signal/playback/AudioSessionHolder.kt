package com.mono.signal.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the audio session id from the player (which lives in [PlaybackService]) to the
 * UI-side [AudioVisualizer]. Both run in the same process, so a shared singleton is the
 * simplest correct channel — no IPC, no global-mix capture permissions.
 */
@Singleton
class AudioSessionHolder @Inject constructor() {
    private val _sessionId = MutableStateFlow(0)
    val sessionId: StateFlow<Int> = _sessionId.asStateFlow()

    fun update(id: Int) { _sessionId.value = id }
}
