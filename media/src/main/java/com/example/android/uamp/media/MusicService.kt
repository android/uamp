/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.media

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.library.JsonSource

/**
 * UAMP's implementation of a [MediaBrowserServiceCompat].
 *
 * This class is the entry point for browsing and playback commands from the APP's UI
 * and other apps that wish to play music via UAMP (for example, Android Auto or
 * the Google Assistant).
 *
 * Browsing begins with the method [MusicService.onGetRoot], and continues in
 * the callback [MusicService.onLoadChildren].
 *
 * For more information on implementing a MediaBrowserService,
 * visit [https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice.html](https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice.html).
 */
class MusicService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionCallback: MediaSessionCallback

    private lateinit var mediaSource: JsonSource

    private lateinit var playback: Playback

    private var announcedMetadata: MediaMetadataCompat? = null

    private val remoteJsonSource: Uri =
            Uri.parse("https://storage.googleapis.com/automotive-media/music.json")

    private val playbackStateBuilder = PlaybackStateCompat.Builder()

    // These actions are always supported.
    private val supportedActionsDefault =
            PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

    // These are the actions supported when the player is playing.
    private val supportedActionsPlaying =
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SEEK_TO

    // These are the actions supported when the player is paused.
    private val supportedActionsPaused =
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_SEEK_TO

    override fun onCreate() {
        super.onCreate()

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSessionCallback = MediaSessionCallback()
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        sessionToken = mediaSession.sessionToken

        mediaSource = JsonSource(context = this, source = remoteJsonSource)
        playback = Playback(this.applicationContext, { playerState ->
            updateState(playerState)
        })
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession.release()
    }

    override fun onGetRoot(clientPackageName: String,
                           clientUid: Int,
                           rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        return BrowserRoot("/", null)
    }

    override fun onLoadChildren(
            parentMediaId: String,
            result: MediaBrowserServiceCompat.Result<List<MediaItem>>) {

        val resultsSent = mediaSource.whenReady { successfullyInitialized ->
            if (successfullyInitialized) {
                val children = mediaSource.catalog.map { item ->
                    MediaItem(item.description, MediaItem.FLAG_PLAYABLE)
                }
                result.sendResult(children)
            } else {
                result.sendError(null)
            }
        }

        if (!resultsSent) {
            result.detach()
        }
    }

    private fun updateState(newState: Int?) {
        val updateState = newState ?: playback.playerState
        playbackStateBuilder.setState(updateState, playback.playerPosition, playback.playerSpeed)

        // Add actions based on state.
        val supportedActions = supportedActionsDefault or
                when (updateState) {
                    PlaybackStateCompat.STATE_PLAYING -> supportedActionsPlaying
                    PlaybackStateCompat.STATE_PAUSED -> supportedActionsPaused
                    else -> 0
                }
        playbackStateBuilder.setActions(supportedActions)

        mediaSession.setPlaybackState(playbackStateBuilder.build())

        // When the state changes, the metadata may have changed, so update that as well.
        updateMetadata(playback.currentlyPlaying)
    }

    private fun updateMetadata(nowPlaying: MediaMetadataCompat) {
        if (announcedMetadata == null || announcedMetadata != nowPlaying) {
            mediaSession.setMetadata(nowPlaying)
            announcedMetadata = nowPlaying
        }
    }

    private fun announceError(errorCode: Int, errorMessage: String) {
        playbackStateBuilder.setErrorMessage(errorCode, errorMessage)
        updateState(PlaybackStateCompat.STATE_ERROR)
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
        }

        override fun onPrepare() {
        }

        override fun onPlay() {
            if (playback.playerState == PlaybackStateCompat.STATE_PAUSED) {
                playback.resume()
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            mediaSource.whenReady {
                val itemToPlay = mediaSource.catalog.find { item ->
                    item.id == mediaId
                }
                if (itemToPlay == null) {
                    announceError(0, "Content not found: MediaID=${mediaId}")
                } else {
                    if (playback.currentlyPlaying != itemToPlay) {
                        playback.play(itemToPlay)
                    } else {
                        onPlay()
                    }
                }
            }
        }

        override fun onPause() {
            playback.pause()
        }

        override fun onStop() {
        }

        override fun onSkipToNext() {
        }

        override fun onSkipToPrevious() {
        }

        override fun onSeekTo(pos: Long) {
        }
    }
}