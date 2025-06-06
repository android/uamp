//
//  MusicLibraryView.swift
//  Mixtape
//
//  Main music library view displaying all tracks
//

import SwiftUI

struct MusicLibraryView: View {
    @StateObject private var musicCatalog = MusicCatalog.shared
    @EnvironmentObject var audioManager: AudioManager
    @State private var selectedSortOption = SortOption.title
    
    enum SortOption: String, CaseIterable {
        case title = "Title"
        case artist = "Artist"
        case album = "Album"
        case duration = "Duration"
    }
    
    var sortedTracks: [Track] {
        switch selectedSortOption {
        case .title:
            return musicCatalog.tracks.sorted { $0.title < $1.title }
        case .artist:
            return musicCatalog.tracks.sorted { $0.artist < $1.artist }
        case .album:
            return musicCatalog.tracks.sorted { $0.album < $1.album }
        case .duration:
            return musicCatalog.tracks.sorted { $0.duration < $1.duration }
        }
    }
    
    var body: some View {
        NavigationView {
            Group {
                if musicCatalog.isLoading {
                    LoadingView()
                } else if let errorMessage = musicCatalog.errorMessage {
                    ErrorView(message: errorMessage) {
                        Task {
                            await musicCatalog.loadCatalog()
                        }
                    }
                } else if musicCatalog.tracks.isEmpty {
                    EmptyStateView()
                } else {
                    libraryContent
                }
            }
            .navigationTitle("Mixtape")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Picker("Sort by", selection: $selectedSortOption) {
                            ForEach(SortOption.allCases, id: \.self) { option in
                                Text(option.rawValue)
                                    .tag(option)
                            }
                        }
                    } label: {
                        Image(systemName: "arrow.up.arrow.down")
                    }
                }
                
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        if !musicCatalog.tracks.isEmpty {
                            // Play all tracks shuffled
                            audioManager.setShuffleMode(true)
                            audioManager.playTrack(musicCatalog.tracks.randomElement()!, in: musicCatalog.tracks)
                        }
                    }) {
                        Image(systemName: "shuffle")
                    }
                    .disabled(musicCatalog.tracks.isEmpty)
                }
            }
        }
    }
    
    private var libraryContent: some View {
        List {
            // Stats section
            statsSection
            
            // Tracks section
            Section {
                ForEach(sortedTracks) { track in
                    TrackRowView(track: track) {
                        audioManager.playTrack(track, in: sortedTracks)
                    }
                }
            } header: {
                HStack {
                    Text("All Songs")
                    Spacer()
                    Text("\(sortedTracks.count) tracks")
                        .foregroundColor(.gray)
                        .font(.caption)
                }
            }
        }
        .listStyle(InsetGroupedListStyle())
        .refreshable {
            await musicCatalog.loadCatalog()
        }
    }
    
    private var statsSection: some View {
        Section {
            VStack(spacing: 16) {
                // Play all button
                Button(action: {
                    if let firstTrack = sortedTracks.first {
                        audioManager.playTrack(firstTrack, in: sortedTracks)
                    }
                }) {
                    HStack {
                        Image(systemName: "play.fill")
                        Text("Play All")
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.purple)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(sortedTracks.isEmpty)
                
                // Stats grid
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ], spacing: 16) {
                    StatCardView(
                        icon: "music.note",
                        title: "Songs",
                        value: "\(musicCatalog.tracks.count)"
                    )
                    
                    StatCardView(
                        icon: "person.fill",
                        title: "Artists",
                        value: "\(musicCatalog.uniqueArtists.count)"
                    )
                    
                    StatCardView(
                        icon: "music.note.list",
                        title: "Albums",
                        value: "\(musicCatalog.uniqueAlbums.count)"
                    )
                }
            }
            .padding(.vertical, 8)
        }
    }
}

// MARK: - Supporting Views

struct LoadingView: View {
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)
            
            Text("Loading your music...")
                .font(.headline)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ErrorView: View {
    let message: String
    let retry: () -> Void
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundColor(.orange)
            
            Text("Something went wrong")
                .font(.headline)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button("Try Again", action: retry)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct EmptyStateView: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "music.note.list")
                .font(.largeTitle)
                .foregroundColor(.gray)
            
            Text("No Music Found")
                .font(.headline)
            
            Text("Your music library is empty. Check your network connection and try again.")
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct StatCardView: View {
    let icon: String
    let title: String
    let value: String
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.purple)
            
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

// MARK: - Preview

struct MusicLibraryView_Previews: PreviewProvider {
    static var previews: some View {
        MusicLibraryView()
            .environmentObject(AudioManager.shared)
    }
} 