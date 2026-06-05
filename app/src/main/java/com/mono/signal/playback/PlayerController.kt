package com.mono.signal.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mono.signal.data.MusicRepository
import com.mono.signal.model.PlaybackState
import com.mono.signal.model.RepeatMode
import com.mono.signal.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single UI-facing entry point for playback. Connects to [PlaybackService] via a
 * [MediaController], translates Media3 callbacks into an immutable [PlaybackState], and
 * forwards transport intents. This is the only place that touches Media3 player types —
 * Composables and ViewModels see [PlaybackState] / [Track] only.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository,
    private val sessionHolder: AudioSessionHolder,
    private val visualizer: AudioVisualizer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlaybackState.Empty)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private val submitted = LinkedHashMap<String, Track>()

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(playerListener) }
            syncState()
        }, MoreExecutors.directExecutor())

        // Attach the live visualizer to whatever session the service exposes.
        scope.launch {
            sessionHolder.sessionId.collect { id -> if (id != 0) visualizer.start(id) }
        }
    }

    fun release() {
        positionJob?.cancel()
        controller?.release()
        controller = null
        visualizer.release()
    }

    /** Replace the queue with [tracks] and start at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        submitted.clear()
        val items = tracks.map { track ->
            submitted[track.mediaId] = track
            track.toMediaItem()
        }
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }

    fun seekToFraction(fraction: Float) {
        val c = controller ?: return
        val duration = c.duration
        if (duration > 0) c.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
    }

    private fun syncState() {
        val c = controller ?: return
        val track = c.currentMediaItem?.mediaId?.let { resolveTrack(it) }
        _state.value = PlaybackState(
            currentTrack = track,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.coerceAtLeast(0L),
            shuffle = c.shuffleModeEnabled,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            },
        )
        managePositionTicker(c.isPlaying)
    }

    /** While playing, tick position so the waveform/seek bar advance smoothly. */
    private fun managePositionTicker(isPlaying: Boolean) {
        if (isPlaying && positionJob?.isActive != true) {
            positionJob = scope.launch {
                while (true) {
                    val c = controller ?: break
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0L),
                        durationMs = c.duration.coerceAtLeast(0L),
                    )
                    delay(500)
                }
            }
        } else if (!isPlaying) {
            positionJob?.cancel()
            positionJob = null
        }
    }

    private fun resolveTrack(mediaId: String): Track? =
        submitted[mediaId] ?: repository.trackById(mediaId)

    private fun Track.toMediaItem(): MediaItem {
        val artworkUri = Uri.parse("content://media/external/audio/albumart/$albumId")
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
