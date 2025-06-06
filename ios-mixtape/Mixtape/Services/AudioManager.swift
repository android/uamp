//
//  AudioManager.swift
//  Mixtape
//
//  Core audio management service using AVFoundation
//

import Foundation
import AVFoundation
import MediaPlayer
import Combine

enum RepeatMode: CaseIterable {
    case none, one, all
    
    var next: RepeatMode {
        switch self {
        case .none: return .one
        case .one: return .all
        case .all: return .none
        }
    }
}

class AudioManager: NSObject, ObservableObject {
    static let shared = AudioManager()
    
    @Published var isPlaying = false
    @Published var currentTrack: Track?
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    @Published var isShuffleEnabled = false
    @Published var repeatMode: RepeatMode = .none
    @Published var playlist: [Track] = []
    @Published var currentIndex = 0
    
    private var player: AVPlayer?
    private var timeObserver: Any?
    private var originalPlaylist: [Track] = []
    private var shuffledPlaylist: [Track] = []
    
    override init() {
        super.init()
        setupNotifications()
    }
    
    deinit {
        if let timeObserver = timeObserver {
            player?.removeTimeObserver(timeObserver)
        }
        NotificationCenter.default.removeObserver(self)
    }
    
    // MARK: - Public Methods
    
    func loadCatalog() async {
        let catalog = await MusicCatalog.shared.loadCatalog()
        DispatchQueue.main.async {
            self.originalPlaylist = catalog
            self.playlist = catalog
        }
    }
    
    func playTrack(_ track: Track, in playlist: [Track] = []) {
        let playlistToUse = playlist.isEmpty ? self.playlist : playlist
        
        self.originalPlaylist = playlistToUse
        updatePlaylist()
        
        if let index = self.playlist.firstIndex(where: { $0.id == track.id }) {
            currentIndex = index
            playCurrentTrack()
        }
    }
    
    func play() {
        player?.play()
        isPlaying = true
        updateNowPlayingInfo()
    }
    
    func pause() {
        player?.pause()
        isPlaying = false
        updateNowPlayingInfo()
    }
    
    func skipNext() {
        switch repeatMode {
        case .one:
            // Repeat current track
            seek(to: 0)
            return
        case .none, .all:
            if currentIndex < playlist.count - 1 {
                currentIndex += 1
            } else if repeatMode == .all {
                currentIndex = 0
            } else {
                // End of playlist, stop playing
                pause()
                return
            }
        }
        playCurrentTrack()
    }
    
    func skipPrevious() {
        if currentTime > 3.0 {
            // If more than 3 seconds played, restart current track
            seek(to: 0)
        } else {
            // Go to previous track
            if currentIndex > 0 {
                currentIndex -= 1
            } else if repeatMode == .all {
                currentIndex = playlist.count - 1
            } else {
                // At beginning of playlist
                seek(to: 0)
                return
            }
            playCurrentTrack()
        }
    }
    
    func seek(to time: TimeInterval) {
        let cmTime = CMTime(seconds: time, preferredTimescale: 1000)
        player?.seek(to: cmTime)
        currentTime = time
        updateNowPlayingInfo()
    }
    
    func setShuffleMode(_ enabled: Bool) {
        isShuffleEnabled = enabled
        updatePlaylist()
    }
    
    func setRepeatMode(_ mode: RepeatMode) {
        repeatMode = mode
        updateNowPlayingInfo()
    }
    
    func toggleShuffle() {
        setShuffleMode(!isShuffleEnabled)
    }
    
    func toggleRepeat() {
        setRepeatMode(repeatMode.next)
    }
    
    // MARK: - Private Methods
    
    private func playCurrentTrack() {
        guard currentIndex < playlist.count else { return }
        
        let track = playlist[currentIndex]
        currentTrack = track
        
        guard let url = URL(string: track.audioURL) else {
            print("Invalid URL for track: \(track.title)")
            return
        }
        
        let playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        
        setupTimeObserver()
        
        // Start playing
        play()
        
        // Update duration when ready
        Task {
            await updateTrackDuration()
        }
        
        updateNowPlayingInfo()
    }
    
    private func updatePlaylist() {
        let currentTrackId = currentTrack?.id
        
        if isShuffleEnabled {
            if shuffledPlaylist.isEmpty || shuffledPlaylist.count != originalPlaylist.count {
                shuffledPlaylist = originalPlaylist.shuffled()
            }
            playlist = shuffledPlaylist
        } else {
            playlist = originalPlaylist
        }
        
        // Update current index to match the current track in the new playlist
        if let trackId = currentTrackId,
           let newIndex = playlist.firstIndex(where: { $0.id == trackId }) {
            currentIndex = newIndex
        }
    }
    
    private func setupTimeObserver() {
        // Remove existing observer
        if let timeObserver = timeObserver {
            player?.removeTimeObserver(timeObserver)
        }
        
        // Add new observer
        let interval = CMTime(seconds: 0.1, preferredTimescale: 1000)
        timeObserver = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            self?.currentTime = time.seconds
        }
    }
    
    private func updateTrackDuration() async {
        guard let player = player,
              let currentItem = player.currentItem else { return }
        
        let status = currentItem.status
        if status == .readyToPlay {
            let duration = currentItem.duration.seconds
            if !duration.isNaN && !duration.isInfinite {
                DispatchQueue.main.async {
                    self.duration = duration
                }
            }
        }
    }
    
    private func setupNotifications() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(playerDidFinishPlaying),
            name: .AVPlayerItemDidPlayToEndTime,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(playerStalled),
            name: .AVPlayerItemPlaybackStalled,
            object: nil
        )
    }
    
    @objc private func playerDidFinishPlaying() {
        // Automatically skip to next track when current track finishes
        skipNext()
    }
    
    @objc private func playerStalled() {
        // Handle playback stalls - could implement buffering UI here
        print("Playback stalled")
    }
    
    private func updateNowPlayingInfo() {
        guard let track = currentTrack else { return }
        
        var nowPlayingInfo = [String: Any]()
        nowPlayingInfo[MPMediaItemPropertyTitle] = track.title
        nowPlayingInfo[MPMediaItemPropertyArtist] = track.artist
        nowPlayingInfo[MPMediaItemPropertyAlbumTitle] = track.album
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentTime
        nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = duration
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        
        // Set shuffle and repeat mode
        nowPlayingInfo[MPNowPlayingInfoPropertyShuffleMode] = isShuffleEnabled ? MPNowPlayingInfoShuffleMode.items.rawValue : MPNowPlayingInfoShuffleMode.off.rawValue
        
        switch repeatMode {
        case .none:
            nowPlayingInfo[MPNowPlayingInfoPropertyRepeatMode] = MPNowPlayingInfoRepeatMode.off.rawValue
        case .one:
            nowPlayingInfo[MPNowPlayingInfoPropertyRepeatMode] = MPNowPlayingInfoRepeatMode.one.rawValue
        case .all:
            nowPlayingInfo[MPNowPlayingInfoPropertyRepeatMode] = MPNowPlayingInfoRepeatMode.all.rawValue
        }
        
        // Load album artwork asynchronously
        if let artworkURL = URL(string: track.albumArtURL) {
            Task {
                do {
                    let (data, _) = try await URLSession.shared.data(from: artworkURL)
                    if let image = UIImage(data: data) {
                        let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
                        nowPlayingInfo[MPMediaItemPropertyArtwork] = artwork
                        
                        DispatchQueue.main.async {
                            MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
                        }
                    }
                } catch {
                    print("Failed to load album artwork: \(error)")
                }
            }
        }
        
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }
}

// MARK: - Extensions

extension AudioManager {
    var formattedCurrentTime: String {
        return formatTime(currentTime)
    }
    
    var formattedDuration: String {
        return formatTime(duration)
    }
    
    private func formatTime(_ time: TimeInterval) -> String {
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
} 