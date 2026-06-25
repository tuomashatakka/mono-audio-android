package com.mono.signal.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background playback host. Owns the single [ExoPlayer] instance and exposes it through a
 * [MediaSession] so the UI (and the system media notification / lock screen) can control it.
 *
 * Live visuals are sourced by inserting a [PcmAudioTap] (via [TeeAudioProcessor]) into the audio
 * sink's processor chain, so the visualizer reads ExoPlayer's real stereo PCM output directly —
 * no `RECORD_AUDIO`, no Android [android.media.audiofx.Visualizer] rate/mono/8-bit limits. The
 * rendered audio session id is still published to [AudioSessionHolder] for any session consumers.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var sessionHolder: AudioSessionHolder

    /** Process-wide tap that turns the player's PCM output into live visualizer frames. */
    @Inject lateinit var pcmTap: PcmAudioTap

    /** Live equalizer / compressor / limiter bound to the player session. */
    @Inject lateinit var dspController: DspController

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Insert the PCM tap ahead of the default chain (silence-skip + Sonic), so it sees the
        // real, full-rate stereo output while playback speed/scrubbing keep working downstream.
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(TeeAudioProcessor(pcmTap)))
                    .build()
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addAnalyticsListener(object : AnalyticsListener {
                    override fun onAudioSessionIdChanged(
                        eventTime: AnalyticsListener.EventTime,
                        audioSessionId: Int,
                    ) {
                        sessionHolder.update(audioSessionId)
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player).build()

        // Attach the DSP chain to whatever audio session the player publishes.
        dspController.start()
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
        dspController.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
