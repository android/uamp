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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android Auto specific MediaBrowserService that provides browsing capabilities
 * while sharing the player instance with MusicService
 */
class AndroidAutoService : MediaBrowserServiceCompat() {

    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var packageValidator: PackageValidator
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AndroidAutoService onCreate")

        // Create MediaSessionCompat for Android Auto
        mediaSessionCompat = MediaSessionCompat(this, TAG).apply {
            setCallback(AndroidAutoSessionCallback())
            isActive = true
        }

        // Set the session token for MediaBrowserServiceCompat
        sessionToken = mediaSessionCompat.sessionToken

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AndroidAutoService onDestroy")
        mediaSessionCompat.release()
        serviceJob.cancel()
    }

    /**
     * This is required for Android Auto to connect and display the UI
     */
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        Log.d(TAG, "onGetRoot: clientPackageName=$clientPackageName, clientUid=$clientUid")

        // Check if the caller is allowed to browse the media library
        if (packageValidator.isKnownCaller(clientPackageName, clientUid)) {
            // Return the root with browseable content
            val extras = Bundle().apply {
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
            }
            return BrowserRoot(MEDIA_ROOT_ID, extras)
        }

        Log.w(TAG, "Unknown caller $clientPackageName (uid=$clientUid). Rejecting connection.")
        return null
    }

    /**
     * This is called by Android Auto to get the list of media items to display
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren: parentId=$parentId")

        when (parentId) {
            MEDIA_ROOT_ID -> {
                // Get catalog from the main music service
                val musicService = MusicService.getInstance()
                if (musicService != null) {
                    val catalogList = musicService.mediaSource.toList()
                    if (catalogList.isEmpty()) {
                        // If catalog isn't loaded yet, return pending result
                        result.detach()
                        serviceScope.launch {
                            musicService.mediaSource.whenReady { success ->
                                if (success) {
                                    val mediaItems = buildMediaItemList(musicService)
                                    result.sendResult(mediaItems)
                                } else {
                                    result.sendResult(mutableListOf())
                                }
                            }
                        }
                    } else {
                        // Catalog is ready, return the items
                        val mediaItems = buildMediaItemList(musicService)
                        result.sendResult(mediaItems)
                    }
                } else {
                    Log.w(TAG, "MusicService not available")
                    result.sendResult(mutableListOf())
                }
            }
            else -> {
                // Unknown parent ID
                Log.w(TAG, "Unknown parentId: $parentId")
                result.sendResult(mutableListOf())
            }
        }
    }

    /**
     * Build list of MediaBrowserCompat.MediaItem from the music service catalog
     */
    private fun buildMediaItemList(musicService: MusicService): MutableList<MediaBrowserCompat.MediaItem> {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        try {
            for (metadata in musicService.mediaSource) {
                val mediaId = metadata.extras?.getString("media_id") ?: continue

                val description = MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(metadata.title)
                    .setSubtitle(metadata.artist)
                    .setDescription(metadata.albumTitle)
                    .setIconUri(metadata.artworkUri)
                    .build()

                val mediaItem = MediaBrowserCompat.MediaItem(
                    description,
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )

                mediaItems.add(mediaItem)
            }
            Log.d(TAG, "Built ${mediaItems.size} media items for browsing")
        } catch (e: Exception) {
            Log.e(TAG, "Error building media item list: ${e.message}")
        }

        return mediaItems
    }

    /**
     * Custom MediaSessionCompat.Callback that forwards commands to the main MusicService
     */
    private inner class AndroidAutoSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Log.d(TAG, "onPlay")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.play()
            }
        }

        override fun onPause() {
            Log.d(TAG, "onPause")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.pause()
            }
        }

        override fun onSkipToNext() {
            Log.d(TAG, "onSkipToNext")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.seekToNext()
            }
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.seekToPrevious()
            }
        }

        override fun onSeekTo(pos: Long) {
            Log.d(TAG, "onSeekTo: $pos")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.seekTo(pos)
            }
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            Log.d(TAG, "onSetShuffleMode: $shuffleMode")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.shuffleModeEnabled = shuffleMode != 0
            }
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            Log.d(TAG, "onSetRepeatMode: $repeatMode")
            MusicService.getInstance()?.let { service ->
                service.mediaSession.player.repeatMode = repeatMode
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Log.d(TAG, "onPlayFromMediaId: $mediaId")
            mediaId?.let { id ->
                MusicService.getInstance()?.let { service ->
                    // Find the track in the catalog and play it
                    serviceScope.launch {
                        service.mediaSource.whenReady { success ->
                            if (success) {
                                val trackMetadata = service.mediaSource.find { metadata ->
                                    metadata.extras?.getString("media_id") == id
                                }

                                if (trackMetadata != null) {
                                    Log.d(TAG, "Found track: ${trackMetadata.title}")

                                    // Set the playlist with all tracks and play the selected one
                                    val allTracks = service.mediaSource.map { metadata ->
                                        val mediaItemId = metadata.extras?.getString("media_id") ?: ""
                                        val uri = metadata.extras?.getString("media_uri") ?: ""

                                        androidx.media3.common.MediaItem.Builder()
                                            .setMediaId(mediaItemId)
                                            .setUri(uri)
                                            .setMediaMetadata(metadata)
                                            .build()
                                    }

                                    // Find the index of the selected track
                                    val startIndex = allTracks.indexOfFirst { it.mediaId == id }
                                    Log.d(TAG, "Playing track at index $startIndex of ${allTracks.size} tracks")

                                    service.mediaSession.player.setMediaItems(allTracks, startIndex, 0)
                                    service.mediaSession.player.prepare()
                                    service.mediaSession.player.play()
                                } else {
                                    Log.w(TAG, "Track not found: $id")
                                }
                            } else {
                                Log.w(TAG, "Catalog not ready for playback")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Content styling constants */
private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
private const val CONTENT_STYLE_LIST = 1
private const val CONTENT_STYLE_GRID = 2

/** MediaBrowser root ID */
private const val MEDIA_ROOT_ID = "__ROOT__"

private const val TAG = "AndroidAutoService" 