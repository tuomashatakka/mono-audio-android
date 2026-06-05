package com.mono.audio.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mono.audio.data.AudioTrack
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PlayerController(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    val audioSessionId: Int get() = player.audioSessionId
    val positionMs: Long get() = player.currentPosition.coerceAtLeast(0L)

    fun playbackEvents(): Flow<Unit> = callbackFlow {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) { trySend(Unit) }
            override fun onIsPlayingChanged(isPlaying: Boolean) { trySend(Unit) }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { trySend(Unit) }
        }
        player.addListener(listener)
        trySend(Unit)
        awaitClose { player.removeListener(listener) }
    }

    fun setTrack(track: AudioTrack) {
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.playWhenReady = true
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun isPlaying(): Boolean = player.isPlaying
    fun release() = player.release()
}
