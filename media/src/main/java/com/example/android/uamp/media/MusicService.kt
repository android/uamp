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

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
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
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.library.JsonSource
import com.example.android.uamp.media.library.MusicSource

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
    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationBuilder
    private var isForegroundService = false
    private lateinit var mediaSource: MusicSource
    private lateinit var playback: Playback
    private var announcedMetadata: MediaMetadataCompat? = null
    private val remoteJsonSource: Uri =
            Uri.parse("https://storage.googleapis.com/uamp/catalog.json")

    private val mediaSessionCallback = MediaSessionCallback()
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

        // Build a PendingIntent that can be used to launch the UI.
        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setSessionActivity(sessionActivityPendingIntent)
        }
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
        playback.release()
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
                val children = mediaSource.map { item ->
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
        val updatedState = newState ?: playback.playerState
        playbackStateBuilder.setState(updatedState, playback.playerPosition, playback.playerSpeed)
                .setActiveQueueItemId(playback.queueIndex)

        // Check if it's possible to skip to previous/next
        val skipToPrevious = if (playback.canSkipToPrevious) {
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        } else {
            UNSUPPORTED_ACTION
        }
        val skipToNext = if (playback.canSkipToNext) {
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        } else {
            UNSUPPORTED_ACTION
        }

        // Add actions based on state.
        val supportedActions = supportedActionsDefault or skipToPrevious or skipToNext or
                when (updatedState) {
                    PlaybackStateCompat.STATE_BUFFERING,
                    PlaybackStateCompat.STATE_PLAYING -> supportedActionsPlaying
                    PlaybackStateCompat.STATE_PAUSED -> supportedActionsPaused
                    PlaybackStateCompat.STATE_STOPPED -> supportedActionsStopped
                    else -> NO_ADDITIONAL_SUPPORTED_ACTIONS
                }

        playbackStateBuilder.setActions(supportedActions)

        mediaSession.setPlaybackState(playbackStateBuilder.build())

        // When the state changes, the metadata may have changed, so update that as well.
        updateMetadata(playback.currentlyPlaying)

        // Skip building a notification when state is "none".
        val notification = if (updatedState != PlaybackStateCompat.STATE_NONE) {
            notificationBuilder.buildNotification(sessionToken!!)
        } else {
            null
        }

        when (updatedState) {
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

                    if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                    } else {
                        removeNowPlayingNotification()
                    }
                    isForegroundService = false
                }
            }
        }
    }

    private fun updateMetadata(nowPlaying: MediaMetadataCompat) {
        if (announcedMetadata == null || announcedMetadata != nowPlaying) {
            mediaSession.setQueue(playback.queue)
            mediaSession.setMetadata(nowPlaying)
            announcedMetadata = nowPlaying
        }
    }

    private fun announceError(errorCode: Int, errorMessage: String) {
        playbackStateBuilder.setErrorMessage(errorCode, errorMessage)
        updateState(PlaybackStateCompat.STATE_ERROR)
    }

    /**
     * Removes the [NOW_PLAYING_NOTIFICATION] notification.
     *
     * Since `stopForeground(false)` was already called (see [MusicService.updateState], it's
     * possible to cancel the notification with
     * `notificationManager.cancel(NOW_PLAYING_NOTIFICATION)` if minSdkVersion is >=
     * [Build.VERSION_CODES.LOLLIPOP].
     *
     * Prior to [Build.VERSION_CODES.LOLLIPOP], notifications associated with a foreground
     * service remained marked as "ongoing" even after calling [Service.stopForeground],
     * and cannot be cancelled normally.
     *
     * Fortunately, it's possible to simply call [Service.stopForeground] a second time, this
     * time with `true`. This won't change anything about the service's state, but will simply
     * remove the notification.
     */
    private fun removeNowPlayingNotification() {
        stopForeground(true)
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
                val itemToPlay = mediaSource.find { item ->
                    item.id == mediaId
                }
                if (itemToPlay == null) {
                    announceError(PlaybackStateCompat.ERROR_CODE_APP_ERROR,
                            getString(R.string.error_media_not_found))
                    Log.w(TAG, "Content not found: MediaID=$mediaId")
                } else {
                    // TODO: Use extras to not always queue the entire album.
                    val playlist = mediaSource.filter { item ->
                        item.album == itemToPlay.album
                    }

                    if (!playback.isPrepared(itemToPlay)) {
                        playback.prepare(playlist)
                    }
                    playback.play(itemToPlay)
                }
            }
        }

        override fun onPause() {
            playback.pause()
        }

        override fun onStop() {
            removeNowPlayingNotification()
            playback.stop()
        }

        override fun onSkipToNext() =
                if (playback.canSkipToNext) playback.skipToNext() else Unit

        override fun onSkipToPrevious() =
                if (playback.canSkipToPrevious) playback.skipToPrevious() else Unit

        override fun onSeekTo(pos: Long) = playback.seekTo(pos)
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

private const val TAG = "MusicService"

/**
 * These provide context to the meaning of "0" in the various bit fields set in a
 * [PlaybackStateCompat]. See [MusicService.updateState] for details.
 */
private const val UNSUPPORTED_ACTION = 0L
private const val NO_ADDITIONAL_SUPPORTED_ACTIONS = 0L