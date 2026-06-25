package com.mono.signal.model

/** Selectable neon highlight color (4 variants). */
enum class AccentOption(val label: String) {
    TURQUOISE("Turquoise"),
    CRIMSON("Crimson"),
    VIOLET("Violet"),
    BLUE("Signal blue"),
    AMBER("Amber"),
    LIME("Lime"),
    ROSE("Rose");
}

/** Selectable base background (2 variants): the deep void, or a lighter graphite. */
enum class BackgroundOption(val label: String) {
    VOID("Void"),
    SLATE("Graphite");
}

/** Persisted appearance config, applied app-wide via the theme. */
data class ThemeConfig(
    val accent: AccentOption = AccentOption.TURQUOISE,
    val background: BackgroundOption = BackgroundOption.VOID,
    val fftBlockSize: Int = 2048,
)
