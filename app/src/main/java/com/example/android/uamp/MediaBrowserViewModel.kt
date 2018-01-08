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

@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
        .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
        .build()

/**
 * ViewModel that implements (and holds onto) a MediaBrowser connection.
 */
class MediaBrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaBrowser: MediaBrowserCompat
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()
    private val mediaControllerCallback = MediaControllerCallback()

    private lateinit var mediaController: MediaControllerCompat

    val nowPlayingId
        get() = mediaController.metadata?.id ?: ""
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls
    val playbackState get() = mediaController.playbackState ?: EMPTY_PLAYBACK_STATE

    private val callbacks = ArrayList<MediaBrowserStateChangeCallback>()

    init {
        mediaBrowser = MediaBrowserCompat(
                application,
                ComponentName(application, MusicService::class.java),
                mediaBrowserConnectionCallback,
                null)
        mediaBrowser.connect()
    }

    fun registerCallback(callback: MediaBrowserStateChangeCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)

            if (mediaBrowser.isConnected) {
                callback.onConnected()
            }
        }
    }

    fun unregisterCallback(callback: MediaBrowserStateChangeCallback) {
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

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state == null) {
                return
            }

            callbacks.forEach { callback -> callback.onPlaybackStateChanged(state) }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            if (metadata == null) {
                return
            }

            callbacks.forEach { callback -> callback.onMetadataChanged(metadata) }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()

            // Normally if a MediaBrowserService drops its connection the callback comes via
            // MediaControllerCompat.Callback (here). But since other connection status events
            // are sent to MediaBrowserCompat.MediaBrowserStateChangeCallback, we catch the disconnect here
            // and send it on to the other callback.
            callbacks.forEach { callback -> callback.onConnectionSuspended() }
        }
    }
}

/**
 * Interface to allow a class to receive callbacks based on the changing state of a
 * [android.support.v4.media.MediaBrowserCompat] connection.
 */
interface MediaBrowserStateChangeCallback {
    fun onConnected() {
    }

    fun onConnectionSuspended() {
    }

    fun onConnectionFailed() {
    }

    fun onPlaybackStateChanged(state: PlaybackStateCompat) {
    }

    fun onMetadataChanged(metadata: MediaMetadataCompat) {
    }
}
