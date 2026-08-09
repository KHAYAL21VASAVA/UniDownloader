# UniDownloader — Native Android Application

**Vibe coded by Khayal and Antigravity**

A personal-use universal media downloader for Android. UniDownloader allows users to paste publicly accessible media links, inspect available video/audio stream qualities, select desired format containers (`.mp4`, `.mkv`, `.webm`, `.mp3`, `.m4a`, `.opus`), and download files directly into the device's public **Downloads** directory using modern Android Scoped Storage and MediaStore APIs.

---

## Key Features

* **Universal Dual-Engine Media Extraction**:
  * **Primary Platform Engine**: `youtubedl-android` (wrapping `yt-dlp`, embedded Python runtime, and FFmpeg native binaries) supporting hundreds of media platforms.
  * **Native High-Speed Engine**: Direct stream metadata parsing, OpenGraph, HTML5 video extraction, and streaming bridge.
* **Lossless Stream Muxing**: FFmpeg stream copy (`-c copy`) merges separate video and audio streams without CPU-intensive transcoding.
* **Audio Extraction**: Direct audio conversion to `.mp3`, `.m4a`, or `.opus` with bitrate controls (320k, 256k, 192k, 128k).
* **Android Scoped Storage & MediaStore**:
  * Files save directly into `Downloads/` (`Environment.DIRECTORY_DOWNLOADS`).
  * Automatic collision incrementing: `Video (1).mp4`, `Video (2).mp4`.
  * Preserves original media title and sanitizes invalid filesystem characters.
  * Media scanner integration so files appear immediately in Gallery, VLC, and Files apps.
* **Real-Time Multi-Stage Progress**:
  * Accurate percentage, download speed (`MB/s`), total & downloaded bytes, and remaining time (`ETA: MM:SS`).
  * Pipeline stage indicators: `Fetching stream info` → `Downloading video` → `Downloading audio` → `Merging streams` → `Saving to Downloads`.
* **Downloads History & System Integration**:
  * **Open**: Launch downloaded files in system media players.
  * **Share**: Native Android Share Sheet via `FileProvider`.
  * **Delete**: Delete media directly from storage.
* **Sleek Material 3 Dark Media Utility Design**: Tailored dark palette with electric blue and vibrant cyan accents, rounded cards, and responsive navigation.

---

## Technology Stack

* **Language**: Kotlin 2.1+ / Java 17
* **UI**: Jetpack Compose & Material 3 (Material You / Dynamic Color compatible)
* **Architecture**: MVVM, Repository Pattern, Kotlin Coroutines, StateFlow
* **Network & Streaming**: OkHttp 4.12+ with real-time byte tracking
* **Image Caching**: Coil Compose 2.7+
* **Engines**: `io.github.junkfood02.youtubedl-android` (`library` + `ffmpeg`)
* **Storage**: Android `MediaStore.Downloads`, `Environment.DIRECTORY_DOWNLOADS`, `FileProvider`
* **Target SDK**: Android 35 (Android 15), Min SDK: Android 26 (Android 8.0)

---

## Project Structure

```text
UniDownloader/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/unidownloader/app/
│   │       │   ├── UniDownloaderApp.kt               // Application entry point & engine init
│   │       │   ├── MainActivity.kt                   // Activity with edge-to-edge Compose scaffold
│   │       │   │
│   │       │   ├── ui/
│   │       │   │   ├── navigation/
│   │       │   │   │   └── Screen.kt                 // Navigation routes
│   │       │   │   ├── screens/
│   │       │   │   │   ├── HomeScreen.kt             // URL input, platform chips, analyze trigger
│   │       │   │   │   ├── MediaInfoScreen.kt        // Thumbnail, title, quality & format dropdowns
│   │       │   │   │   ├── DownloadScreen.kt         // Active downloads with real-time progress
│   │       │   │   │   ├── DownloadsScreen.kt        // Completed downloads history with Open/Share/Delete
│   │       │   │   │   └── SettingsScreen.kt         // Preferences & credits
│   │       │   │   ├── components/
│   │       │   │   │   ├── UrlInput.kt               // Rounded paste input field
│   │       │   │   │   ├── MediaCard.kt              // 16:9 thumbnail preview
│   │       │   │   │   ├── QualitySelector.kt        // Material 3 exposed dropdowns
│   │       │   │   │   ├── DownloadProgressCard.kt   // Animated progress card with live speed & ETA
│   │       │   │   │   ├── DownloadItemRow.kt        // History item with popup menu
│   │       │   │   │   └── BottomNavBar.kt           // Material 3 bottom navigation
│   │       │   │   └── theme/
│   │       │   │       ├── Color.kt                  // Sleek dark media utility palette
│   │       │   │       ├── Theme.kt                  // Material 3 theme provider
│   │       │   │       └── Type.kt                   // Typography scale
│   │       │   │
│   │       │   ├── data/
│   │       │   │   ├── model/
│   │       │   │   │   ├── MediaInfo.kt              // Media metadata
│   │       │   │   │   ├── MediaFormat.kt            // Stream formats
│   │       │   │   │   ├── DownloadTask.kt           // Real-time task progress
│   │       │   │   │   ├── DownloadHistoryItem.kt    // Persisted download records
│   │       │   │   │   └── AppSettings.kt            // Settings preferences
│   │       │   │   └── repository/
│   │       │   │       ├── DownloadRepository.kt     // Reactive StateFlow store
│   │       │   │       └── SettingsRepository.kt     // SharedPreferences / DataStore
│   │       │   │
│   │       │   ├── downloader/
│   │       │   │   ├── DownloadManager.kt            // Download pipeline coordinator
│   │       │   │   ├── MediaExtractor.kt             // Dual-engine extraction coordinator
│   │       │   │   ├── NativeStreamExtractor.kt      // Direct stream fallback extractor
│   │       │   │   └── DownloadService.kt            // Foreground service for background downloads
│   │       │   │
│   │       │   ├── ffmpeg/
│   │       │   │   └── FFmpegManager.kt              // Stream muxing and audio extraction
│   │       │   │
│   │       │   ├── storage/
│   │       │   │   └── DownloadStorage.kt            // Scoped Storage & MediaStore writer
│   │       │   │
│   │       │   ├── notification/
│   │       │   │   └── NotificationHelper.kt         // Android notifications helper
│   │       │   │
│   │       │   └── utils/
│   │       │       ├── FileNameUtils.kt          // Sanitization & duplicate numbering
│   │       │       ├── FormatUtils.kt            // Byte & speed formatting
│   │       │       └── UrlUtils.kt               // URL validation & platform detection
│   │       │
│   │       ├── res/                              // XML drawables, colors, strings, adaptive icons
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## How to Build and Run in Android Studio

1. Open **Android Studio**.
2. Select **Open** and navigate to:
   `path/../../../../UniDownloader`
3. Allow Gradle to sync dependencies.
4. Select a connected device or Android Virtual Device (AVD).
5. Click **Run 'app'** (`Shift + F10`) to build and deploy the APK.

To build via terminal:
```bash
./gradlew assembleDebug
```
The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

*Vibe coded by Khayal and Antigravity.*
