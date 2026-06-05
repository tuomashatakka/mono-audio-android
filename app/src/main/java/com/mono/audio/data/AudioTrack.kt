package com.mono.audio.data

import android.net.Uri

/** Small immutable model for one locally indexed audio item. */
data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
)
