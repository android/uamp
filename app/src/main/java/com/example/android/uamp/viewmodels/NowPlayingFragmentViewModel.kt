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

package com.example.android.uamp.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.common.PlaybackState
import com.example.android.uamp.fragments.NowPlayingFragment

/**
 * [ViewModel] for [NowPlayingFragment] which displays the album art in full size.
 * It extends AndroidViewModel and uses the [Application]'s context to be able to reference string
 * resources.
 */
class NowPlayingFragmentViewModel(
    app: Application,
    musicServiceConnection: MusicServiceConnection
) : AndroidViewModel(app) {

    // Current media item being played
    val mediaItem = MutableLiveData<MediaItem>(MediaItem.EMPTY)

    // Current position of the media item being played
    val mediaPositionSeconds = MutableLiveData<Long>(0L)

    // Duration of the media item being played
    val mediaDurationSeconds = MutableLiveData<Long>(0L)

    // Boolean value to indicate current playback status of the mediaItem
    val mediaIsPlaying = MutableLiveData<Boolean>(true)

    // Boolean value to indicate the current status of spatialization
    val spatializationStatus =
        MutableLiveData<Boolean>(true)

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackState> {
        updateState(
            it, musicServiceConnection.nowPlaying.value!!,
            musicServiceConnection.isAppSpatializationEnabled.value!!
        )
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val nowPlayingObserver = Observer<MediaItem> {
        updateState(
            musicServiceConnection.playbackState.value!!, it,
            musicServiceConnection.isAppSpatializationEnabled.value!!
        )
    }

    /**
     * When the session's spatial audio is toggled, the [spatializationStatus] needs to be updated
     * as it represents the current spatial audio status of the app. As a result, the old
     * spatialization status needs to be updated with the new status while keeping the nowPlaying
     * and playback states the same.
     */
    private val isAppSpatializationEnabledObserver = Observer<Boolean> {
        updateState(
            musicServiceConnection.playbackState.value!!,
            musicServiceConnection.nowPlaying.value!!, it
        )
    }

    /**
     * Because there's a complex dance between this [ViewModel] and the [MusicServiceConnection]
     * (which is wrapping a [MediaBrowserCompat] object), the usual guidance of using
     * [Transformations] doesn't quite work.
     *
     * Specifically there's three things that are watched that will cause the single piece of
     * [LiveData] exposed from this class to be updated.
     *
     * [MusicServiceConnection.playbackState] changes state based on the playback state of
     * the player, which can change the [MediaItemData.playbackRes]s in the list.
     *
     * [MusicServiceConnection.nowPlaying] changes based on the item that's being played,
     * which can also change the [MediaItemData.playbackRes]s in the list.
     */
    private val musicServiceConnection = musicServiceConnection.also {
        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(nowPlayingObserver)
        it.isAppSpatializationEnabled.observeForever(isAppSpatializationEnabledObserver)
        checkPlaybackPosition(POSITION_UPDATE_INTERVAL_MILLIS)
    }

    /**
     * Internal function that recursively calls itself to check the current playback position and
     * updates the corresponding LiveData object when it has changed.
     */
    private fun checkPlaybackPosition(delayMs: Long): Boolean = handler.postDelayed({
        val currPosition = (musicServiceConnection.player?.currentPosition ?: 0) / 1000
        if (mediaPositionSeconds.value != currPosition)
            mediaPositionSeconds.postValue(currPosition)
        if (updatePosition)
            checkPlaybackPosition(100)
    }, delayMs)

    /**
     * Since we use [LiveData.observeForever] above (in [musicServiceConnection]), we want
     * to call [LiveData.removeObserver] here to prevent leaking resources when the [ViewModel]
     * is not longer in use.
     *
     * For more details, see the kdoc on [musicServiceConnection] above.
     */
    override fun onCleared() {
        super.onCleared()

        // Remove the permanent observers from the MusicServiceConnection.
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(nowPlayingObserver)
        musicServiceConnection.isAppSpatializationEnabled
            .removeObserver(isAppSpatializationEnabledObserver)

        // Stop updating the position
        updatePosition = false
    }

    private fun updateState(
        playbackState: PlaybackState,
        mediaItem: MediaItem,
        isAppSpatializationEnabled: Boolean
    ) {

        // Only update media item once we have duration available
        if (playbackState.duration != 0L && !TextUtils.isEmpty(mediaItem.mediaId)) {
            this.mediaItem.postValue(mediaItem)
        }

        mediaDurationSeconds.postValue(playbackState.duration / 1000)

        mediaIsPlaying.postValue(playbackState.isPlaying)

        spatializationStatus.postValue(isAppSpatializationEnabled)

    }

    class Factory(
        private val app: Application,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NowPlayingFragmentViewModel(app, musicServiceConnection) as T
        }
    }
}

private const val POSITION_UPDATE_INTERVAL_MILLIS = 1L
