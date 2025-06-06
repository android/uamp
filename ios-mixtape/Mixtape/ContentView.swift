//
//  ContentView.swift
//  Mixtape
//
//  Main app content view with navigation and mini player
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var audioManager: AudioManager
    @StateObject private var musicCatalog = MusicCatalog.shared
    @State private var selectedTab = 0
    @State private var showingNowPlaying = false
    
    var body: some View {
        ZStack {
            // Main content
            TabView(selection: $selectedTab) {
                MusicLibraryView()
                    .tabItem {
                        Image(systemName: "music.note.list")
                        Text("Library")
                    }
                    .tag(0)
                
                SearchView()
                    .tabItem {
                        Image(systemName: "magnifyingglass")
                        Text("Search")
                    }
                    .tag(1)
                
                PlaylistsView()
                    .tabItem {
                        Image(systemName: "music.note")
                        Text("Playlists")
                    }
                    .tag(2)
            }
            .accentColor(.purple)
            
            // Mini player overlay
            VStack {
                Spacer()
                
                if audioManager.currentTrack != nil {
                    MiniPlayerView(showingNowPlaying: $showingNowPlaying)
                        .padding(.horizontal)
                        .padding(.bottom, 90) // Space above tab bar
                }
            }
            .ignoresSafeArea(.keyboard)
        }
        .fullScreenCover(isPresented: $showingNowPlaying) {
            NowPlayingView(isPresented: $showingNowPlaying)
        }
        .onAppear {
            // Load catalog when app appears
            Task {
                await musicCatalog.loadCatalog()
            }
        }
    }
}

// MARK: - Search View

struct SearchView: View {
    @StateObject private var musicCatalog = MusicCatalog.shared
    @EnvironmentObject var audioManager: AudioManager
    @State private var searchText = ""
    
    var filteredTracks: [Track] {
        musicCatalog.searchTracks(searchText)
    }
    
    var body: some View {
        NavigationView {
            VStack {
                SearchBar(text: $searchText)
                    .padding(.horizontal)
                
                List(filteredTracks) { track in
                    TrackRowView(track: track) {
                        audioManager.playTrack(track, in: filteredTracks)
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("Search")
        }
    }
}

// MARK: - Search Bar

struct SearchBar: View {
    @Binding var text: String
    
    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search songs, artists, albums...", text: $text)
                .textFieldStyle(RoundedBorderTextFieldStyle())
        }
    }
}

// MARK: - Playlists View

struct PlaylistsView: View {
    @StateObject private var musicCatalog = MusicCatalog.shared
    
    var body: some View {
        NavigationView {
            List {
                Section("Artists") {
                    ForEach(musicCatalog.uniqueArtists, id: \.self) { artist in
                        NavigationLink(destination: ArtistView(artist: artist)) {
                            HStack {
                                Image(systemName: "person.circle.fill")
                                    .foregroundColor(.purple)
                                    .font(.title2)
                                
                                VStack(alignment: .leading) {
                                    Text(artist)
                                        .font(.headline)
                                    Text("\(musicCatalog.tracksByArtist(artist).count) songs")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                            }
                        }
                    }
                }
                
                Section("Albums") {
                    ForEach(musicCatalog.uniqueAlbums, id: \.self) { album in
                        NavigationLink(destination: AlbumView(album: album)) {
                            HStack {
                                Image(systemName: "music.note.list")
                                    .foregroundColor(.purple)
                                    .font(.title2)
                                
                                VStack(alignment: .leading) {
                                    Text(album)
                                        .font(.headline)
                                    Text("\(musicCatalog.tracksByAlbum(album).count) songs")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                            }
                        }
                    }
                }
            }
            .navigationTitle("Browse")
        }
    }
}

// MARK: - Artist View

struct ArtistView: View {
    let artist: String
    @StateObject private var musicCatalog = MusicCatalog.shared
    @EnvironmentObject var audioManager: AudioManager
    
    var artistTracks: [Track] {
        musicCatalog.tracksByArtist(artist)
    }
    
    var body: some View {
        List(artistTracks) { track in
            TrackRowView(track: track) {
                audioManager.playTrack(track, in: artistTracks)
            }
        }
        .navigationTitle(artist)
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: - Album View

struct AlbumView: View {
    let album: String
    @StateObject private var musicCatalog = MusicCatalog.shared
    @EnvironmentObject var audioManager: AudioManager
    
    var albumTracks: [Track] {
        musicCatalog.tracksByAlbum(album)
    }
    
    var body: some View {
        List(albumTracks) { track in
            TrackRowView(track: track) {
                audioManager.playTrack(track, in: albumTracks)
            }
        }
        .navigationTitle(album)
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: - Track Row View

struct TrackRowView: View {
    let track: Track
    let onTap: () -> Void
    @EnvironmentObject var audioManager: AudioManager
    
    var isCurrentTrack: Bool {
        audioManager.currentTrack?.id == track.id
    }
    
    var body: some View {
        Button(action: onTap) {
            HStack {
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
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(track.title)
                        .font(.headline)
                        .foregroundColor(isCurrentTrack ? .purple : .primary)
                        .lineLimit(1)
                    
                    Text(track.artist)
                        .font(.subheadline)
                        .foregroundColor(.gray)
                        .lineLimit(1)
                    
                    Text(track.album)
                        .font(.caption)
                        .foregroundColor(.gray)
                        .lineLimit(1)
                }
                
                Spacer()
                
                VStack {
                    if isCurrentTrack && audioManager.isPlaying {
                        Image(systemName: "speaker.wave.2.fill")
                            .foregroundColor(.purple)
                            .font(.caption)
                    }
                    
                    Text(track.formattedDuration)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Preview

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(AudioManager.shared)
    }
} 