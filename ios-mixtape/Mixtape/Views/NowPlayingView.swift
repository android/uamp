//
//  NowPlayingView.swift
//  Mixtape
//
//  Full-screen now playing view with dynamic colors and gestures
//

import SwiftUI

struct NowPlayingView: View {
    @Binding var isPresented: Bool
    @EnvironmentObject var audioManager: AudioManager
    @State private var isDragging = false
    @State private var dominantColor: Color = .purple
    @State private var backgroundGradient: LinearGradient = LinearGradient(
        colors: [.purple.opacity(0.8), .black],
        startPoint: .top,
        endPoint: .bottom
    )
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Background with album art
                backgroundView
                
                // Content overlay
                VStack {
                    // Top bar
                    topBar
                    
                    Spacer()
                    
                    // Bottom controls
                    bottomControls
                        .padding(.bottom, geometry.safeAreaInsets.bottom + 20)
                }
                .padding(.horizontal, 20)
            }
        }
        .ignoresSafeArea()
        .gesture(
            DragGesture()
                .onChanged { value in
                    isDragging = true
                }
                .onEnded { value in
                    isDragging = false
                    
                    // Handle swipe gestures
                    if value.translation.y > 100 {
                        // Swipe down to dismiss
                        isPresented = false
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
    }
    
    private var backgroundView: some View {
        Group {
            if let track = audioManager.currentTrack {
                AsyncImage(url: URL(string: track.albumArtURL)) { phase in
                    switch phase {
                    case .success(let image):
                        GeometryReader { geometry in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: geometry.size.width, height: geometry.size.height)
                                .clipped()
                                .overlay(
                                    backgroundGradient
                                )
                                .onAppear {
                                    extractDominantColor(from: image)
                                }
                        }
                    case .failure(_), .empty:
                        defaultBackground
                    @unknown default:
                        defaultBackground
                    }
                }
            } else {
                defaultBackground
            }
        }
    }
    
    private var defaultBackground: some View {
        LinearGradient(
            colors: [dominantColor.opacity(0.8), .black],
            startPoint: .top,
            endPoint: .bottom
        )
    }
    
    private var topBar: some View {
        HStack {
            Button(action: {
                isPresented = false
            }) {
                Image(systemName: "chevron.down")
                    .font(.title2)
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Text("Now Playing")
                .font(.headline)
                .foregroundColor(.white)
            
            Spacer()
            
            Button(action: {
                // Add to favorites or show menu
            }) {
                Image(systemName: "ellipsis")
                    .font(.title2)
                    .foregroundColor(.white)
            }
        }
        .padding(.top, 10)
    }
    
    private var bottomControls: some View {
        VStack(spacing: 20) {
            // Track info
            if let track = audioManager.currentTrack {
                VStack(spacing: 8) {
                    Text(track.title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                    
                    Text(track.artist)
                        .font(.title3)
                        .foregroundColor(.white.opacity(0.8))
                        .multilineTextAlignment(.center)
                        .lineLimit(1)
                    
                    Text(track.album)
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .lineLimit(1)
                }
                .padding(.horizontal)
            }
            
            // Progress slider
            progressSlider
            
            // Control buttons
            controlButtons
        }
        .padding()
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 20))
    }
    
    private var progressSlider: some View {
        VStack(spacing: 8) {
            Slider(
                value: Binding(
                    get: { audioManager.currentTime },
                    set: { audioManager.seek(to: $0) }
                ),
                in: 0...max(audioManager.duration, 1),
                onEditingChanged: { editing in
                    isDragging = editing
                }
            )
            .accentColor(.white)
            
            HStack {
                Text(audioManager.formattedCurrentTime)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
                
                Spacer()
                
                Text(audioManager.formattedDuration)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
            }
        }
    }
    
    private var controlButtons: some View {
        VStack(spacing: 20) {
            // Secondary controls
            HStack(spacing: 40) {
                Button(action: audioManager.toggleShuffle) {
                    Image(systemName: audioManager.isShuffleEnabled ? "shuffle" : "shuffle")
                        .font(.title2)
                        .foregroundColor(audioManager.isShuffleEnabled ? .white : .white.opacity(0.5))
                }
                
                Spacer()
                
                Button(action: audioManager.toggleRepeat) {
                    Image(systemName: repeatIcon)
                        .font(.title2)
                        .foregroundColor(audioManager.repeatMode != .none ? .white : .white.opacity(0.5))
                }
            }
            
            // Main controls
            HStack(spacing: 30) {
                Button(action: audioManager.skipPrevious) {
                    Image(systemName: "backward.fill")
                        .font(.title)
                        .foregroundColor(.white)
                }
                
                Button(action: {
                    if audioManager.isPlaying {
                        audioManager.pause()
                    } else {
                        audioManager.play()
                    }
                }) {
                    Image(systemName: audioManager.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 60))
                        .foregroundColor(.white)
                }
                
                Button(action: audioManager.skipNext) {
                    Image(systemName: "forward.fill")
                        .font(.title)
                        .foregroundColor(.white)
                }
            }
        }
    }
    
    private var repeatIcon: String {
        switch audioManager.repeatMode {
        case .none:
            return "repeat"
        case .one:
            return "repeat.1"
        case .all:
            return "repeat"
        }
    }
    
    private func extractDominantColor(from image: Image) {
        // Convert SwiftUI Image to UIImage for color extraction
        let renderer = ImageRenderer(content: image)
        if let uiImage = renderer.uiImage {
            extractDominantColor(from: uiImage)
        }
    }
    
    private func extractDominantColor(from uiImage: UIImage) {
        Task {
            let dominantUIColor = await getDominantColor(from: uiImage)
            await MainActor.run {
                dominantColor = Color(dominantUIColor)
                updateBackgroundGradient()
            }
        }
    }
    
    private func updateBackgroundGradient() {
        backgroundGradient = LinearGradient(
            colors: [
                dominantColor.opacity(0.8),
                dominantColor.opacity(0.4),
                .black
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
    
    private func getDominantColor(from image: UIImage) async -> UIColor {
        return await withCheckedContinuation { continuation in
            DispatchQueue.global(qos: .background).async {
                guard let inputImage = CIImage(image: image) else {
                    continuation.resume(returning: .purple)
                    return
                }
                
                let extentVector = CIVector(x: inputImage.extent.origin.x,
                                          y: inputImage.extent.origin.y,
                                          z: inputImage.extent.size.width,
                                          w: inputImage.extent.size.height)
                
                guard let filter = CIFilter(name: "CIAreaAverage",
                                          parameters: [kCIInputImageKey: inputImage,
                                                     kCIInputExtentKey: extentVector]) else {
                    continuation.resume(returning: .purple)
                    return
                }
                
                guard let outputImage = filter.outputImage else {
                    continuation.resume(returning: .purple)
                    return
                }
                
                var bitmap = [UInt8](repeating: 0, count: 4)
                let context = CIContext(options: [.workingColorSpace: kCFNull as Any])
                context.render(outputImage,
                             toBitmap: &bitmap,
                             rowBytes: 4,
                             bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
                             format: .RGBA8,
                             colorSpace: nil)
                
                let color = UIColor(red: CGFloat(bitmap[0]) / 255,
                                  green: CGFloat(bitmap[1]) / 255,
                                  blue: CGFloat(bitmap[2]) / 255,
                                  alpha: 1)
                
                continuation.resume(returning: color)
            }
        }
    }
}

// MARK: - Preview

struct NowPlayingView_Previews: PreviewProvider {
    static var previews: some View {
        NowPlayingView(isPresented: .constant(true))
            .environmentObject(AudioManager.shared)
    }
} 