package com.mono.signal.data.waveform

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.mono.signal.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Decodes a local audio file to PCM via [MediaExtractor] + [MediaCodec] and reduces it
 * to a compact peak-per-bucket amplitude envelope. Runs entirely off the main thread.
 */
@Singleton
class WaveformExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cache: WaveformCache,
) {
    /** Returns a cached envelope if present, otherwise decodes and caches it. */
    suspend fun envelopeFor(track: Track): FloatArray = withContext(Dispatchers.Default) {
        cache.get(track.id)?.let { return@withContext it }
        val envelope = runCatching { decode(track.uri) }.getOrNull()
            ?: FloatArray(WaveformReducer.DEFAULT_BUCKETS)
        cache.put(track.id, envelope)
        envelope
    }

    private fun decode(uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        val trackIndex = selectAudioTrack(extractor)
            ?: return FloatArray(WaveformReducer.DEFAULT_BUCKETS)
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: return FloatArray(WaveformReducer.DEFAULT_BUCKETS)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION))
            format.getLong(MediaFormat.KEY_DURATION) else 0L
        val totalFrames = max(1L, (durationUs / 1_000_000.0 * sampleRate).toLong())

        val accumulator = WaveformReducer.Accumulator(totalFrames)
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false

        try {
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(inBuf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (outIndex >= 0) {
                    if (info.size > 0) {
                        val outBuf = codec.getOutputBuffer(outIndex)!!
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        feed(outBuf, info.size, channels, accumulator)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            extractor.release()
        }
        return accumulator.build()
    }

    private fun feed(buffer: ByteBuffer, size: Int, channels: Int, acc: WaveformReducer.Accumulator) {
        val shorts = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val count = size / 2
        val samples = ShortArray(count)
        shorts.get(samples, 0, count)
        acc.add(samples, count, channels)
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
