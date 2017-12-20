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

package com.example.android.uamp

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.ComponentName
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.android.uamp.media.MusicService
import com.example.android.uamp.media.extensions.id

/**
 * ViewModel that implements (and holds onto) a MediaBrowser connection.
 */
class MediaBrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaBrowser: MediaBrowserCompat
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()
    private val mediaControllerCallback = MediaControllerCallback()

    private lateinit var mediaController: MediaControllerCompat

    val nowPlayingId
        get() = if (mediaController.metadata != null) mediaController.metadata.id else ""
    val playbackState
        get() =
            if (mediaController.playbackState != null) {
                mediaController.playbackState.state
            } else {
                PlaybackStateCompat.STATE_NONE
            }
    val transportControls get() = mediaController.transportControls

    private val callbacks = ArrayList<ConnectionCallback>()

    init {
        mediaBrowser = MediaBrowserCompat(
                application,
                ComponentName(application, MusicService::class.java),
                mediaBrowserConnectionCallback,
                null)
        mediaBrowser.connect()
    }

    fun registerCallback(callback: ConnectionCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)

            if (mediaBrowser.isConnected) {
                callback.onConnected()
            }
        }
    }

    fun unregisterCallback(callback: ConnectionCallback) {
        if (callbacks.contains(callback)) {
            callbacks.remove(callback)
        }
    }

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    fun getRoot(): String {
        return mediaBrowser.root
    }

    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            // Get a MediaController for the MediaSession.
            mediaController = MediaControllerCompat(getApplication(), mediaBrowser.sessionToken)
            mediaController.registerCallback(mediaControllerCallback)

            callbacks.forEach { callback -> callback.onConnected() }
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()

            callbacks.forEach { callback -> callback.onConnectionSuspended() }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()

            callbacks.forEach { callback -> callback.onConnectionFailed() }
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        private var lastStateUpdateTime = -1L

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state != null && state.lastPositionUpdateTime > lastStateUpdateTime) {
                lastStateUpdateTime = state.lastPositionUpdateTime
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()

            // Normally if a MediaBrowserService drops its connection the callback comes via
            // MediaControllerCompat.Callback (here). But since other connection status events
            // are sent to MediaBrowserCompat.ConnectionCallback, we catch the disconnect here
            // and send it on to the other callback.
            callbacks.forEach { callback -> callback.onConnectionSuspended() }
        }
    }
}

/**
 * Interface to allow a class to receive callbacks based on the changing state of a
 * [MediaBrowser] connection.
 */
interface ConnectionCallback {
    fun onConnected() {
    }

    fun onConnectionSuspended() {
    }

    fun onConnectionFailed() {
    }
}
