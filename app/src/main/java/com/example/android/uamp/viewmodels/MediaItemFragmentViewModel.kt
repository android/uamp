/*
 * Copyright 2018 Google Inc. All rights reserved.
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

import androidx.media3.session.MediaBrowser
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.R
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.fragments.MediaItemFragment

/**
 * [ViewModel] for [MediaItemFragment].
 */
class MediaItemFragmentViewModel(
    private val mediaId: String,
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    /**
     * Use a backing property so consumers of mediaItems only get a [LiveData] instance so
     * they don't inadvertently modify it.
     */
    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems

    /**
     * Pass the status of the [MusicServiceConnection.networkFailure] through.
     */
    val networkError = musicServiceConnection.networkFailure.map { it }

    private val currentMediaTitle: String?
        get() = musicServiceConnection.nowPlaying.value?.title?.toString()

    /**
     * When the session's [Player] state changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<Int> {
        val playbackState = it ?: Player.STATE_IDLE
        val metadata = musicServiceConnection.nowPlaying.value
        if (metadata != null) {
            _mediaItems.postValue(updateState(playbackState, metadata))
        }
    }

    /**
     * When the session's [MediaMetadata] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val mediaMetadataObserver = Observer<MediaMetadata> {
        val playbackState = musicServiceConnection.playbackState.value ?: Player.STATE_IDLE
        val metadata = it
        if (metadata != null) {
            _mediaItems.postValue(updateState(playbackState, metadata))
        }
    }

    init {
        // Subscribe to changes
        musicServiceConnection.playbackState.observeForever(playbackStateObserver)
        musicServiceConnection.nowPlaying.observeForever(mediaMetadataObserver)
        
        // Load the media items for this mediaId
        loadMediaItems()
    }

    private fun loadMediaItems() {
        // For now, create a simple mock implementation
        // In a real implementation, this would subscribe to a MediaBrowser
        val items = listOf<MediaItemData>()
        _mediaItems.value = items
    }

    /**
     * Play the media item with the given ID
     */
    fun playMediaId(mediaId: String) {
        musicServiceConnection.playMedia(mediaId)
    }

    private fun getResourceForMediaId(itemTitle: String): Int {
        val isActive = itemTitle == currentMediaTitle
        val isPlaying = musicServiceConnection.playbackState.value == Player.STATE_READY && 
                       musicServiceConnection.isPlaying.value == true

        return when {
            !isActive -> 0
            isPlaying -> R.drawable.ic_pause
            else -> R.drawable.ic_play_arrow_black_24dp
        }
    }

    private fun updateState(
        playbackState: Int,
        mediaMetadata: MediaMetadata
    ): List<MediaItemData> {

        val newResId = when {
            playbackState == Player.STATE_READY && musicServiceConnection.isPlaying.value == true -> 
                R.drawable.ic_pause
            else -> R.drawable.ic_play_arrow_black_24dp
        }

        return mediaItems.value?.map {
            val useResId = if (it.title == mediaMetadata.title?.toString()) newResId else 0
            it.copy(playbackRes = useResId)
        } ?: emptyList()
    }

    /**
     * Since we use [LiveData.observeForever] above, we want to remove those listeners when this
     * ViewModel is not longer in use.
     */
    override fun onCleared() {
        super.onCleared()

        // Remove the permanent observers from the MusicServiceConnection.
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)
    }

    class Factory(
        private val mediaId: String,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MediaItemFragmentViewModel(mediaId, musicServiceConnection) as T
        }
    }
}

private const val TAG = "MediaItemFragmentVM"
private const val NO_RES = 0
