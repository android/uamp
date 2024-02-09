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
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import com.example.android.uamp.R
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.common.PlaybackState
import com.example.android.uamp.fragments.NowPlayingFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * [ViewModel] for [NowPlayingFragment] which displays the album art in full size.
 * It extends AndroidViewModel and uses the [Application]'s context to be able to reference string
 * resources.
 */

private const val TAG = "NowPlayingFragmentViewM"

@HiltViewModel
class NowPlayingFragmentViewModel @Inject constructor(
    musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    val mediaItem = MutableLiveData<MediaItem>().apply {
        postValue(MediaItem.EMPTY)
    }
    val mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }
    val mediaDuration = MutableLiveData<Long>().apply {
        postValue(0L)
    }
    val mediaButtonRes = MutableLiveData<Int>().apply {
        postValue(R.drawable.ic_album_black_24dp)
    }

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackState> {
        Log.d(TAG, "nowPlaying: ${it.isPlaying}")
        updateState(it, musicServiceConnection.nowPlaying.value!!)
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val nowPlayingObserver = Observer<MediaItem> {
        Log.d(TAG, "nowPlaying: ${it.mediaId}")
        updateState(musicServiceConnection.playbackState.value!!, it)
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
        checkPlaybackPosition(POSITION_UPDATE_INTERVAL_MILLIS)
    }

    /**
     * Internal function that recursively calls itself to check the current playback position and
     * updates the corresponding LiveData object when it has changed.
     */
    private fun checkPlaybackPosition(delayMs: Long): Boolean = handler.postDelayed({
        val currPosition = musicServiceConnection.player?.currentPosition ?: 0
        if (mediaPosition.value != currPosition)
            mediaPosition.postValue(currPosition)
        if (updatePosition)
            checkPlaybackPosition(1000 - (currPosition % 1000))
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

        // Stop updating the position
        updatePosition = false
    }

    private fun updateState(
        playbackState: PlaybackState,
        mediaItem: MediaItem
    ) {

        // Only update media item once we have duration available
        if (playbackState.duration != 0L && !TextUtils.isEmpty(mediaItem.mediaId)) {
            this.mediaItem.postValue(mediaItem)
        }

        mediaDuration.postValue(playbackState.duration)

        // Update the media button resource ID
        mediaButtonRes.postValue(
            when (playbackState.isPlaying) {
                true -> R.drawable.ic_pause_black_24dp
                else -> R.drawable.ic_play_arrow_black_24dp
            }
        )
    }
}

private const val POSITION_UPDATE_INTERVAL_MILLIS = 1L
