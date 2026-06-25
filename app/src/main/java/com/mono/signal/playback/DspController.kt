package com.mono.signal.playback

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import com.mono.signal.data.DspPreferences
import com.mono.signal.model.DspConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns an [android.media.audiofx.DynamicsProcessing] effect bound to the player's audio session and
 * keeps it in sync with the user's [DspConfig]. Same-process singletons make this trivial: we watch
 * [AudioSessionHolder.sessionId] (published by [PlaybackService]) the same way [PlayerController]
 * attaches the visualizer, and re-apply [DspPreferences.config] whenever the user moves a slider.
 *
 * The effect is best-effort: some devices/emulators refuse to instantiate it, so every native call
 * is wrapped in [runCatching]. A failure degrades to "no processing" — the UI still works, audio is
 * just passed through — rather than crashing playback.
 */
@Singleton
class DspController @Inject constructor(
    private val sessionHolder: AudioSessionHolder,
    private val preferences: DspPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var effect: DynamicsProcessing? = null
    private var boundSession = 0

    fun start() {
        scope.launch {
            sessionHolder.sessionId.collectLatest { id ->
                rebind(id)
                // collectLatest cancels this inner collect when the session id changes.
                if (id != 0) {
                    preferences.config.collect { config -> applyConfig(config) }
                }
            }
        }
    }

    fun release() {
        disposeEffect()
    }

    /** Tear down any effect on the old session and build a fresh one for [sessionId]. */
    private fun rebind(sessionId: Int) {
        if (sessionId == boundSession && effect != null) return
        disposeEffect()
        if (sessionId == 0) return

        val bandCount = DspConfig.EQ_FREQUENCIES.size
        effect = runCatching {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                CHANNEL_COUNT,
                /* preEqInUse */ true, /* preEqBandCount */ bandCount,
                /* mbcInUse */ true, /* mbcBandCount */ 1,
                /* postEqInUse */ false, /* postEqBandCount */ 0,
                /* limiterInUse */ true,
            ).build()
            DynamicsProcessing(0, sessionId, config)
        }.onFailure {
            Log.w(TAG, "Could not attach DynamicsProcessing to session $sessionId", it)
        }.getOrNull()

        boundSession = sessionId
        applyConfig(preferences.config.value)
    }

    private fun disposeEffect() {
        runCatching { effect?.release() }
        effect = null
        boundSession = 0
    }

    /** Push [config] into the live effect. Master toggle gates the whole chain. */
    private fun applyConfig(config: DspConfig) {
        val dp = effect ?: return
        runCatching {
            dp.setEnabled(config.enabled)
            if (!config.enabled) return

            applyEq(dp, config)
            applyCompressor(dp, config.compressor)
            applyLimiter(dp, config.limiter)
        }.onFailure { Log.w(TAG, "Failed to apply DSP config", it) }
    }

    /** 12-band pre-EQ — fully wired: each band's center frequency + gain (dB) from the UI. */
    private fun applyEq(dp: DynamicsProcessing, config: DspConfig) {
        val freqs = DspConfig.EQ_FREQUENCIES
        for (band in freqs.indices) {
            val eqBand = dp.getPreEqBandByChannelIndex(0, band).apply {
                isEnabled = true
                cutoffFrequency = freqs[band]
                gain = config.eqBands.getOrElse(band) { 0f }
            }
            dp.setPreEqBandAllChannelsTo(band, eqBand)
        }
        val preEq = dp.getPreEqByChannelIndex(0).apply { isEnabled = true }
        dp.setPreEqAllChannelsTo(preEq)
    }

    // ── Compressor + limiter mapping onto DynamicsProcessing's stage model ──────────────────
    // This is the spot with real "how should it sound" judgment (knee width, full-band vs split,
    // limiter attack). The defaults below are a sane musical starting point; tune to taste.

    private fun applyCompressor(dp: DynamicsProcessing, comp: DspConfig.Compressor) {
        val mbcBand = dp.getMbcBandByChannelIndex(0, 0).apply {
            isEnabled = true
            cutoffFrequency = 20_000f // single full-band MBC band
            attackTime = comp.attackMs
            releaseTime = comp.releaseMs
            ratio = comp.ratio
            threshold = comp.thresholdDb
            kneeWidth = 0f
            preGain = 0f
            postGain = 0f
        }
        dp.setMbcBandAllChannelsTo(0, mbcBand)
        val mbc = dp.getMbcByChannelIndex(0).apply { isEnabled = true }
        dp.setMbcAllChannelsTo(mbc)
    }

    private fun applyLimiter(dp: DynamicsProcessing, lim: DspConfig.Limiter) {
        val limiter = dp.getLimiterByChannelIndex(0).apply {
            isEnabled = true
            attackTime = 1f
            releaseTime = lim.releaseMs
            ratio = 10f
            threshold = lim.thresholdDb
            postGain = lim.gainDb
        }
        dp.setLimiterAllChannelsTo(limiter)
    }

    private companion object {
        const val TAG = "DspController"
        const val CHANNEL_COUNT = 2
    }
}
