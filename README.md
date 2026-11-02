# EMBER Music — Premium Android Audio Streaming Application

EMBER Music is an obsidian-noir luxury audio streaming application for Android built with a resilient **dual-engine audio pipeline** (direct 320kbps Akamai CDN streams paired with an automated multi-source YouTube audio fallback), synced karaoke lyrics powered by LRCLIB, a hardware-accelerated 5-band audio equalizer with sub-harmonic bass boost, dynamic album-art ambient mesh glow, an instant Spotify playlist importer, and full Supabase cloud library synchronization.

---

## 1. Visual Design & Now Playing Screen Specifications

The Now Playing screen is recreated to pixel perfection according to the official design reference:

### Palette & Tokens
* **Main Background**: `#0B0A0D` (Deep obsidian black)
* **Surface**: `#171619`
* **Secondary Surface**: `#211E20`
* **Primary Text**: `#F7F5F5`
* **Secondary Text**: `#99969F`
* **Primary Accent**: `#F05A3D` (Ember orange-red)
* **Border**: `#302C30` / `#1E1C22`
* **Vinyl Surface**: `#090909`
* **Spindle Gold Ring**: `#D4AF37`
* **Upper Ambient Glow**: `#803D1F17` -> `#D9160E12` -> `#FF0B0A0D`

---

## 2. Key Screen Components

### A. Top Header
* **Drag Handle**: Centered pill handle (`38dp` × `4dp`).
* **Left**: Circular translucent button with downward chevron icon to collapse the player sheet.
* **Center**: `NOW PLAYING` in uppercase letter-spaced bold typography (`#99969F`).
* **Right**: Circular translucent button with horizontal 3-dots icon opening a comprehensive bottom sheet menu:
  - Add to Playlist
  - View Album
  - View Artist
  - Share Track (native Android share sheet)
  - Song & Audio Quality Information
  - Copy 320 kbps Stream URL

### B. Dynamic 1:1 Album Artwork Container
* Enclosed in `layoutArtworkWrapper` with an `OnLayoutChangeListener` dynamically enforcing:
  $$\text{squareSide} = \min(\text{availWidth}, \text{availHeight})$$
* Centered with `Gravity.CENTER`, guaranteeing a true 1:1 square aspect ratio without vertical stretching or side cropping on any display size or aspect ratio (including 20:9 devices).
* Corner radius: `28dp` with subtle border stroke `#302C30` and elevation.

### C. Vinyl Mode vs. Cover Art Mode
* **Cover Art Mode**: Displays full square album cover with rounded corners. In the lower-right corner, a dark translucent pill button with disc icon reads `"Vinyl Mode"`.
* **Vinyl Mode**: Smoothly displays `VinylRecordView`, a custom Canvas component rendering:
  - Jet-black base disc (`#090909`) with outer edge rim
  - 46 concentric micro-grooves with varied stroke weights and optical depths
  - Dual specular reflection sheen simulating studio overhead lighting
  - Circular album art center label (40% diameter) with a golden brass center spindle ring (`#D4AF37`) and dark center hole
  - Smooth continuous rotation animation at 33 1/3 RPM while music is playing
  - Pauses rotation in place when playback is paused; resumes seamlessly when playback resumes
* In the lower-right corner, the pill button switches to solid orange-red background (`#F05A3D`) with disc icon reading `"Cover Art"`.
* Tapping the pill switches between modes instantly without interrupting playback.

### D. Track Info & Favorite Heart
* Bold white song title (`#F7F5F5`, `24sp`) and muted gray artist names (`#99969F`, `15sp`).
* Circular favorite button with thin border:
  - When liked: Filled orange-red heart (`#F05A3D`) with glowing accent background (`#261416`).
  - When unliked: Outlined muted gray heart (`#99969F`) with dark surface background (`#1A181C`).
  - Tapping toggles state, persists locally, and synchronizes with Supabase `liked_songs`.

### E. Seek Bar & Timestamps
* Custom seek bar with inactive track (`#2C292E`), active track (`#FFFFFF`), and white circular thumb with an orange-red ambient glow ring (`#40F05A3D`).
* Timestamps: `0:00` current position and `3:26` total duration in `#99969F`.
* Scrubbing updates playback position immediately.

### F. Playback Controls
* Horizontal row of 5 balanced controls:
  1. **Shuffle**: Toggles shuffle with `#F05A3D` active tint.
  2. **Previous**: Skips to previous track in queue.
  3. **Play/Pause**: Prominent white circular button (`70dp`) surrounded by a soft ambient white glow aura (`84dp`, `circle_play_ambient_glow`), with black play triangle / pause bars.
  4. **Next**: Skips to next track in queue.
  5. **Repeat**: Cycles Repeat Off, Repeat All, Repeat One with `#F05A3D` active tint.

### G. Audio Badges & Divider
* Thin horizontal divider line (`#1E1C22`).
* **`LOSSLESS 320 KBPS`**: Orange-red text (`#F05A3D`), dark translucent pill background, thin accent border.
* **`AWAKE`**: Sun icon + `"AWAKE"` in `#F05A3D`. Tapping toggles `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` on the window to keep the screen on while listening.

### H. Bottom Utility Buttons (Row of 4)
* Four rounded rectangular tiles (`bottom_tile_bg`, `#171619`, border `#2A262C`, corner radius `20dp`, height `54dp`) with vertical orientation (icon on top, label below):
  1. **🎙 Lyrics**: Opens LRCLIB synced karaoke lyrics sheet with live auto-scroll and highlight.
  2. **🎚 EQ**: Opens 5-band hardware equalizer dialog with 5 presets + Bass Boost.
  3. **🌙 Timer**: Opens Sleep Timer dialog (15m, 30m, 45m, 60m, end of track) with live countdown.
  4. **≣ Queue**: Opens interactive Queue sheet with reordering and tap-to-play.

---

## 3. Architecture & Tech Stack

* **Platform**: Native Android (Kotlin)
* **Audio Engine**: AndroidX Media3 ExoPlayer (`1.5.1`)
* **Audio Effects**: Android Hardware `Equalizer` and `BassBoost`
* **Image Loading**: Coil (`2.7.0`)
* **Network & Ingestion**: OkHttp (`4.12.0`), DES ECB Decryption for JioSaavn CDN streams, Multi-instance Invidious/Piped YouTube Audio Fallback
* **Cloud & Auth**: Supabase PostgreSQL REST API, Firebase Auth (`33.5.1`), Google Sign-In (`21.2.0`)
* **System Bars & Insets**: `WindowInsetsCompat` edge-to-edge status bar & navigation bar handling

---

## 4. Build & Installation

### Prerequisites
* Android Studio Ladybug / Meerkat or newer
* JDK 17 / JBR (Java 17+)
* Android SDK 35+ (compileSdk: 37, minSdk: 24)

### Building the Debug APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk` (~19.69 MB)
