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
import com.mono.signal.playback.dsp.MonoDspAudioProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background playback host. Owns the single [ExoPlayer] instance and exposes it through a
 * [MediaSession] so the UI (and the system media notification / lock screen) can control it.
 *
 * The user's DSP chain ([MonoDspAudioProcessor]) and the visualizer tap ([PcmAudioTap] via
 * [TeeAudioProcessor]) are inserted into the audio sink's processor chain, in that order: EQ /
 * compressor / limiter run in-process on the real PCM — no fragile per-device
 * [android.media.audiofx.DynamicsProcessing] — and the visualizer reads the *processed* stereo
 * output directly, with no `RECORD_AUDIO` and none of the Android
 * [android.media.audiofx.Visualizer] rate/mono/8-bit limits. The rendered audio session id is
 * still published to [AudioSessionHolder] for any session consumers.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var sessionHolder: AudioSessionHolder

    /** Process-wide tap that turns the player's PCM output into live visualizer frames. */
    @Inject lateinit var pcmTap: PcmAudioTap

    /** In-process equalizer / compressor / limiter driven by the DSP screen. */
    @Inject lateinit var dspProcessor: MonoDspAudioProcessor

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Insert the DSP chain and then the PCM tap ahead of the default chain (silence-skip +
        // Sonic): the tap sees the processed, full-rate stereo output — so the visualizer reflects
        // EQ moves — while playback speed/scrubbing keep working downstream. Float output must stay
        // disabled: with it on, the sink routes high-res float content around the custom processor
        // chain entirely (and off means Media3 converts to 16-bit PCM before our processors).
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(dspProcessor, TeeAudioProcessor(pcmTap)))
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
