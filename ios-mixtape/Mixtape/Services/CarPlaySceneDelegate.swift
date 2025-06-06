//
//  CarPlaySceneDelegate.swift
//  Mixtape
//
//  CarPlay integration for in-car music experience
//

import CarPlay
import UIKit
import MediaPlayer

class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    
    var interfaceController: CPInterfaceController?
    
    // MARK: - Scene Lifecycle
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                didConnect interfaceController: CPInterfaceController) {
        self.interfaceController = interfaceController
        setupCarPlayInterface()
    }
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene,
                                didDisconnectInterfaceController interfaceController: CPInterfaceController) {
        self.interfaceController = nil
    }
    
    // MARK: - Interface Setup
    
    private func setupCarPlayInterface() {
        guard let interfaceController = interfaceController else { return }
        
        // Create the main tab bar template
        let tabBarTemplate = createTabBarTemplate()
        
        // Set as root template
        interfaceController.setRootTemplate(tabBarTemplate, animated: false, completion: nil)
        
        // Setup Now Playing template for audio controls
        setupNowPlayingTemplate()
    }
    
    private func createTabBarTemplate() -> CPTabBarTemplate {
        // Library tab
        let libraryTab = CPListTemplate(title: "Library", sections: [createLibrarySection()])
        libraryTab.tabTitle = "Library"
        libraryTab.tabImage = UIImage(systemName: "music.note.list")
        libraryTab.delegate = self
        
        // Playlists tab  
        let playlistsTab = CPListTemplate(title: "Browse", sections: [createArtistsSection(), createAlbumsSection()])
        playlistsTab.tabTitle = "Browse"
        playlistsTab.tabImage = UIImage(systemName: "music.note")
        playlistsTab.delegate = self
        
        return CPTabBarTemplate(templates: [libraryTab, playlistsTab])
    }
    
    private func createLibrarySection() -> CPListSection {
        let tracks = MusicCatalog.shared.tracks
        
        let items = tracks.map { track in
            let item = CPListItem(text: track.title, detailText: track.artist)
            item.userInfo = track
            item.accessoryType = .disclosureIndicator
            return item
        }
        
        return CPListSection(items: items)
    }
    
    private func createArtistsSection() -> CPListSection {
        let artists = MusicCatalog.shared.uniqueArtists
        
        let items = artists.map { artist in
            let artistTracks = MusicCatalog.shared.tracksByArtist(artist)
            let item = CPListItem(text: artist, detailText: "\(artistTracks.count) songs")
            item.userInfo = ["type": "artist", "name": artist]
            item.accessoryType = .disclosureIndicator
            return item
        }
        
        return CPListSection(items: items, header: "Artists", sectionIndexTitle: nil)
    }
    
    private func createAlbumsSection() -> CPListSection {
        let albums = MusicCatalog.shared.uniqueAlbums
        
        let items = albums.map { album in
            let albumTracks = MusicCatalog.shared.tracksByAlbum(album)
            let item = CPListItem(text: album, detailText: "\(albumTracks.count) songs")
            item.userInfo = ["type": "album", "name": album]
            item.accessoryType = .disclosureIndicator
            return item
        }
        
        return CPListSection(items: items, header: "Albums", sectionIndexTitle: nil)
    }
    
    private func setupNowPlayingTemplate() {
        // CarPlay automatically handles Now Playing when we use MPNowPlayingInfoCenter
        // which is already set up in AudioManager
    }
}

// MARK: - CPListTemplateDelegate

extension CarPlaySceneDelegate: CPListTemplateDelegate {
    
    func listTemplate(_ listTemplate: CPListTemplate, didSelect item: CPListItem, completionHandler: @escaping () -> Void) {
        defer { completionHandler() }
        
        if let track = item.userInfo as? Track {
            // Play selected track
            AudioManager.shared.playTrack(track, in: MusicCatalog.shared.tracks)
        } else if let info = item.userInfo as? [String: String],
                  let type = info["type"],
                  let name = info["name"] {
            
            // Handle artist or album selection
            switch type {
            case "artist":
                showArtistTracks(artist: name, in: listTemplate)
            case "album":
                showAlbumTracks(album: name, in: listTemplate)
            default:
                break
            }
        }
    }
    
    private func showArtistTracks(artist: String, in listTemplate: CPListTemplate) {
        let tracks = MusicCatalog.shared.tracksByArtist(artist)
        let items = tracks.map { track in
            let item = CPListItem(text: track.title, detailText: track.album)
            item.userInfo = track
            item.accessoryType = .disclosureIndicator
            return item
        }
        
        let section = CPListSection(items: items)
        let artistTemplate = CPListTemplate(title: artist, sections: [section])
        artistTemplate.delegate = self
        
        interfaceController?.pushTemplate(artistTemplate, animated: true, completion: nil)
    }
    
    private func showAlbumTracks(album: String, in listTemplate: CPListTemplate) {
        let tracks = MusicCatalog.shared.tracksByAlbum(album)
        let items = tracks.map { track in
            let item = CPListItem(text: track.title, detailText: track.artist)
            item.userInfo = track
            item.accessoryType = .disclosureIndicator
            return item
        }
        
        let section = CPListSection(items: items)
        let albumTemplate = CPListTemplate(title: album, sections: [section])
        albumTemplate.delegate = self
        
        interfaceController?.pushTemplate(albumTemplate, animated: true, completion: nil)
    }
}

// MARK: - Now Playing Integration

extension CarPlaySceneDelegate {
    
    func updateNowPlayingTemplate() {
        // CarPlay automatically displays Now Playing controls when audio is playing
        // and MPNowPlayingInfoCenter is properly configured (done in AudioManager)
        
        // We can add custom actions here if needed
        setupNowPlayingButtons()
    }
    
    private func setupNowPlayingButtons() {
        let commandCenter = MPRemoteCommandCenter.shared()
        
        // Enable additional commands for CarPlay
        commandCenter.changeShuffleModeCommand.isEnabled = true
        commandCenter.changeRepeatModeCommand.isEnabled = true
        commandCenter.ratingCommand.isEnabled = false
        commandCenter.likeCommand.isEnabled = false
        commandCenter.dislikeCommand.isEnabled = false
        commandCenter.bookmarkCommand.isEnabled = false
        
        // Set supported shuffle modes
        commandCenter.changeShuffleModeCommand.supportedShuffleTypes = [.off, .items]
        
        // Set supported repeat modes
        commandCenter.changeRepeatModeCommand.supportedRepeatTypes = [.off, .one, .all]
    }
}

// MARK: - CarPlay Template Updates

extension CarPlaySceneDelegate {
    
    func refreshCarPlayContent() {
        // Refresh the content when the music catalog is updated
        DispatchQueue.main.async {
            self.setupCarPlayInterface()
        }
    }
}

// MARK: - Notification Handling

extension CarPlaySceneDelegate {
    
    func setupNotifications() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(catalogDidUpdate),
            name: NSNotification.Name("MusicCatalogDidUpdate"),
            object: nil
        )
    }
    
    @objc private func catalogDidUpdate() {
        refreshCarPlayContent()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
} 