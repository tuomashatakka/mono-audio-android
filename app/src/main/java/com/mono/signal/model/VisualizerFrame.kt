package com.mono.signal.model

/**
 * One captured slice of live audio output.
 *
 * @param fftBands normalized (0..1) log-spaced spectrum magnitudes, ready to draw as bars.
 * @param waveform normalized (-1..1) time-domain samples for the current capture window.
 */
data class VisualizerFrame(
    val fftBands: FloatArray,
    val waveform: FloatArray,
) {
    companion object {
        fun empty(bands: Int = 48, points: Int = 64) =
            VisualizerFrame(FloatArray(bands), FloatArray(points))
    }

    // FloatArray needs explicit equals/hashCode for data-class semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualizerFrame) return false
        return fftBands.contentEquals(other.fftBands) &&
            waveform.contentEquals(other.waveform)
    }

    override fun hashCode(): Int = 31 * fftBands.contentHashCode() + waveform.contentHashCode()
}
