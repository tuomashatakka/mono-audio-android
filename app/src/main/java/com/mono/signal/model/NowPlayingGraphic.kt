package com.mono.signal.model

/** The three graphics the Now Playing artwork area cycles through on tap. */
enum class NowPlayingGraphic(val label: String) {
    ALBUM_ART("ART"),
    WAVE_3D("WAVE 3D"),
    FFT("SPECTRUM");

    fun next(): NowPlayingGraphic = entries[(ordinal + 1) % entries.size]
}
