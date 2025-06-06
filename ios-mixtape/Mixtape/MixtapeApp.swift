//
//  MixtapeApp.swift
//  Mixtape
//
//  Created by Assistant on iOS Port
//

import SwiftUI
import AVFoundation
import MediaPlayer

@main
struct MixtapeApp: App {
    @StateObject private var audioManager = AudioManager.shared
    
    init() {
        setupAudioSession()
        setupRemoteCommands()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(audioManager)
                .onAppear {
                    // Load music catalog when app appears
                    Task {
                        await audioManager.loadCatalog()
                    }
                }
        }
    }
    
    private func setupAudioSession() {
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .default, options: [.allowAirPlay, .allowBluetooth, .allowBluetoothA2DP])
            try audioSession.setActive(true)
        } catch {
            print("Failed to set up audio session: \(error)")
        }
    }
    
    private func setupRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()
        
        // Play command
        commandCenter.playCommand.addTarget { _ in
            AudioManager.shared.play()
            return .success
        }
        
        // Pause command
        commandCenter.pauseCommand.addTarget { _ in
            AudioManager.shared.pause()
            return .success
        }
        
        // Next track command
        commandCenter.nextTrackCommand.addTarget { _ in
            AudioManager.shared.skipNext()
            return .success
        }
        
        // Previous track command
        commandCenter.previousTrackCommand.addTarget { _ in
            AudioManager.shared.skipPrevious()
            return .success
        }
        
        // Change playback position command
        commandCenter.changePlaybackPositionCommand.addTarget { event in
            if let positionEvent = event as? MPChangePlaybackPositionCommandEvent {
                AudioManager.shared.seek(to: positionEvent.positionTime)
                return .success
            }
            return .commandFailed
        }
        
        // Toggle shuffle command
        commandCenter.changeShuffleModeCommand.addTarget { event in
            if let shuffleEvent = event as? MPChangeShuffleModeCommandEvent {
                AudioManager.shared.setShuffleMode(shuffleEvent.shuffleType == .items)
                return .success
            }
            return .commandFailed
        }
        
        // Toggle repeat command
        commandCenter.changeRepeatModeCommand.addTarget { event in
            if let repeatEvent = event as? MPChangeRepeatModeCommandEvent {
                switch repeatEvent.repeatType {
                case .off:
                    AudioManager.shared.setRepeatMode(.none)
                case .one:
                    AudioManager.shared.setRepeatMode(.one)
                case .all:
                    AudioManager.shared.setRepeatMode(.all)
                @unknown default:
                    AudioManager.shared.setRepeatMode(.none)
                }
                return .success
            }
            return .commandFailed
        }
        
        // Enable the commands
        commandCenter.playCommand.isEnabled = true
        commandCenter.pauseCommand.isEnabled = true
        commandCenter.nextTrackCommand.isEnabled = true
        commandCenter.previousTrackCommand.isEnabled = true
        commandCenter.changePlaybackPositionCommand.isEnabled = true
        commandCenter.changeShuffleModeCommand.isEnabled = true
        commandCenter.changeRepeatModeCommand.isEnabled = true
    }
} 