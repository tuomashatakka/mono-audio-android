package com.mono.signal.model

/**
 * One captured slice of live audio output.
 *
 * @param fftBands legacy peak spectrum values, retained as an alias for older callers/tests.
 * @param fftPeakBands normalized (0..1) log-spaced per-band peak magnitudes.
 * @param fftRmsBands normalized (0..1) log-spaced per-band RMS magnitudes.
 * @param waveform normalized (-1..1) time-domain samples for the current capture window.
 */
data class VisualizerFrame(
    val fftBands: FloatArray,
    val waveform: FloatArray,
    val fftPeakBands: FloatArray = fftBands,
    val fftRmsBands: FloatArray = FloatArray(fftBands.size),
) {
    companion object {
        fun empty(bands: Int = 48, points: Int = 96) =
            VisualizerFrame(FloatArray(bands), FloatArray(points), FloatArray(bands), FloatArray(bands))
    }

    // FloatArray needs explicit equals/hashCode for data-class semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualizerFrame) return false
        return fftBands.contentEquals(other.fftBands) &&
            waveform.contentEquals(other.waveform) &&
            fftPeakBands.contentEquals(other.fftPeakBands) &&
            fftRmsBands.contentEquals(other.fftRmsBands)
    }

    override fun hashCode(): Int {
        var result = fftBands.contentHashCode()
        result = 31 * result + waveform.contentHashCode()
        result = 31 * result + fftPeakBands.contentHashCode()
        result = 31 * result + fftRmsBands.contentHashCode()
        return result
    }
}
