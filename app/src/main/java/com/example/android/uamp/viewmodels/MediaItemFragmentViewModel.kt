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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.R
import com.example.android.uamp.common.EMPTY_PLAYBACK_STATE
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.common.NOTHING_PLAYING
import com.example.android.uamp.common.PlaybackState
import com.example.android.uamp.fragments.MediaItemFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/**
 * [ViewModel] for [MediaItemFragment].
 */
@HiltViewModel(assistedFactory = MediaItemViewModelFactory::class)
class MediaItemFragmentViewModel @AssistedInject constructor(
    @Assisted private val mediaId: String,
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    /**
     * Use a backing property so consumers of mediaItems only get a [LiveData] instance so
     * they don't inadvertently modify it.
     */
    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
    val mediaItems: LiveData<List<MediaItemData>>
        get() = _mediaItems

    /**
     * Pass the status of the [MusicServiceConnection.networkFailure] through.
     */
    val networkError: LiveData<Boolean>
        get() = serviceConnection.networkFailure

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackState> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE
        val mediaItem = musicServiceConnection.nowPlaying.value ?: NOTHING_PLAYING
        if (mediaItem != MediaItem.EMPTY) {
            _mediaItems.postValue(updateState(playbackState, mediaItem))
        }
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val nowPlayingObserver = Observer<MediaItem> {
        val playbackState = musicServiceConnection.playbackState.value ?: EMPTY_PLAYBACK_STATE
        val mediaItem = it ?: NOTHING_PLAYING
        if (mediaItem != MediaItem.EMPTY) {
            _mediaItems.postValue(updateState(playbackState, mediaItem))
        }
    }

    /**
     * Because there's a complex dance between this [ViewModel] and the [MusicServiceConnection]
     * (which is wrapping a [MediaBrowser] object), the usual guidance of using
     * [Transformations] doesn't quite work.
     *
     * Specifically there's three things that are watched that will cause the single piece of
     * [LiveData] exposed from this class to be updated.
     *
     * [MusicServiceConnection.playbackState] changes state based on the playback state of
     * the player, which can change the [MediaItemData.playbackRes]s in the list to possibly mark
     * the playing item in the list.
     *
     * [MusicServiceConnection.nowPlaying] changes based on the item that's being played,
     * which can also change the [MediaItemData.playbackRes]s in the list.
     */
    private val serviceConnection = musicServiceConnection.also {
        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(nowPlayingObserver)
    }

    init {
        viewModelScope.launch {
            val mediaItemList = musicServiceConnection.getChildren(mediaId)
            val itemsList = mediaItemList.map { child ->
                MediaItemData(
                    child,
                    child.mediaMetadata.isPlayable?.not() == true,
                    getResourceForMediaId(child.mediaId),
                    /* parentMediaId= */ mediaId
                )
            }
            _mediaItems.postValue(itemsList)
        }
    }

    /**
     * Since we use [LiveData.observeForever] above (in [serviceConnection]), we want
     * to call [LiveData.removeObserver] here to prevent leaking resources when the [ViewModel]
     * is not longer in use.
     *
     * For more details, see the kdoc on [serviceConnection] above.
     */
    override fun onCleared() {
        super.onCleared()
        // Remove the permanent observers from the MusicServiceConnection.
        serviceConnection.playbackState.removeObserver(playbackStateObserver)
        serviceConnection.nowPlaying.removeObserver(nowPlayingObserver)
    }

    private fun getResourceForMediaId(mediaId: String): Int {
        val isActive = mediaId == serviceConnection.nowPlaying.value?.mediaId
        val isPlaying = serviceConnection.playbackState.value?.isPlaying ?: false
        return when {
            !isActive -> NO_RES
            isPlaying -> R.drawable.ic_pause_black_24dp
            else -> R.drawable.ic_play_arrow_black_24dp
        }
    }

    private fun updateState(
        playbackState: PlaybackState,
        nowPlaying: MediaItem
    ): List<MediaItemData> {

        val newResId = when (playbackState.isPlaying) {
            true -> R.drawable.ic_pause_black_24dp
            else -> R.drawable.ic_play_arrow_black_24dp
        }

        return mediaItems.value?.map {
            val useResId = if (it.mediaItem.mediaId == nowPlaying.mediaId) newResId else NO_RES
            it.copy(playbackRes = useResId)
        } ?: emptyList()
    }
}

private const val NO_RES = 0
