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

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.android.uamp.media.extensions.flag
import com.example.android.uamp.media.library.BrowseTree
import com.example.android.uamp.media.library.JsonSource
import com.example.android.uamp.media.library.MEDIA_SEARCH_SUPPORTED
import com.example.android.uamp.media.library.MusicSource
import com.example.android.uamp.media.library.UAMP_BROWSABLE_ROOT
import com.example.android.uamp.media.library.UAMP_EMPTY_ROOT
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
open class MusicService : MediaBrowserServiceCompat() {

    private lateinit var notificationManager: UampNotificationManager
    private lateinit var mediaSource: MusicSource
    private lateinit var packageValidator: PackageValidator

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector

    /**
     * This must be `by lazy` because the source won't initially be ready.
     * See [MusicService.onLoadChildren] to see where it's accessed (and first
     * constructed).
     */
    private val browseTree: BrowseTree by lazy {
        BrowseTree(applicationContext, mediaSource)
    }

    private var isForegroundService = false

    private val remoteJsonSource: Uri =
        Uri.parse("https://storage.googleapis.com/uamp/catalog.json")

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()

    /**
     * Configure ExoPlayer to handle audio focus for us.
     * See [Player.AudioComponent.setAudioAttributes] for details.
     */
    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build().apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "MusicService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        /**
         * In order for [MediaBrowserCompat.ConnectionCallback.onConnected] to be called,
         * a [MediaSessionCompat.Token] needs to be set on the [MediaBrowserServiceCompat].
         *
         * It is possible to wait to set the session token, if required for a specific use-case.
         * However, the token *must* be set by the time [MediaBrowserServiceCompat.onGetRoot]
         * returns, or the connection will fail silently. (The system will not even call
         * [MediaBrowserCompat.ConnectionCallback.onConnectionFailed].)
         */
        sessionToken = mediaSession.sessionToken

        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        notificationManager = UampNotificationManager(
            this,
            exoPlayer,
            mediaSession.sessionToken,
            PlayerNotificationListener()
        )

        // The media library is built from a remote JSON file. We'll create the source here,
        // and then use a suspend function to perform the download off the main thread.
        mediaSource = JsonSource(context = this, source = remoteJsonSource)
        serviceScope.launch {
            mediaSource.load()
        }

        // ExoPlayer will manage the MediaSession for us.
        mediaSessionConnector = MediaSessionConnector(mediaSession).also { connector ->
            // Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(
                this, Util.getUserAgent(this, UAMP_USER_AGENT), null
            )

            // Create the PlaybackPreparer of the media session connector.
            val playbackPreparer = UampPlaybackPreparer(
                mediaSource,
                exoPlayer,
                dataSourceFactory
            )

            connector.setPlayer(exoPlayer)
            connector.setPlaybackPreparer(playbackPreparer)
            connector.setQueueNavigator(UampQueueNavigator(mediaSession))
        }

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }

    /**
     * This is the code that causes UAMP to stop playing when swiping the activity away from
     * recents. The choice to do this is app specific. Some apps stop playback, while others allow
     * playback to continue and allow users to stop it with the notification.
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        /**
         * By stopping playback, the player will transition to [Player.STATE_IDLE] triggering
         * [Player.EventListener.onPlayerStateChanged] to be called. This will cause the
         * notification to be hidden and trigger
         * [PlayerNotificationManager.NotificationListener.onNotificationCancelled] to be called.
         * The service will then remove itself as a foreground service, and will call
         * [stopSelf].
         */
        exoPlayer.stop(true)
    }

    override fun onDestroy() {
        mediaSession.run {
            isActive = false
            release()
        }

        // Cancel coroutines when the service is going away.
        serviceJob.cancel()

        // Free ExoPlayer resources.
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    /**
     * Returns the "root" media ID that the client should request to get the list of
     * [MediaItem]s to browse/play.
     */
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {

        /*
         * By default, all known clients are permitted to search, but only tell unknown callers
         * about search if permitted by the [BrowseTree].
         */
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        val rootExtras = Bundle().apply {
            putBoolean(
                MEDIA_SEARCH_SUPPORTED,
                isKnownCaller || browseTree.searchableByUnknownCaller
            )
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }

        return if (isKnownCaller) {
            // The caller is allowed to browse, so return the root.
            BrowserRoot(UAMP_BROWSABLE_ROOT, rootExtras)
        } else {
            /**
             * Unknown caller. There are two main ways to handle this:
             * 1) Return a root without any content, which still allows the connecting client
             * to issue commands.
             * 2) Return `null`, which will cause the system to disconnect the app.
             *
             * UAMP takes the first approach for a variety of reasons, but both are valid
             * options.
             */
            BrowserRoot(UAMP_EMPTY_ROOT, rootExtras)
        }
    }

    /**
     * Returns (via the [result] parameter) a list of [MediaItem]s that are child
     * items of the provided [parentMediaId]. See [BrowseTree] for more details on
     * how this is build/more details about the relationships.
     */
    override fun onLoadChildren(
        parentMediaId: String,
        result: Result<List<MediaItem>>
    ) {

        // If the media source is ready, the results will be set synchronously here.
        val resultsSent = mediaSource.whenReady { successfullyInitialized ->
            if (successfullyInitialized) {
                val children = browseTree[parentMediaId]?.map { item ->
                    MediaItem(item.description, item.flag)
                }
                result.sendResult(children)
            } else {
                mediaSession.sendSessionEvent(NETWORK_FAILURE, null)
                result.sendResult(null)
            }
        }

        // If the results are not ready, the service must "detach" the results before
        // the method returns. After the source is ready, the lambda above will run,
        // and the caller will be notified that the results are ready.
        //
        // See [MediaItemFragmentViewModel.subscriptionCallback] for how this is passed to the
        // UI/displayed in the [RecyclerView].
        if (!resultsSent) {
            result.detach()
        }
    }

    /**
     * Returns a list of [MediaItem]s that match the given search query
     */
    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<List<MediaItem>>
    ) {

        val resultsSent = mediaSource.whenReady { successfullyInitialized ->
            if (successfullyInitialized) {
                val resultsList = mediaSource.search(query, extras ?: Bundle.EMPTY)
                    .map { mediaMetadata ->
                        MediaItem(mediaMetadata.description, mediaMetadata.flag)
                    }
                result.sendResult(resultsList)
            }
        }

        if (!resultsSent) {
            result.detach()
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
    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotification()

                    // If playback is paused we remove the foreground state which allows the
                    // notification to be dismissed. An alternative would be to provide a "close"
                    // button in the notification which stops playback and clears the notification.
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) stopForeground(false)
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }
    }
}

/**
 * Helper class to retrieve the the Metadata necessary for the ExoPlayer MediaSession connection
 * extension to call [MediaSessionCompat.setMetadata].
 */
private class UampQueueNavigator(
    mediaSession: MediaSessionCompat
) : TimelineQueueNavigator(mediaSession) {
    private val window = Timeline.Window()
    override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
        player.currentTimeline
            .getWindow(windowIndex, window).tag as MediaDescriptionCompat
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
private const val TAG = "MusicService"
