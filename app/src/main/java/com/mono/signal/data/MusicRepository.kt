package com.mono.signal.data

import com.mono.signal.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for the local track list. */
@Singleton
class MusicRepository @Inject constructor(
    private val source: MediaStoreSource,
) {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val refreshLock = Mutex()

    suspend fun refresh() = refreshLock.withLock {
        _tracks.value = source.queryTracks()
    }

    fun trackById(id: String): Track? = _tracks.value.firstOrNull { it.mediaId == id }
}
