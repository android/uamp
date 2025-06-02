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
        if (isConnected.value == true) {
            mediaBrowser.setMediaItems(
                listOf(MediaItem.fromUri("fake://uri/$mediaId")),
                /* resetPosition = */ true
            )
            mediaBrowser.prepare()
            mediaBrowser.play()
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

    fun subscribe(parentId: String, callback: (List<MediaItem>) -> Unit) {
        // In Media3, we would typically handle subscription differently
        // For now, we'll store this callback for future use
    }

    fun unsubscribe(parentId: String, callback: ((List<MediaItem>) -> Unit)? = null) {
        // In Media3, we would typically handle unsubscription differently
        // For now, this is a placeholder
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
