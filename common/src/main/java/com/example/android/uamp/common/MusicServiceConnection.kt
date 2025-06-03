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

package com.example.android.uamp.common

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.media3.session.SessionResult
import com.example.android.uamp.media.NETWORK_FAILURE
import com.example.android.uamp.media.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Class that manages a connection to a MediaBrowserService.
 *
 * Typically it's best to construct/inject dependencies either using DI or, as UAMP does,
 * using [InjectorUtils] to handle the construction.
 */
class MusicServiceConnection(
    context: Context,
    serviceComponent: ComponentName
) {
    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val networkFailure = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val playbackState = MutableLiveData<Int>()
        .apply { postValue(Player.STATE_IDLE) }

    val nowPlaying = MutableLiveData<MediaMetadata>()

    val rootMediaId = MutableLiveData<String>()

    val isPlaying = MutableLiveData<Boolean>()
        .apply { postValue(false) }

    val currentPosition = MutableLiveData<Long>()
        .apply { postValue(0L) }

    val duration = MutableLiveData<Long>()
        .apply { postValue(0L) }

    private val mediaBrowserFuture: ListenableFuture<MediaBrowser> =
        MediaBrowser.Builder(
            context,
            SessionToken(context, serviceComponent)
        )
        .buildAsync()

    val mediaBrowser: MediaBrowser get() = mediaBrowserFuture.get()

    private val mediaBrowserCallback = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            playbackState.postValue(state)
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying.postValue(playing)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaMetadata?.let { metadata ->
                nowPlaying.postValue(metadata)
            }
        }
    }

    private val subscriptions = mutableMapOf<String, (List<MediaItem>) -> Unit>()

    init {
        mediaBrowserFuture.addListener({
            mediaBrowser.addListener(mediaBrowserCallback)
            isConnected.postValue(true)
            rootMediaId.postValue("/")
        }, MoreExecutors.directExecutor())
    }

    /**
     * Play a specific media item by ID
     */
    fun playMedia(mediaId: String) {
        Log.d(TAG, "Play media called for: $mediaId")
        if (isConnected.value == true) {
            val service = MusicService.getInstance()
            if (service != null) {
                service.mediaSource.whenReady { success ->
                    if (success) {
                        // Find the track in the catalog
                        val trackMetadata = service.mediaSource.find { metadata ->
                            metadata.extras?.getString("media_id") == mediaId
                        }
                        
                        if (trackMetadata != null) {
                            Log.d(TAG, "Found track: ${trackMetadata.title}")
                            val mediaUri = trackMetadata.extras?.getString("media_uri") ?: ""
                            
                            // Set the playlist with all tracks and play the selected one
                            val allTracks = service.mediaSource.map { metadata ->
                                val id = metadata.extras?.getString("media_id") ?: ""
                                val uri = metadata.extras?.getString("media_uri") ?: ""
                                
                                MediaItem.Builder()
                                    .setMediaId(id)
                                    .setUri(uri)
                                    .setMediaMetadata(metadata)
                                    .build()
                            }
                            
                            // Find the index of the selected track
                            val startIndex = allTracks.indexOfFirst { it.mediaId == mediaId }
                            Log.d(TAG, "Playing track at index $startIndex of ${allTracks.size} tracks")
                            
                            mediaBrowser.setMediaItems(allTracks, startIndex, 0)
                            mediaBrowser.prepare()
                            mediaBrowser.play()
                        } else {
                            Log.w(TAG, "Track not found: $mediaId")
                        }
                    } else {
                        Log.w(TAG, "Catalog not ready for playback")
                    }
                }
            }
        }
    }

    /**
     * Play the current media item
     */
    fun play() {
        if (isConnected.value == true) {
            mediaBrowser.play()
        }
    }

    /**
     * Pause the current media item
     */
    fun pause() {
        if (isConnected.value == true) {
            mediaBrowser.pause()
        }
    }

    /**
     * Skip to the next track
     */
    fun skipNext() {
        if (isConnected.value == true) {
            mediaBrowser.seekToNext()
        }
    }

    /**
     * Skip to the previous track
     */
    fun skipPrevious() {
        if (isConnected.value == true) {
            mediaBrowser.seekToPrevious()
        }
    }

    /**
     * Seek to a specific position
     */
    fun seekTo(position: Long) {
        if (isConnected.value == true) {
            mediaBrowser.seekTo(position)
        }
    }

    /**
     * Subscribe to a parent media ID to get its children
     */
    fun subscribe(parentId: String, callback: (List<MediaItem>) -> Unit) {
        Log.d(TAG, "Subscribe called for parentId: $parentId")
        subscriptions[parentId] = callback
        
        if (isConnected.value == true && parentId == "/") {
            // Get the catalog directly from the service
            val service = MusicService.getInstance()
            if (service != null) {
                Log.d(TAG, "Service found, checking if catalog is ready")
                
                // Try to get the catalog immediately, if not ready, poll for it
                checkCatalogAndCallback(service, callback, 0)
            } else {
                Log.w(TAG, "Service not found")
                callback(emptyList())
            }
        } else {
            Log.d(TAG, "Not connected or not root path")
        }
    }

    private fun checkCatalogAndCallback(service: MusicService, callback: (List<MediaItem>) -> Unit, attempt: Int) {
        val isReady = service.mediaSource.whenReady { success ->
            if (success) {
                Log.d(TAG, "Catalog is ready via whenReady callback (attempt $attempt)")
                val mediaItems = service.mediaSource.map { metadata ->
                    val mediaId = metadata.extras?.getString("media_id") ?: ""
                    val mediaUri = metadata.extras?.getString("media_uri") ?: ""
                    
                    MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(mediaUri)
                        .setMediaMetadata(metadata)
                        .build()
                }
                Log.d(TAG, "Returning ${mediaItems.size} media items via whenReady")
                callback(mediaItems)
            } else {
                Log.w(TAG, "Catalog failed to load")
                callback(emptyList())
            }
        }
        
        if (isReady) {
            // Catalog was already ready, callback was called synchronously
            Log.d(TAG, "Catalog was already ready (attempt $attempt)")
        } else {
            // Catalog is still loading - set up a retry mechanism
            Log.d(TAG, "Catalog is still loading, will retry (attempt $attempt)")
            if (attempt < 10) { // Max 10 attempts (10 seconds)
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "Retrying catalog check (attempt ${attempt + 1})")
                    checkCatalogAndCallback(service, callback, attempt + 1)
                }, 1000) // Check again in 1 second
            } else {
                Log.e(TAG, "Catalog loading timed out after 10 attempts")
                callback(emptyList())
            }
        }
    }

    /**
     * Unsubscribe from a parent media ID
     */
    fun unsubscribe(parentId: String, callback: ((List<MediaItem>) -> Unit)? = null) {
        subscriptions.remove(parentId)
    }

    fun sendCommand(command: String, parameters: Bundle?) {
        mediaBrowser.sendCustomCommand(
            SessionCommand(command, Bundle.EMPTY),
            parameters ?: Bundle.EMPTY
        )
    }
}

@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: Int = Player.STATE_IDLE

@Suppress("PropertyName")
val NOTHING_PLAYING: MediaMetadata = MediaMetadata.Builder().build()

private const val TAG = "MusicServiceConnection"
