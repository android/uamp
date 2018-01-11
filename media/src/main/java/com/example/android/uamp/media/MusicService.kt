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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.library.JsonSource

/**
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
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationBuilder
    private var isForegroundService = false
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
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO

    // These are the actions supported when the player is paused.
    private val supportedActionsPaused =
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO

    // These are the actions supported when the player is stopped.
    private val supportedActionsStopped =
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY

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

        notificationBuilder = NotificationBuilder(this)
        notificationManager = NotificationManagerCompat.from(this)

        becomingNoisyReceiver =
                BecomingNoisyReceiver(context = this, sessionToken = mediaSession.sessionToken)

        mediaSource = JsonSource(context = this, source = remoteJsonSource)
        playback = buildPlayback()
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

    private fun buildPlayback(): Playback {
        return Playback(applicationContext) { playerState ->
            updateState(playerState)
        }
    }

    private fun updateState(newState: Int?) {
        val updateState = newState ?: playback.playerState
        playbackStateBuilder.setState(updateState, playback.playerPosition, playback.playerSpeed)

        // Add actions based on state.
        val supportedActions = supportedActionsDefault or
                when (updateState) {
                    PlaybackStateCompat.STATE_BUFFERING,
                    PlaybackStateCompat.STATE_PLAYING -> supportedActionsPlaying
                    PlaybackStateCompat.STATE_PAUSED -> supportedActionsPaused
                    PlaybackStateCompat.STATE_STOPPED -> supportedActionsStopped
                    else -> 0
                }
        playbackStateBuilder.setActions(supportedActions)

        mediaSession.setPlaybackState(playbackStateBuilder.build())

        // Stop requires a bit of special handling, since the player was released.
        if (updateState == PlaybackStateCompat.STATE_STOPPED) {
            playback = buildPlayback()
        }

        // When the state changes, the metadata may have changed, so update that as well.
        updateMetadata(playback.currentlyPlaying)

        val notification = notificationBuilder.buildNotification(sessionToken!!)
        when (updateState) {
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING -> {
                becomingNoisyReceiver.register()

                startForeground(NOW_PLAYING_NOTIFICATION, notification)
                isForegroundService = true
            }
            else -> {
                becomingNoisyReceiver.unregister()

                if (isForegroundService) {
                    stopForeground(false)

                    notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                    isForegroundService = false
                }
            }
        }
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
        override fun onAddQueueItem(description: MediaDescriptionCompat?) = Unit

        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) = Unit

        override fun onPrepare() = Unit

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
                    announceError(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                            getString(R.string.error_media_not_found))
                    Log.w(TAG, "Content not found: MediaID=$mediaId")
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
            playback.stop()
        }

        override fun onSkipToNext() = Unit

        override fun onSkipToPrevious() = Unit

        override fun onSeekTo(pos: Long) = Unit
    }
}

/**
 * Helper class for listening for when headphones are unplugged (or the audio
 * will otherwise cause playback to become "noisy").
 */
private class BecomingNoisyReceiver(private val context: Context,
                                    sessionToken: MediaSessionCompat.Token)
    : BroadcastReceiver() {

    private val noisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val controller = MediaControllerCompat(context, sessionToken)

    private var registered = false

    fun register() {
        if (!registered) {
            context.registerReceiver(this, noisyIntentFilter)
            registered = true
        }
    }

    fun unregister() {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }
    }
}

// For logcat.
private const val TAG = "MusicService"