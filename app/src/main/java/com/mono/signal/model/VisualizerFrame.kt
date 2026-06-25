package com.mono.signal.model

/**
 * One captured slice of live audio output.
 *
 * @param fftBands legacy peak spectrum values, retained as an alias for older callers/tests.
 * @param fftPeakBands normalized (0..1) log-spaced per-band peak magnitudes.
 * @param fftRmsBands normalized (0..1) log-spaced per-band RMS magnitudes.
 * @param waveform normalized (-1..1) time-domain samples (mono mix) for the current capture window.
 * @param waveformLeft normalized (-1..1) left-channel samples. Defaults to the mono [waveform].
 * @param waveformRight normalized (-1..1) right-channel samples. Defaults to the mono [waveform].
 */
data class VisualizerFrame(
    val fftBands: FloatArray,
    val waveform: FloatArray,
    val fftPeakBands: FloatArray = fftBands,
    val fftRmsBands: FloatArray = FloatArray(fftBands.size),
    val waveformLeft: FloatArray = waveform,
    val waveformRight: FloatArray = waveform,
) {
    companion object {
        fun empty(bands: Int = 48, points: Int = 96) =
            VisualizerFrame(
                fftBands = FloatArray(bands),
                waveform = FloatArray(points),
                fftPeakBands = FloatArray(bands),
                fftRmsBands = FloatArray(bands),
                waveformLeft = FloatArray(points),
                waveformRight = FloatArray(points),
            )
    }

    // FloatArray needs explicit equals/hashCode for data-class semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisualizerFrame) return false
        return fftBands.contentEquals(other.fftBands) &&
            waveform.contentEquals(other.waveform) &&
            fftPeakBands.contentEquals(other.fftPeakBands) &&
            fftRmsBands.contentEquals(other.fftRmsBands) &&
            waveformLeft.contentEquals(other.waveformLeft) &&
            waveformRight.contentEquals(other.waveformRight)
    }

    override fun hashCode(): Int {
        var result = fftBands.contentHashCode()
        result = 31 * result + waveform.contentHashCode()
        result = 31 * result + fftPeakBands.contentHashCode()
        result = 31 * result + fftRmsBands.contentHashCode()
        result = 31 * result + waveformLeft.contentHashCode()
        result = 31 * result + waveformRight.contentHashCode()
        return result
    }
}
