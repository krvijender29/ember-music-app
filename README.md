# 🎵 EMBER Music

<div align="center">

![EMBER Header](https://img.shields.io/badge/EMBER-Music%20Player%20v2.4.0-FF4500?style=for-the-badge&logo=fire&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![ExoPlayer](https://img.shields.io/badge/Audio-Media3%20ExoPlayer%201.5.1-1A73E8?style=for-the-badge&logo=google&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**An obsidian-noir luxury audio streaming application for Android.**  
*Engineered for audiophiles. Designed with relentless precision.*

[Key Features](#-key-features) • [Audio Architecture](#-audio-architecture) • [Recommendation Engine](#-recommendation-engine-v20) • [Visual Design System](#-visual-design-system) • [Project Structure](#-project-structure) • [Installation](#-build--installation)

</div>

---

## 🌟 Overview

**EMBER Music** is a premier native Android streaming player built with an unapologetic **obsidian-noir aesthetic** and an uncompromised audio-first architecture. It combines studio-grade 320 kbps stream delivery, resilient multi-source failover engines, real-time synchronized karaoke lyrics, hardware-accelerated sound enhancement, and an intelligent client-side recommendation engine into a fluid, tactile user experience.

Whether listening through headphones or high-fidelity sound systems, EMBER ensures seamless playback, zero interruptions, and breathtaking visual fidelity.

---

## ✨ Key Features

### 🔊 Dual-Engine Resilient Audio Pipeline
- **Direct 320 kbps Lossless CDN Streams**: Direct extraction and DES-ECB link decryption for maximum dynamic range and studio-quality bitrate.
- **Automated YouTube / Invidious Fallback**: Instantaneous multi-instance proxy failover engine. If a primary stream is restricted, EMBER resolves and streams high-bitrate audio with zero interruption.
- **Configurable Streaming Fidelity**: User-selectable bitrates: `320 kbps` (Lossless Hi-Fi), `256 kbps` (High Quality), `160 kbps` (Standard), and `96 kbps` (Data Saver).
- **Background Media3 Service**: Foreground audio service integrated with Android `MediaSessionCompat`, rich notification controls, and lockscreen media management.

### 🎚️ Audiophile Hardware Equalizer & Bass Boost
- **5-Band Parametric Equalizer**: Direct hardware DSP control tailored to your audio device.
- **Sub-Harmonic Bass Booster**: Deep low-end frequency reinforcement without clipping or distortion.
- **Acoustic Presets**: Instant profiles for *Flat*, *Acoustic*, *Bass Booster*, *Electronic*, *Pop*, and *Rock*.

### 🎙️ Synced Karaoke Lyrics (LRCLIB Integration)
- Real-time time-stamped `.lrc` lyrics with millisecond precision.
- Interactive auto-scrolling lyrics view with dynamic highlighting of active phrases.
- Tap-to-seek functionality: jump to any point in the track directly by tapping a lyric line.

### 💿 Vinyl Record Mode (`VinylRecordView`)
- Custom hardware-accelerated Canvas turntable component.
- Features **46 concentric micro-grooves**, optical sheen reflections, and brass spindle gold ring (`#D4AF37`).
- Smooth 33⅓ RPM rotation during active playback that pauses and resumes seamlessly with the music.
- Instant 1-tap toggle between **Cover Art Mode** and **Vinyl Record Mode**.

### 👤 Redesigned Luxury Profile Hub
- **Glowing Avatar Ring & Pro Badge**: Profile identity with camera update trigger and gold `[ 👑 EMBER Pro ]` status badge.
- **Listening Telemetry**: 3-column stats card tracking *Liked Songs*, *Created Playlists*, and *Total Listening Hours*.
- **"My Playlists" Carousel**: Curated playlist carousels with custom vector artwork (Liked Songs, Chill Vibes, Workout, etc.).
- **"Your Activity" Hub**: Dynamic vinyl disc visualization displaying live metrics (Songs Played, Listening Hours, Top Listener %).
- **Obsidian Settings Modal**: Unified bottom sheet controlling Audio Bitrate, Cache Cleaning, Theme, Privacy, and Account settings.

### 🧠 Intelligent Recommendation Engine (v2.0)
- Multi-Armed Bandit (MAB) sampling balancing exploration and exploitation.
- Context-aware Day/Time models adapting to Morning, Afternoon, Evening, and Late Night listening habits.
- Collaborative artist co-occurrence network across 100+ multi-lingual artist seed graphs.
- Dynamic anti-fatigue filtering with exponential recency decay.

### ☁️ Cloud Library & Spotify Importer
- **Spotify Playlist Importer**: Ingest public Spotify playlists directly into your personal EMBER library with automatic metadata matching.
- **Supabase Cloud Sync**: Instant bi-directional synchronization of playlists, liked songs, and playback history.
- **Firebase Auth & Google Sign-In**: Secure authentication with cloud profile persistence.

---

## 🏗️ Audio Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     EMBER AUDIO PIPELINE                     │
└──────────────────────────────────────────────────────────────┘
                               │
                [ Song Playback Requested ]
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   ┌───────────────────┐                 ┌───────────────────┐
   │  Primary Stream   │                 │ Secondary Stream  │
   │  Akamai 320 kbps  │                 │ YouTube Fallback  │
   │  DES-ECB Decrypt  │                 │ Piped / Invidious │
   └─────────┬─────────┘                 └─────────┬─────────┘
             │ (On Stream Failure)                 │
             └─────────────────► ◄─────────────────┘
                               │
                               ▼
               ┌───────────────────────────────┐
               │    AndroidX Media3 ExoPlayer  │
               │   (v1.5.1 Low-Latency Engine) │
               └───────────────┬───────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
   ┌───────────────────┐                 ┌───────────────────┐
   │ Hardware Equalizer│                 │ Bass Boost Engine │
   │  5-Band DSP Node  │                 │ Sub-Harmonic Gain │
   └───────────────────┘                 └───────────────────┘
```

---

## 🧠 Recommendation Engine (v2.0)

EMBER features a client-side algorithmic engine designed to serve music that matches your personal taste:

$$\text{FinalScore}(t) = w_a \cdot A(t) + w_c \cdot C(t) + w_m \cdot M(t) - \lambda \cdot D(t)$$

| Component | Weight | Description |
|:---|:---:|:---|
| **$A(t)$ (Artist Affinity)** | `0.35` | Measures historical listens and favorites matching seed artist graphs. |
| **$C(t)$ (Collaborative Score)** | `0.25` | Co-occurrence network mapping related artists and track pairings. |
| **$M(t)$ (Temporal Context)** | `0.20` | Dynamic daypart scoring (Morning energy, Evening focus, Midnight lofi). |
| **$D(t)$ (Fatigue Penalty)** | `0.20` | Exponential decay penalty to avoid repetitive playback fatigue. |

---

## 🎨 Visual Design System

EMBER uses a signature obsidian-noir theme designed for OLED displays, minimizing eye strain while maximizing visual immersion.

### Color Tokens

| Token Name | Hex Code | Visual Preview | Usage |
|:---|:---:|:---:|:---|
| **Obsidian Base** | `#0B0A0D` | `⬛` | App window background |
| **Surface Dark** | `#171619` | `⬛` | Cards, bottom sheets, navigation |
| **Surface Elevated** | `#211E20` | `⬛` | Floating buttons, chips, pill containers |
| **Ember Primary** | `#F05A3D` | `🟧` | Active toggles, badges, play aura |
| **Text Primary** | `#F7F5F5` | `⬜` | Headlines, titles, active track names |
| **Text Secondary** | `#99969F` | `◽` | Subtitles, artists, timestamps |
| **Spindle Gold** | `#D4AF37` | `🟨` | Vinyl record brass spindle center |
| **Border Subtle** | `#302C30` | `⬛` | Card outlines, dividers, glassy borders |

---

## 📁 Project Structure

```text
EMBER/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/ember/
│   │   │   │   ├── MainActivity.kt               # Central coordinator & UI lifecycle
│   │   │   │   ├── adapter/                      # Fast RecyclerView & ViewPager adapters
│   │   │   │   │   ├── AlbumAdapter.kt
│   │   │   │   │   ├── ArtistAdapter.kt
│   │   │   │   │   ├── HorizontalCardAdapter.kt
│   │   │   │   │   ├── LyricsAdapter.kt
│   │   │   │   │   ├── PlaylistAdapter.kt
│   │   │   │   │   └── TrackAdapter.kt
│   │   │   │   ├── data/                         # Data layer & remote sync
│   │   │   │   │   ├── EmberAuthManager.kt       # Firebase + Google Sign-In
│   │   │   │   │   └── EmberDatabaseManager.kt   # Supabase REST + SQLite cache
│   │   │   │   ├── engine/                       # Core playback & AI engines
│   │   │   │   │   ├── EmberAudioEngine.kt       # Stream resolver & search client
│   │   │   │   │   └── EmberRecommendationEngine.kt # Algorithmic discovery
│   │   │   │   ├── model/                        # Kotlin data classes & contracts
│   │   │   │   │   ├── Playlist.kt
│   │   │   │   │   ├── SearchModels.kt
│   │   │   │   │   ├── Track.kt
│   │   │   │   │   └── User.kt
│   │   │   │   ├── service/                      # Background services
│   │   │   │   │   └── EmberPlaybackService.kt   # Foreground MediaSession service
│   │   │   │   ├── ui/                           # Custom canvas & shader views
│   │   │   │   │   ├── EmberLaserSweepView.kt
│   │   │   │   │   ├── EmberRadialBufferView.kt
│   │   │   │   │   ├── EmberShimmerLayout.kt
│   │   │   │   │   ├── VerticalEqSliderView.kt
│   │   │   │   │   └── VinylRecordView.kt        # 33⅓ RPM rotating turntable
│   │   │   │   └── util/                         # Blur, thumbnails & crypto helpers
│   │   │   │       ├── BlurUtils.kt
│   │   │   │       └── ThumbnailLoadingDrawable.kt
│   │   │   └── res/                              # Layouts, vector drawables & themes
│   └── build.gradle.kts                          # Module dependencies & SDK config
├── gradle/
│   └── libs.versions.toml                        # Version catalog (Dependencies)
├── build.gradle.kts                              # Root build script
├── settings.gradle.kts                           # Module settings
└── README.md                                     # Project documentation
```

---

## 🛠️ Tech Stack & Dependencies

| Category | Technology / Library | Version | Purpose |
|:---|:---|:---:|:---|
| **Core** | [Kotlin](https://kotlinlang.org/) | `1.9+` | Modern type-safe Android development |
| **Audio Engine** | [AndroidX Media3 ExoPlayer](https://developer.android.com/media/media3) | `1.5.1` | Low-latency audio streaming & queue management |
| **Session Control** | [Media3 Session](https://developer.android.com/media/media3) | `1.5.1` | MediaSessionCompat & Lockscreen controls |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) | `2.7.0` | Memory-efficient artwork loading with blur transforms |
| **Networking** | [OkHttp](https://square.github.io/okhttp/) | `4.12.0` | Resilient HTTP requests, stream failover & decryption |
| **Authentication** | [Firebase Auth](https://firebase.google.com/docs/auth) | `33.5.1` | Cloud user identities & Google Sign-In |
| **Cloud Database** | [Supabase REST](https://supabase.com/) | Cloud API | Remote playlist sync & liked track persistence |
| **Concurrency** | [Kotlin Coroutines & Flow](https://github.com/Kotlin/kotlinx.coroutines) | `1.8.1` | Non-blocking asynchronous reactive workflows |
| **UI Components** | [Google Material Components](https://github.com/material-components/material-components-android) | `1.10.0` | Bottom sheets, cards, chips, dialogs |

---

## 🚀 Build & Installation

### Prerequisites
- **Android Studio**: Ladybug (2024.2+) or Meerkat
- **JDK**: Java 17+ (e.g., Android Studio bundled JetBrains Runtime)
- **Android SDK**: `compileSdk = 37`, `minSdk = 24` (Supports Android 7.0 Nougat to Android 15+)

### Clone the Repository
```bash
git clone https://github.com/krvijender29/ember-music-app.git
cd ember-music-app
```

### Build the Debug APK
```powershell
# Windows (PowerShell)
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install Directly to Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions & Security

EMBER requires only standard media-related permissions:
- `android.permission.INTERNET`: Streaming music streams and fetching synced lyrics.
- `android.permission.ACCESS_NETWORK_STATE`: Detecting Wi-Fi / Cellular conditions to adapt bitrates.
- `android.permission.FOREGROUND_SERVICE`: Continuous background audio playback with low memory kill immunity.
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required on Android 14+ for foreground playback.
- `android.permission.POST_NOTIFICATIONS`: Android 13+ playback notification bar controls.
- `android.permission.WAKE_LOCK`: Audio stream wake-locks and optional `AWAKE` bedside display mode.

---

## 🤝 Contributing

Contributions, feature suggestions, and feedback are welcomed!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

<div align="center">
<sub>Crafted with passion for pure sound. © 2026 EMBER Music.</sub>
</div>
