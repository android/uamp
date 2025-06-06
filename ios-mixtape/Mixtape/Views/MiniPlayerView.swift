//
//  MiniPlayerView.swift
//  Mixtape
//
//  Mini player overlay that appears at bottom of screen
//

import SwiftUI

struct MiniPlayerView: View {
    @Binding var showingNowPlaying: Bool
    @EnvironmentObject var audioManager: AudioManager
    @State private var isVisible = true
    @State private var hideTimer: Timer?
    
    private let autoHideDelay: TimeInterval = 5.0
    
    var body: some View {
        VStack {
            if let track = audioManager.currentTrack, isVisible {
                miniPlayerContent(track: track)
                    .onTapGesture {
                        showingNowPlaying = true
                    }
                    .gesture(
                        DragGesture()
                            .onEnded { value in
                                if value.translation.y > 50 {
                                    // Swipe down to hide
                                    hidePlayer()
                                } else if abs(value.translation.x) > 100 {
                                    // Horizontal swipes for track navigation
                                    if value.translation.x > 0 {
                                        audioManager.skipPrevious()
                                    } else {
                                        audioManager.skipNext()
                                    }
                                }
                            }
                    )
                    .onAppear {
                        startAutoHideTimer()
                    }
                    .onDisappear {
                        cancelAutoHideTimer()
                    }
            }
        }
        .animation(.easeInOut(duration: 0.3), value: isVisible)
        .onChange(of: audioManager.currentTrack) { _ in
            // Show player when new track starts
            showPlayer()
        }
        .onChange(of: audioManager.isPlaying) { _ in
            // Reset timer when playback state changes
            startAutoHideTimer()
        }
    }
    
    private func miniPlayerContent(track: Track) -> some View {
        HStack(spacing: 12) {
            // Album art
            AsyncImage(url: URL(string: track.albumArtURL)) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Rectangle()
                    .fill(Color.gray.opacity(0.3))
                    .overlay(
                        Image(systemName: "music.note")
                            .foregroundColor(.gray)
                    )
            }
            .frame(width: 50, height: 50)
            .cornerRadius(8)
            
            // Track info
            VStack(alignment: .leading, spacing: 2) {
                Text(track.title)
                    .font(.headline)
                    .lineLimit(1)
                    .foregroundColor(.primary)
                
                Text(track.artist)
                    .font(.subheadline)
                    .lineLimit(1)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            // Control buttons
            HStack(spacing: 15) {
                Button(action: audioManager.skipPrevious) {
                    Image(systemName: "backward.fill")
                        .font(.title3)
                        .foregroundColor(.primary)
                }
                
                Button(action: {
                    if audioManager.isPlaying {
                        audioManager.pause()
                    } else {
                        audioManager.play()
                    }
                }) {
                    Image(systemName: audioManager.isPlaying ? "pause.fill" : "play.fill")
                        .font(.title2)
                        .foregroundColor(.primary)
                }
                
                Button(action: audioManager.skipNext) {
                    Image(systemName: "forward.fill")
                        .font(.title3)
                        .foregroundColor(.primary)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 2)
    }
    
    private func showPlayer() {
        isVisible = true
        startAutoHideTimer()
    }
    
    private func hidePlayer() {
        withAnimation {
            isVisible = false
        }
        cancelAutoHideTimer()
    }
    
    private func startAutoHideTimer() {
        cancelAutoHideTimer()
        
        // Only auto-hide if music is not playing
        guard !audioManager.isPlaying else { return }
        
        hideTimer = Timer.scheduledTimer(withTimeInterval: autoHideDelay, repeats: false) { _ in
            withAnimation {
                isVisible = false
            }
        }
    }
    
    private func cancelAutoHideTimer() {
        hideTimer?.invalidate()
        hideTimer = nil
    }
}

// MARK: - Auto-Hide Mini Player Modifier

struct AutoHideMiniPlayerModifier: ViewModifier {
    let onUserInteraction: () -> Void
    
    func body(content: Content) -> some View {
        content
            .onTapGesture {
                onUserInteraction()
            }
            .onLongPressGesture(minimumDuration: 0) {
                onUserInteraction()
            }
    }
}

extension View {
    func autoHideMiniPlayer(onUserInteraction: @escaping () -> Void) -> some View {
        modifier(AutoHideMiniPlayerModifier(onUserInteraction: onUserInteraction))
    }
}

// MARK: - Preview

struct MiniPlayerView_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            Spacer()
            MiniPlayerView(showingNowPlaying: .constant(false))
                .padding()
        }
        .environmentObject(AudioManager.shared)
        .background(Color.gray.opacity(0.1))
    }
} 