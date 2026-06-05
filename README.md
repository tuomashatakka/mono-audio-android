# MONO Audio Android

MVP Android music player for local audio, written in Kotlin with a small modern architecture:

- **UI:** Jetpack Compose, dark minimal MONO-inspired layout, neon gradient panel edges, FFT display, and waveform progress seeking.
- **State:** `PlayerViewModel` exposes a single immutable `PlayerUiState` via `StateFlow`.
- **Data:** `LocalAudioRepository` indexes local music from Android `MediaStore`.
- **Playback:** Media3 ExoPlayer powers local audio playback.
- **Visualizer:** Android `Visualizer` streams live FFT bands for the Compose graph.

## Project layout

```text
app/src/main/java/com/mono/audio/
├── data/       # MediaStore model + repository
├── player/     # ExoPlayer and Visualizer wrappers
├── ui/         # state + ViewModel
└── ui/components and ui/theme
```

## Build

```bash
gradle :app:assembleDebug
```

An Android SDK must be available through `ANDROID_HOME` or `local.properties` (`sdk.dir=...`).
