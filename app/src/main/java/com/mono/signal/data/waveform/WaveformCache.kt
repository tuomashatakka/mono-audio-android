package com.mono.signal.data.waveform

import android.content.Context
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-tier cache for decoded amplitude envelopes: an in-memory [LruCache] backed by a
 * small per-track file under the app cache dir, so envelopes survive process restarts
 * without re-decoding.
 */
@Singleton
class WaveformCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val memory = LruCache<Long, FloatArray>(64)
    private val dir = File(context.cacheDir, "waveforms").apply { mkdirs() }

    fun get(trackId: Long): FloatArray? {
        memory.get(trackId)?.let { return it }
        return readFile(trackId)?.also { memory.put(trackId, it) }
    }

    fun put(trackId: Long, envelope: FloatArray) {
        memory.put(trackId, envelope)
        writeFile(trackId, envelope)
    }

    private fun fileFor(trackId: Long) = File(dir, "$trackId.wf")

    private fun readFile(trackId: Long): FloatArray? {
        val file = fileFor(trackId)
        if (!file.exists()) return null
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                val size = input.readInt()
                FloatArray(size) { input.readFloat() }
            }
        }.getOrNull()
    }

    private fun writeFile(trackId: Long, envelope: FloatArray) {
        runCatching {
            DataOutputStream(fileFor(trackId).outputStream().buffered()).use { out ->
                out.writeInt(envelope.size)
                for (v in envelope) out.writeFloat(v)
            }
        }
    }
}
