# Mixtape - iOS Music Player

A beautiful, feature-rich music streaming app for iOS with CarPlay support. This is the iOS port of the Android Mixtape music player, maintaining the same design philosophy and functionality.

## ✨ Features

### 🎵 Core Music Experience
- **Full-screen album art backgrounds** with dynamic color theming
- **Gesture-based navigation** (swipe for tracks, tap to expand)
- **Smart mini player** with auto-hide functionality
- **High-quality audio streaming** from remote catalog
- **Beautiful UI animations** and smooth transitions

### 🎛️ Playback Controls
- Play, pause, skip, seek controls
- **Shuffle mode** with visual feedback
- **Repeat modes**: None, One, All with cycling
- **Background audio playback** support
- **Lock screen controls** integration

### 📱 iOS Integration
- **CarPlay support** for in-car music experience
- **Control Center integration** with Now Playing info
- **Apple Watch controls** (via system integration)
- **Shortcuts app support** for automation
- **iOS 15+ modern design language**

### 🚗 CarPlay Features
- **Browseable music library** organized by songs, artists, albums
- **Full playback controls** including shuffle and repeat
- **Safe driving interface** optimized for car displays
- **Voice control support** through Siri integration
- **Automatic Now Playing display**

## 🛠 Technical Architecture

### Core Technologies
- **SwiftUI** for modern, declarative UI
- **AVFoundation** for high-quality audio playback
- **MediaPlayer framework** for system integration
- **CarPlay framework** for automotive interface
- **Combine** for reactive programming

### Key Components

#### AudioManager
- Centralized audio playback service using `AVPlayer`
- Manages playlist, shuffle, repeat modes
- Handles remote control commands
- Updates Now Playing info for system integration

#### MusicCatalog
- Loads and manages music catalog from remote JSON
- Provides search and filtering capabilities
- Organizes tracks by artist and album
- Handles network loading and error states

#### Views Architecture
- **ContentView**: Main app navigation with tab interface
- **MusicLibraryView**: Browse all tracks with sorting options
- **NowPlayingView**: Full-screen player with dynamic theming
- **MiniPlayerView**: Compact overlay player
- **CarPlaySceneDelegate**: CarPlay interface management

## 🚀 Getting Started

### Prerequisites
- Xcode 15.0 or later
- iOS 15.0 or later deployment target
- macOS for development
- Apple Developer account (for device testing/CarPlay)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd mixtape-player/ios-mixtape
   ```

2. **Open in Xcode**
   ```bash
   open Mixtape.xcodeproj
   ```

3. **Configure signing**
   - Select your development team in project settings
   - Update bundle identifier if needed
   - Ensure CarPlay capability is enabled

4. **Build and run**
   - Select target device/simulator
   - Press ⌘+R to build and run

### CarPlay Testing

#### Simulator Testing
1. **Enable CarPlay Simulator**
   - iOS Simulator → I/O → External Displays → CarPlay
   
2. **Test CarPlay interface**
   - Launch app on iOS Simulator
   - Play music to see Now Playing
   - Browse library through CarPlay interface

#### Device Testing
1. **Physical CarPlay**
   - Connect iPhone to CarPlay-enabled vehicle
   - Launch Mixtape app
   - Access through CarPlay interface

2. **CarPlay Simulator Box**
   - Use official Apple CarPlay simulator
   - Connect via USB/Lightning
   - Test all CarPlay functionality

## 📐 Project Structure

```
ios-mixtape/
├── Mixtape.xcodeproj/          # Xcode project configuration
├── Mixtape/                    # Main app source
│   ├── MixtapeApp.swift        # App entry point
│   ├── ContentView.swift       # Main navigation
│   ├── Views/                  # UI components
│   │   ├── NowPlayingView.swift
│   │   ├── MiniPlayerView.swift
│   │   └── MusicLibraryView.swift
│   ├── Models/                 # Data models
│   │   └── MusicCatalog.swift
│   ├── Services/               # Core services
│   │   ├── AudioManager.swift
│   │   └── CarPlaySceneDelegate.swift
│   ├── Resources/              # Assets and resources
│   └── Info.plist             # App configuration
├── MixtapeTests/              # Unit tests
├── MixtapeUITests/            # UI tests
└── README.md                  # This file
```

## 🎨 Design Features

### Dynamic Color Theming
- Automatically extracts dominant colors from album artwork
- Applies color palette throughout the interface
- Smooth color transitions between tracks
- Maintains accessibility and readability

### Gesture Controls
- **Tap mini player**: Expand to full screen
- **Swipe down**: Dismiss full screen player
- **Swipe left/right**: Skip tracks
- **Swipe down mini player**: Hide temporarily
- **Long press**: Context menus and additional actions

### Auto-Hide Mini Player
- Intelligently hides when not actively used
- Reappears on track changes or user interaction
- Customizable timing and behavior
- Preserves UI cleanliness

## 🔧 Configuration

### Audio Session
The app configures AVAudioSession for optimal playback:
- **Category**: `.playback` for music streaming
- **Options**: AirPlay, Bluetooth, CarPlay support
- **Background modes**: Continuous audio playback

### CarPlay Entitlements
Required entitlements for CarPlay functionality:
- `com.apple.developer.carplay-audio` for audio apps
- CarPlay scene configuration in Info.plist
- Proper audio session setup

### Info.plist Keys
```xml
<key>NSAppleMusicUsageDescription</key>
<string>Access music for streaming and playback</string>

<key>UIBackgroundModes</key>
<array>
    <string>audio</string>
</array>

<key>UIApplicationSceneManifest</key>
<dict>
    <!-- CarPlay scene configuration -->
</dict>
```

## 🧪 Testing

### Unit Tests
- AudioManager functionality
- MusicCatalog data handling
- Model validation and parsing

### UI Tests
- Navigation flows
- Playback controls
- CarPlay interface interaction

### Manual Testing
- Different network conditions
- Various audio formats
- CarPlay integration scenarios

## 📦 Building for Release

### App Store Preparation
1. **Update version numbers**
   - CFBundleShortVersionString (marketing version)
   - CFBundleVersion (build number)

2. **Configure release signing**
   - Distribution certificate
   - App Store provisioning profile

3. **Archive and validate**
   - Product → Archive
   - Validate through Xcode Organizer

### CarPlay App Store Review
- Submit CarPlay entitlement request to Apple
- Provide CarPlay interface screenshots
- Document CarPlay functionality in App Store notes

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the Android project LICENSE file for details.

## 🙏 Acknowledgments

- Original Android UAMP project by Google
- The Kyoto Connection for sample music tracks
- Silent Partner for additional audio content
- Apple for CarPlay framework and documentation

## 📞 Support

For issues and questions:
- Create GitHub issues for bugs
- Check existing issues before creating new ones
- Provide detailed reproduction steps
- Include device and iOS version information

---

Built with ❤️ using SwiftUI and modern iOS technologies 