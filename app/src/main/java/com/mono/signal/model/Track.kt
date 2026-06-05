package com.mono.signal.model

import android.net.Uri

/**
 * A single local audio item, as surfaced from [android.provider.MediaStore].
 * Pure data — no Android playback types leak through here.
 */
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumId: Long,
) {
    val mediaId: String get() = id.toString()
}
