# Sonique

<div align="center">

**A powerful, minimal, and ad-free YouTube Music client for Android**

[![Version](https://img.shields.io/badge/version-1.0.1-blue.svg)](https://github.com/07-Ansh/Sonique)
[![License](https://img.shields.io/badge/license-GPL--3.0-green.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.10.0-orange.svg)](https://developer.android.com/jetpack/compose)

Built with **Jetpack Compose** and **Kotlin Multiplatform** for a premium music streaming experience

[Features](#-features) • [Tech Stack](#-tech-stack) • [Architecture](#-architecture) • [Download](#-download) • [Building](#-building-from-source)

</div>

---

## ✨ Features

### 🎵 **Core Music Features**
- **🚫 Ad-Free Experience** - Enjoy uninterrupted music without any advertisements
- **🎧 Background Playback** - Continue listening while using other apps or with screen off
- **📥 Offline Downloads** - Save songs and playlists for offline listening
- **🎬 Video Mode** - Toggle between high-quality audio and 1080p music videos
- **📜 Synced Lyrics** - Real-time lyrics from multiple sources (YouTube, Spotify, LRCLIB)
- **🎼 Queue Management** - Smart playlist and queue controls

### 🎨 **User Interface**
- **Dynamic Material You Design** - Adaptive theming based on album artwork
- **Tablet Support** - Optimized UI for tablets with navigation rail
- **Landscape Mode** - Seamless experience in all orientations
- **Mini Player** - Swipeable compact player with drag gestures
- **Dark Mode** - Beautiful dark theme optimized for OLED displays

### 📊 **Advanced Features**
- **Music Analytics** - Track listening habits and most played tracks
- **🚗 Android Auto** - Full Android Auto integration for in-car usage
- **🌚 Sleep Timer** - Built-in timer to automatically stop playback
- **🔒 SponsorBlock Integration** - Skip sponsored segments automatically
- **📱 Home Screen Widget** - Control playback from your home screen
- **🌐 Multi-Language** - Support for 25+ languages

### 🔐 **Privacy & Control**
- **Zero Trackers** - No analytics or third-party tracking
- **Local-First Storage** - All data stored locally on your device
- **Optional Google Sync** - Opt-in listening history sync with YouTube Music
- **Content Filtering** - Skip "Music Off-topic" segments

> **⚠️ Note:** This app uses undocumented YouTube Music APIs. While generally stable, occasional playback issues may occur due to API changes.

---

## 🛠️ Tech Stack

### **Core Technologies**

| Component | Technology | Version |
|-----------|-----------|---------|
| 🎨 **UI Framework** | Jetpack Compose Multiplatform | 1.10.0 |
| 💜 **Language** | Kotlin | 2.2.21 |
| 🎭 **Material Design** | Material 3 | 1.10.0 |
| 🏗️ **Architecture** | MVVM / MVI | - |
| 💉 **Dependency Injection** | Koin | 4.1.1 |
| 🧭 **Navigation** | Navigation Compose | 2.9.1 |

### **Media & Networking**

- **🎵 Media Playback:** Media3/ExoPlayer 1.8.0
- **🌐 HTTP Client:** Ktor 3.3.3
- **🖼️ Image Loading:** Coil 3.3.0
- **🎥 Video Extraction:** ytdlp-android 0.18.1
- **🔊 Audio Processing:** FFmpeg Kit Audio 6.0.1

### **Data & Storage**

- **💾 Database:** Room 2.8.4
- **⚙️ Preferences:** DataStore 1.2.0
- **📄 Pagination:** Paging 3 (3.4.0)
- **🔄 Serialization:** Kotlinx Serialization

### **UI Enhancements**

- **🎨 Color Extraction:** KMPalette 3.1.0
- **✨ Animations:** Compottie (Lottie) 2.0.2
- **🌫️ Blur Effects:** Haze 1.7.1
- **📊 Markdown:** Multiplatform Markdown Renderer

---

## 🏗️ Architecture

**Architecture Pattern:** **MVVM (Model-View-ViewModel)** with elements of **MVI (Model-View-Intent)**
- **Views:** Jetpack Compose screens and components
- **ViewModels:** State management with Kotlin StateFlows
- **Models:** Domain entities and data transfer objects
- **Repository Pattern:** Data layer abstraction

### **Module Structure**

Sonique follows **Clean Architecture** principles with a multi-module structure:

```
sonique/
├── composeApp/          # Main Android app with UI
├── core/
│   ├── common/          # Shared utilities
│   ├── data/            # Repository implementations
│   ├── domain/          # Business logic & use cases
│   ├── media/           # Media3 integration
│   └── service/         # External service integrations
│       ├── ktorExt/     # Ktor extensions
│       ├── kotlinYtmusicScraper/  # YouTube Music API
│       ├── spotify/     # Spotify integration
│       └── lyricsService/  # Lyrics fetching
└── MediaServiceCore/    # Shared media service modules
```

### **Design Patterns**
- **MVVM/MVI:** Unidirectional data flow with ViewModels
- **Repository Pattern:** Abstract data sources
- **Dependency Injection:** Koin for loose coupling
- **Clean Architecture:** Separation of concerns across layers

---

## 📊 Data Sources

### **Music Streaming**
- **YouTube Music** (Undocumented APIs) - Primary music source
- **NewPipe Extractor** - YouTube data extraction
- **SmartTube** techniques - Streaming URL extraction

### **Lyrics & Metadata**
- **YouTube Music** - Official lyrics when available
- **Spotify Web API** - Canvas animations and lyrics
- **LRCLIB** - Synced lyrics database

### **Enhancements**
- **SponsorBlock** - Skip sponsored video segments
- **Return YouTube Dislike** - Community voting data

> 💡 **Credits:** Inspired by [InnerTune](https://github.com/z-huang/InnerTune/) and [SmartTube](https://github.com/yuliskov/SmartTube)

---

## 🔒 Privacy Policy

**Your privacy matters.**

- ✅ **No tracking or analytics** - Zero telemetry
- ✅ **No ads or ad networks** - Completely ad-free
- ✅ **Local data storage** - Everything stored on device
- ✅ **Open source** - Transparent codebase
- ⚙️ **Optional YouTube sync** - Opt-in listening history (helps recommendations)

When "Send back to Google" is enabled, Sonique uses the official YouTube Music API to sync playback history, supporting artists and improving recommendations.

---

## 📥 Download

### **Requirements**
- **Android 8.0 (API 26)** or higher
- **~50MB** storage space
- **Internet connection** for streaming

### **Installation**
1. Download the latest APK from [Releases](../../releases)
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK
4. Enjoy ad-free music! 🎵

> **Note:** This app is not available on Google Play Store due to its nature of bypassing YouTube Music's official client.

---

## 🛠️ Building from Source

### **Prerequisites**
```bash
- Android Studio Ladybug or later
- JDK 17
- Android SDK 36
- Kotlin 2.2.21
```

### **Build Variants**
- **Debug:** Development build with logging (`app-debug.apk`)
- **Release:** Optimized production build with ProGuard

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. 🐛 **Report Bugs** - Open an issue with detailed reproduction steps
2. 💡 **Suggest Features** - Share your ideas in discussions
3. 🔧 **Submit PRs** - Fix bugs or implement features
4. 📖 **Improve Docs** - Help with documentation
5. 🌐 **Translations** - Add support for more languages

### **Development Guidelines**
- Follow Kotlin coding conventions
- Use Jetpack Compose best practices
- Write meaningful commit messages
- Test on multiple Android versions

---

## 📄 License

```
Copyright (C) 2025 Ansh Sharma

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

See [LICENSE](LICENSE) file for full details.

---

## 🙏 Acknowledgments

- **[InnerTune](https://github.com/z-huang/InnerTune/)** - Inspiration for YouTube Music API extraction
- **[SmartTube](https://github.com/yuliskov/SmartTube)** - Streaming URL extraction techniques
- **[NewPipe](https://github.com/TeamNewPipe/NewPipeExtractor)** - YouTube data extraction
- **[SponsorBlock](https://sponsor.ajay.app/)** - Sponsored segment database
- **[LRCLIB](https://lrclib.net/)** - Lyrics database

---

## 👨‍💻 Developer

**Ansh Sharma**

## ☕ Support the Project

If you find Sonique useful and want to support its development:

<div align="center">

<a href="https://buymeacoffee.com/07Ansh">
  <img src="asset/qr-code.png" width="200" alt="Buy Me A Coffee QR Code">
</a>

**[buymeacoffee.com/07Ansh](https://buymeacoffee.com/07Ansh)**

Your support helps keep this project alive! ❤️

</div>

---

<div align="center">

**Built with ❤️ using [Kotlin](https://kotlinlang.org/) • Fueled by coffee ☕**

⭐ **Star this repo if you like it!** ⭐

</div>
