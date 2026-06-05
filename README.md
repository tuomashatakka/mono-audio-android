# MONO Signal — Android local audio player

A minimal, dark, neon-accented music player for **local audio** on Android. MVP built with
modern Android architecture (Jetpack Compose, Media3, Hilt, coroutines), with UI strictly
separated from state.

## Features

- **Library** — scans on-device audio via `MediaStore` (READ_MEDIA_AUDIO).
- **Now Playing** — tap the artwork to cycle three graphics:
  1. **Album art**
  2. **3D waveform terrain** — Serum-style wavetable, a scrolling stack of live captures.
  3. **FFT spectrum** — log-spaced frequency bars.
- **Waveform seek bar** — the track's *real* amplitude envelope (decoded + cached), with a
  neon gradient progress fill and drag-to-seek.
- **Background playback** — Media3 `ExoPlayer` in a `MediaSessionService` with system media
  notification / lock-screen controls.
- **Live visualizer** — `android.media.audiofx.Visualizer` attached to the player's audio
  session (requires RECORD_AUDIO).
- Minimal dark UI with the **MONO Signal** design tokens: neon edge gradients, hairline
  borders, tracked monospace labels, corner brackets.

## Architecture

Single `:app` module, package-structured for KISS and easy extraction later:

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Model | `model/` | Pure data: `Track`, `PlaybackState`, `VisualizerFrame`, `NowPlayingGraphic` |
| Data | `data/` | `MediaStoreSource`, `MusicRepository`, `waveform/` (decode + cache) |
| Playback | `playback/` | `PlaybackService`, `PlayerController` (MediaController → `StateFlow`), `AudioVisualizer`, pure `VisualizerDsp` |
| UI | `ui/` | Compose `theme/`, `components/`, `library/`, `nowplaying/`, `nav/` |
| State | `viewmodel/` | `LibraryViewModel`, `NowPlayingViewModel` — expose immutable `StateFlow` UI state |

Composables are stateless (state in, callbacks out). The only place Media3 player types are
touched is `PlayerController`. DSP math (`VisualizerDsp`, `WaveformReducer`) is Android-free
and unit-tested on the JVM.

## Build & run

Requires the Android SDK (platform 35, build-tools 35) and JDK 17+.

```bash
./gradlew :app:assembleDebug          # build debug APK
./gradlew :app:testDebugUnitTest      # run JVM unit tests
adb install app/build/outputs/apk/debug/app-debug.apk
```

- **minSdk 33** (Android 13), **targetSdk 35**.
- On first launch, grant **audio access** (library scan) and **microphone** (the Visualizer
  captures the audio output stream — no recording is stored). The app degrades gracefully if
  either is denied.

## Not in this MVP

Search, queue, settings, artists/playlists tabs, custom bundled fonts. The structure leaves
room to add these without refactoring.
