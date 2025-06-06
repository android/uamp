# Mixtape Player

A modern Android music player with comprehensive Android Auto integration, built on the foundation of Google's Universal Android Music Player (UAMP) sample with significant enhancements for real-world use.

## ✨ Features

### 🎵 **Music Experience**
- **Full-screen album art backgrounds** with dynamic color theming extracted from artwork
- **Gesture controls**: swipe for track navigation, tap to expand, swipe down to dismiss
- **Smart mini player** with auto-hide after 5 seconds of inactivity
- **Complete playback controls**: play, pause, skip, seek, shuffle, repeat modes
- **Remote music catalog** loaded from cloud sources with local fallback

### 🚗 **In-Vehicle Integration** 

#### **Android Auto**
- **Dual service architecture**: 
  - MusicService (MediaSessionService) for phone app compatibility
  - AndroidAutoService (MediaBrowserServiceCompat) for Android Auto browsing
- **Full browsing capability** in Android Auto with searchable music library
- **Now Playing screen** with track metadata, album artwork, and progress tracking
- **Seamless synchronization** between phone app and in-vehicle display
- **Voice command support** through Google Assistant integration

#### **CarPlay (iOS)**
- **Native CarPlay integration** with CPListTemplate for music browsing
- **Tab-based interface** for songs, artists, and albums
- **Now Playing integration** with playback controls and artwork
- **Voice control** through Siri integration
- **Background audio** maintains playback when switching apps

### 📱 **Phone Apps (Android & iOS)**

#### **Android**
- **Material Design 3** with dynamic theming
- **Multiple viewing modes**: library browsing, now playing, mini player
- **Album art integration** throughout the interface
- **Real-time playback state** synchronized across all interfaces

#### **iOS** 
- **SwiftUI implementation** with native iOS design patterns
- **CarPlay integration** for in-vehicle control and browsing
- **Dynamic color theming** extracted from album artwork
- **Background audio** with lock screen and Control Center integration
- **Apple Watch support** and Shortcuts app compatibility
- **Feature parity** with Android version using same music catalog

## 🏗️ Architecture

### **Service Architecture**
```
┌─ Phone App ────────────────┐    ┌─ Android Auto ─────────────┐
│                            │    │                            │
│  MainActivity              │    │  Car Infotainment System   │
│  NowPlayingFragment        │    │  Browse + Now Playing UI   │
│  MiniPlayerFragment        │    │                            │
│                            │    │                            │
└────────────┬───────────────┘    └────────────┬───────────────┘
             │                                 │
             ▼                                 ▼
    ┌────────────────────┐            ┌─────────────────────┐
    │   MusicService     │            │ AndroidAutoService  │
    │ (MediaSessionService)          │ (MediaBrowserService) │
    │                    │            │                     │
    │ - ExoPlayer        │◄───────────┤ - Shared Player     │
    │ - MediaSession     │            │ - MediaSession Sync │
    │ - Notifications    │            │ - Browse Capability │
    └────────────────────┘            └─────────────────────┘
```

### **Key Components**
- **MusicService**: Core playback service using Media3 ExoPlayer
- **AndroidAutoService**: Browser service for Android Auto UI
- **MusicServiceConnection**: Bridge between UI and services
- **JsonSource**: Remote catalog management with network loading
- **UampNotificationManager**: Rich media notifications

## 🚀 Getting Started

### **Prerequisites**
- Android Studio Arctic Fox (2020.3.1) or later
- Android 6.0 (API level 23) or higher
- Android Auto compatible vehicle or Android Auto Desktop Head Unit for testing

### **Building the App**
```bash
# Clone the repository
git clone <repository-url>
cd mixtape-player

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### **Android Auto Testing**

#### **Option 1: Real Vehicle Testing**
1. Connect phone via USB to Android Auto compatible vehicle
2. Launch Mixtape app and start playing music
3. Access through vehicle's Android Auto interface

#### **Option 2: Desktop Head Unit (Development)**
```bash
# Install Android Auto Desktop Head Unit
# Available from: https://developer.android.com/training/cars/testing

# Enable Android Auto developer mode on phone
adb shell am start -n "com.google.android.projection.gearhead/.MainActivity"

# Launch Desktop Head Unit and connect phone
```

## 🛠️ Development

### **Monorepo Structure**
```
├── Android/
│   ├── app/                    # Main Android application module
│   ├── common/                 # Shared Android code and services  
│   ├── automotive/            # Android Automotive OS specific code
│   ├── gradle/                # Gradle wrapper and configuration
│   ├── build.gradle           # Android project build configuration
│   └── settings.gradle        # Android module settings
├── iOS/
│   └── ios-mixtape/           # Complete iOS Xcode project
│       ├── Mixtape/           # Main iOS app source code
│       │   ├── Views/         # SwiftUI views (NowPlaying, MiniPlayer, etc.)
│       │   ├── Services/      # AudioManager, CarPlaySceneDelegate
│       │   └── Models/        # MusicCatalog and data models
│       ├── Mixtape.xcodeproj/ # Xcode project configuration
│       ├── Package.swift      # Swift Package Manager dependencies
│       ├── build.sh           # iOS build and testing script
│       └── README.md          # iOS-specific documentation
└── docs/                      # Shared documentation and guides
```

### **Key Files**
- `MusicService.kt` - Core music playback service
- `AndroidAutoService.kt` - Android Auto browsing integration
- `MusicServiceConnection.kt` - Service communication layer
- `JsonSource.kt` - Music catalog management

### **Testing Android Auto Integration**
1. **Enable Developer Options** on Android device
2. **Install Android Auto** from Play Store
3. **Connect to Desktop Head Unit** or vehicle
4. **Test browsing and playback** functionality

## 🎵 Music Catalog

The app loads music from a remote JSON catalog with fallback to local tracks:
- Dynamic loading from cloud sources
- Automatic retry with exponential backoff
- Local fallback catalog for offline testing
- Support for album artwork URLs

## 🔧 Configuration

### **Android Auto Setup**
The app includes proper Android Auto configuration:
- `automotive_app_desc.xml` - Android Auto app descriptor
- `allowed_media_browser_callers.xml` - Security for media browsing
- Manifest declarations for Android Auto support

### **Media Session Integration**
- Full Media3 MediaSession implementation
- Proper metadata handling for Now Playing display
- Synchronized playback state across all interfaces

## 📖 Documentation

- [Android Auto Integration Guide](docs/android-auto-integration.md)
- [Service Architecture](docs/service-architecture.md)  
- [Original UAMP Documentation](docs/FullGuide.md)

## 🐛 Known Issues

- Shuffle and repeat buttons may not appear in some Android Auto implementations
- Album art loading may be slow on poor network connections
- Some vehicles may have limited Android Auto UI capabilities

## 🤝 Contributing

This project is based on Google's UAMP sample with significant enhancements for Android Auto integration. Contributions welcome!

## 📄 License

Based on Google's Universal Android Music Player sample:

```
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🎵 Music Credits

Music provided by the [Free Music Archive](http://freemusicarchive.org/):
- **The Kyoto Connection** - [Wake Up](http://freemusicarchive.org/music/The_Kyoto_Connection/Wake_Up_1957/)

Ambisonic recordings by [Ambisonic Sound Library](https://library.soundfield.com/).
