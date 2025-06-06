//
//  MusicCatalog.swift
//  Mixtape
//
//  Music catalog management and Track models
//

import Foundation
import SwiftUI

struct Track: Identifiable, Codable, Hashable {
    let id: String
    let title: String
    let artist: String
    let album: String
    let albumArtURL: String
    let audioURL: String
    let duration: TimeInterval
    
    enum CodingKeys: String, CodingKey {
        case id
        case title
        case artist
        case album
        case albumArtURL = "image"
        case audioURL = "source"
        case duration
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle potential variations in JSON structure
        if let idValue = try? container.decode(String.self, forKey: .id) {
            id = idValue
        } else {
            // Generate ID from title if not provided
            let titleValue = try container.decode(String.self, forKey: .title)
            id = titleValue.lowercased().replacingOccurrences(of: " ", with: "_")
        }
        
        title = try container.decode(String.self, forKey: .title)
        artist = try container.decode(String.self, forKey: .artist)
        album = try container.decode(String.self, forKey: .album)
        albumArtURL = try container.decode(String.self, forKey: .albumArtURL)
        audioURL = try container.decode(String.self, forKey: .audioURL)
        
        // Handle duration - might be string or number
        if let durationInt = try? container.decode(Int.self, forKey: .duration) {
            duration = TimeInterval(durationInt)
        } else if let durationString = try? container.decode(String.self, forKey: .duration) {
            duration = TimeInterval(Int(durationString) ?? 0)
        } else {
            duration = 0
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(title, forKey: .title)
        try container.encode(artist, forKey: .artist)
        try container.encode(album, forKey: .album)
        try container.encode(albumArtURL, forKey: .albumArtURL)
        try container.encode(audioURL, forKey: .audioURL)
        try container.encode(Int(duration), forKey: .duration)
    }
}

struct CatalogResponse: Codable {
    let music: [Track]
}

class MusicCatalog: ObservableObject {
    static let shared = MusicCatalog()
    
    @Published var tracks: [Track] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let catalogURL = "https://storage.googleapis.com/uamp/catalog.json"
    
    private init() {}
    
    func loadCatalog() async -> [Track] {
        await MainActor.run {
            isLoading = true
            errorMessage = nil
        }
        
        guard let url = URL(string: catalogURL) else {
            await MainActor.run {
                errorMessage = "Invalid catalog URL"
                isLoading = false
            }
            return []
        }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let catalogResponse = try JSONDecoder().decode(CatalogResponse.self, from: data)
            
            await MainActor.run {
                self.tracks = catalogResponse.music
                self.isLoading = false
            }
            
            return catalogResponse.music
        } catch {
            await MainActor.run {
                self.errorMessage = "Failed to load catalog: \(error.localizedDescription)"
                self.isLoading = false
            }
            
            // Return default tracks if network fails
            return defaultTracks()
        }
    }
    
    private func defaultTracks() -> [Track] {
        // Fallback tracks if network loading fails
        return [
            Track(
                id: "drop_and_roll",
                title: "Drop and Roll",
                artist: "Silent Partner",
                album: "YouTube Audio Library",
                albumArtURL: "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg",
                audioURL: "https://storage.googleapis.com/uamp/Silent_Partner_-_Drop_and_Roll.mp3",
                duration: 221
            ),
            Track(
                id: "wake_up_01",
                title: "Wake Up 01",
                artist: "The Kyoto Connection",
                album: "Wake Up",
                albumArtURL: "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg",
                audioURL: "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up_01_-_Intro_-_The_Way_Of_Waking_Up_-_Album_-_Wake_Up.mp3",
                duration: 84
            ),
            Track(
                id: "geisha",
                title: "Geisha",
                artist: "The Kyoto Connection",
                album: "Wake Up",
                albumArtURL: "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/art.jpg",
                audioURL: "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Geisha.mp3",
                duration: 267
            )
        ]
    }
    
    // MARK: - Search and Filter
    
    func searchTracks(_ query: String) -> [Track] {
        if query.isEmpty {
            return tracks
        }
        
        return tracks.filter { track in
            track.title.localizedCaseInsensitiveContains(query) ||
            track.artist.localizedCaseInsensitiveContains(query) ||
            track.album.localizedCaseInsensitiveContains(query)
        }
    }
    
    func tracksByArtist(_ artist: String) -> [Track] {
        return tracks.filter { $0.artist == artist }
    }
    
    func tracksByAlbum(_ album: String) -> [Track] {
        return tracks.filter { $0.album == album }
    }
    
    var uniqueArtists: [String] {
        return Array(Set(tracks.map { $0.artist })).sorted()
    }
    
    var uniqueAlbums: [String] {
        return Array(Set(tracks.map { $0.album })).sorted()
    }
}

// MARK: - Extensions

extension Track {
    var formattedDuration: String {
        let minutes = Int(duration) / 60
        let seconds = Int(duration) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

// MARK: - Sample Data for Previews

extension Track {
    static let sample = Track(
        id: "sample",
        title: "Sample Track",
        artist: "Sample Artist",
        album: "Sample Album",
        albumArtURL: "https://via.placeholder.com/300",
        audioURL: "https://example.com/sample.mp3",
        duration: 180
    )
    
    static let samplePlaylist = [
        Track(
            id: "track1",
            title: "First Track",
            artist: "Artist One",
            album: "Album One",
            albumArtURL: "https://via.placeholder.com/300",
            audioURL: "https://example.com/track1.mp3",
            duration: 210
        ),
        Track(
            id: "track2",
            title: "Second Track",
            artist: "Artist Two",
            album: "Album Two",
            albumArtURL: "https://via.placeholder.com/300",
            audioURL: "https://example.com/track2.mp3",
            duration: 195
        )
    ]
} 