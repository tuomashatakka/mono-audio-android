package com.mono.signal.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.mono.signal.data.DspPreferences
import com.mono.signal.model.DspConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user's [DspConfig] (12-band EQ → compressor → limiter) to the player's PCM inside
 * the audio sink, replacing the old [android.media.audiofx.DynamicsProcessing] approach that
 * silently failed on emulators and many OEM devices. Processing the samples ourselves guarantees
 * the DSP screen audibly does what it says on every device.
 *
 * Config changes reach the audio thread through a single `@Volatile` reference collected from
 * [DspPreferences.config] (same singleton-with-own-scope pattern as [com.mono.signal.playback.PcmAudioTap]);
 * coefficients are recomputed at buffer boundaries, which keeps slider drags click-free enough in
 * practice since each drag emits many small deltas.
 *
 * The processor stays active even while the master switch is off — the sink's processing pipeline
 * only re-evaluates the active set on reconfigure/flush, so an `isActive`-based bypass could not
 * toggle mid-stream. Disabled means a bit-exact copy instead.
 */
@Singleton
class MonoDspAudioProcessor @Inject constructor(
    preferences: DspPreferences,
) : BaseAudioProcessor() {

    @Volatile private var config: DspConfig = preferences.config.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch { preferences.config.collect { config = it } }
    }

    // Playback-thread state: which config snapshot the chain's coefficients currently reflect.
    private var appliedConfig: DspConfig? = null
    private var chain: DspChain? = null

    // Grow-only per-channel scratch buffers so steady-state processing never allocates.
    private var channels: Array<FloatArray> = emptyArray()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val supported = (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) &&
            inputAudioFormat.channelCount in 1..2
        if (!supported) {
            // Unsupported layouts (e.g. 5.1 content) pass through un-processed rather than
            // failing playback: NOT_SET marks this processor inactive for the stream.
            return AudioProcessor.AudioFormat.NOT_SET
        }
        chain = DspChain(inputAudioFormat.channelCount)
        appliedConfig = null
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val cfg = config
        if (!cfg.enabled) {
            replaceOutputBuffer(remaining).put(inputBuffer).flip()
            return
        }

        val chain = checkNotNull(chain)
        if (cfg !== appliedConfig) {
            chain.configure(inputAudioFormat.sampleRate, cfg)
            appliedConfig = cfg
        }

        val channelCount = inputAudioFormat.channelCount
        val float = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val bytesPerFrame = (if (float) 4 else 2) * channelCount
        val frameCount = remaining / bytesPerFrame
        ensureScratch(channelCount, frameCount)

        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        if (float) {
            for (i in 0 until frameCount) {
                for (ch in 0 until channelCount) channels[ch][i] = input.float
            }
        } else {
            for (i in 0 until frameCount) {
                for (ch in 0 until channelCount) channels[ch][i] = input.short / 32768f
            }
        }
        input.position(input.limit())

        chain.process(channels, frameCount)

        val output = replaceOutputBuffer(frameCount * bytesPerFrame).order(ByteOrder.LITTLE_ENDIAN)
        if (float) {
            for (i in 0 until frameCount) {
                for (ch in 0 until channelCount) output.putFloat(channels[ch][i])
            }
        } else {
            for (i in 0 until frameCount) {
                for (ch in 0 until channelCount) {
                    // The chain clamps to ±1, so this stays within PCM16 range.
                    output.putShort((channels[ch][i] * 32767f).toInt().toShort())
                }
            }
        }
        output.flip()
    }

    override fun onFlush() {
        // Called on seeks and after configure: stale filter state must not ring into new audio.
        chain?.reset()
    }

    override fun onReset() {
        chain = null
        appliedConfig = null
        channels = emptyArray()
    }

    private fun ensureScratch(channelCount: Int, frameCount: Int) {
        if (channels.size != channelCount || channels[0].size < frameCount) {
            channels = Array(channelCount) { FloatArray(frameCount) }
        }
    }
}
