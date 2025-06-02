/*
 * Copyright 2019 Google Inc. All rights reserved.
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

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.android.uamp.media.library.JsonSource
import com.example.android.uamp.media.library.MusicSource
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * This class is the entry point for browsing and playback commands from the APP's UI
 * and other apps that wish to play music via UAMP (for example, Android Auto or
 * the Google Assistant).
 *
 * For more information on implementing a MediaSessionService,
 * visit [https://developer.android.com/guide/topics/media/media3].
 */
class MusicService : MediaSessionService() {

    private lateinit var notificationManager: UampNotificationManager
    private lateinit var mediaSource: MusicSource
    private lateinit var packageValidator: PackageValidator

    private lateinit var player: Player
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSession
    private var currentPlaylistItems: List<MediaItem> = emptyList()
    private var currentMediaItemIndex: Int = 0

    private lateinit var storage: PersistentStorage
    private var isForegroundService = false

    private val remoteJsonSource: Uri =
        Uri.parse("https://storage.googleapis.com/uamp/catalog.json")

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()

    /**
     * Configure ExoPlayer to handle audio focus for us.
     * See [Player.setAudioAttributes] for details.
     */
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

    /**
     * If Cast is available, create a CastPlayer to handle communication with a Cast session.
     */
    private val castPlayer: Player? by lazy {
        try {
            val castContext = CastContext.getSharedInstance(this)
            androidx.media3.cast.CastPlayer(castContext).apply {
                addListener(playerListener)
                setSessionAvailabilityListener(UampCastSessionAvailabilityListener())
            }
        } catch (e : Exception) {
            Log.i(TAG, "Cast is not available on this device. " +
                    "Exception thrown when attempting to obtain CastContext. " + e.message)
            null
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        // Create the player instance
        player = exoPlayer

        // Create a new MediaSession.
        mediaSession = MediaSession.Builder(this, player)
            .apply {
                sessionActivityPendingIntent?.let { setSessionActivity(it) }
            }
            .build()

        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        notificationManager = UampNotificationManager(
            this,
            mediaSession.token,
            PlayerNotificationListener()
        )

        // The media library is built from a remote JSON file. We'll create the source here,
        // and then use a suspend function to perform the download off the main thread.
        mediaSource = JsonSource(source = remoteJsonSource)
        serviceScope.launch {
            mediaSource.load()
        }

        // Show notification for the player
        notificationManager.showNotificationForPlayer(exoPlayer)

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
        storage = PersistentStorage.getInstance(applicationContext)
    }

    /**
     * This is the code that causes UAMP to stop playing when swiping the activity away from
     * recents. The choice to do this is app specific. Some apps stop playback, while others allow
     * playback to continue and allow users to stop it with the notification.
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        saveRecentSongToStorage()
        super.onTaskRemoved(rootIntent)

        /**
         * By stopping playback, the player will transition to [Player.STATE_IDLE] triggering
         * [Player.EventListener.onPlayerStateChanged] to be called. This will cause the
         * notification to be hidden and trigger
         * [PlayerNotificationManager.NotificationListener.onNotificationCancelled] to be called.
         * The service will then remove itself as a foreground service, and will call
         * [stopSelf].
         */
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        mediaSession.release()

        // Cancel coroutines when the service is going away.
        serviceJob.cancel()

        // Free ExoPlayer resources.
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    /**
     * Load the supplied list of songs and the song to play into the current player.
     */
    private fun preparePlaylist(
        metadataList: List<MediaItem>,
        itemToPlay: MediaItem?,
        playWhenReady: Boolean,
        playbackStartPositionMs: Long
    ) {
        // Since the playlist was probably based on some ordering (such as tracks in an album),
        // find which window index to play first so that the song the user actually wants to
        // hear plays first.
        val initialWindowIndex = if (itemToPlay == null) 0 else metadataList.indexOf(itemToPlay)
        currentPlaylistItems = metadataList

        player.playWhenReady = playWhenReady
        player.setMediaItems(metadataList, initialWindowIndex, playbackStartPositionMs)
        player.prepare()
    }

    private fun saveRecentSongToStorage() {
        // Obtain the current song details *before* saving them on a separate thread, otherwise
        // the current player may have been unloaded by the time the save routine runs.
        val mediaMetadata = currentPlaylistItems[currentMediaItemIndex].mediaMetadata
        val position = player.currentPosition

        serviceScope.launch {
            storage.saveRecentSong(
                mediaMetadata,
                position
            )
        }
    }

    private inner class UampCastSessionAvailabilityListener : androidx.media3.cast.SessionAvailabilityListener {
        override fun onCastSessionAvailable() {
            castPlayer?.let { player ->
                mediaSession.setPlayer(player)
            }
        }

        override fun onCastSessionUnavailable() {
            mediaSession.setPlayer(exoPlayer)
        }
    }

    /**
     * Listen for notification events.
     */
    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, this@MusicService.javaClass)
                )

                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    /**
     * Listen for events from ExoPlayer.
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotificationForPlayer(exoPlayer)
                    if (playbackState == Player.STATE_READY) {
                        // When playing/paused save the current media item in persistent storage so that
                        // playback can be resumed between device reboots.
                        saveRecentSongToStorage()
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)
                || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                currentMediaItemIndex = if (currentPlaylistItems.isNotEmpty()) {
                    player.currentMediaItemIndex.coerceIn(0, currentPlaylistItems.size - 1)
                } else {
                    0
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            var message = R.string.generic_error
            Log.e(TAG, "Player error: " + error.errorCodeName + " (" + error.errorCode + ")")
            when (error.errorCode) {
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                    message = R.string.error_media_not_found
                }
            }
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return if (packageValidator.isKnownCaller(controllerInfo.packageName, controllerInfo.uid)) {
            mediaSession
        } else {
            null
        }
    }
}

/*
 * (Media) Session events
 */
const val NETWORK_FAILURE = "com.example.android.uamp.media.session.NETWORK_FAILURE"

/** Content styling constants */
private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
private const val CONTENT_STYLE_LIST = 1
private const val CONTENT_STYLE_GRID = 2

private const val UAMP_USER_AGENT = "uamp.next"

val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"

private const val TAG = "MusicService"
