package com.mono.audio.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class LocalAudioRepository(private val context: Context) {
    fun loadTracks(): List<AudioTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        AudioTrack(
                            id = id,
                            title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown track" },
                            artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown artist" },
                            durationMs = cursor.getLong(durationColumn),
                            uri = ContentUris.withAppendedId(collection, id),
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
