package com.mono.audio.player

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.hypot
import kotlin.math.ln

class VisualizerEngine {
    fun fft(audioSessionId: Int): Flow<List<Float>> = callbackFlow {
        if (audioSessionId == 0) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val visualizer = runCatching {
            Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange().last()
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null) trySend(toBands(fft))
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true,
                )
                enabled = true
            }
        }.getOrElse {
            trySend(emptyList())
            close(it)
            return@callbackFlow
        }

        awaitClose { visualizer.release() }
    }

    private fun toBands(fft: ByteArray, bands: Int = 36): List<Float> {
        if (fft.size < 4) return emptyList()
        val bins = (fft.size / 2).coerceAtLeast(1)
        val values = FloatArray(bands)
        for (band in 0 until bands) {
            val start = (band * bins / bands).coerceAtLeast(1)
            val end = (((band + 1) * bins / bands).coerceAtLeast(start + 1)).coerceAtMost(bins)
            var sum = 0f
            for (i in start until end) {
                val real = fft[i * 2].toFloat()
                val imaginary = fft[i * 2 + 1].toFloat()
                sum += ln(1f + hypot(real, imaginary)) / 6f
            }
            values[band] = (sum / (end - start)).coerceIn(0f, 1f)
        }
        return values.toList()
    }
}
