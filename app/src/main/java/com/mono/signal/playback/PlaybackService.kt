package com.mono.signal.playback

import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background playback host. Owns the single [ExoPlayer] instance and exposes it through a
 * [MediaSession] so the UI (and the system media notification / lock screen) can control it.
 *
 * The player's audio session id is published to [AudioSessionHolder] so the in-process
 * [AudioVisualizer] can attach to exactly this stream.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var sessionHolder: AudioSessionHolder

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val sessionId = audioManager.generateAudioSessionId()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { setAudioSessionId(sessionId) }

        sessionHolder.update(sessionId)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sessionHolder.update(0)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
