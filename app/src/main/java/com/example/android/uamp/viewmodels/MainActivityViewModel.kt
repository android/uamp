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

import android.net.Uri
import androidx.media3.session.MediaBrowser
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.example.android.uamp.MainActivity
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.R
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.fragments.MediaItemFragment
import com.example.android.uamp.fragments.NowPlayingFragment
import com.example.android.uamp.utils.Event

/**
 * Small [ViewModel] that watches a [MusicServiceConnection] to become connected
 * and provides the root/initial media ID of the underlying [MediaBrowser].
 */
class MainActivityViewModel(
    val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems

    private val _navigateToMediaItem = MutableLiveData<Event<String>>()
    val navigateToMediaItem: LiveData<Event<String>> = _navigateToMediaItem

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkFailure
    val playbackState = musicServiceConnection.playbackState
    val nowPlaying = musicServiceConnection.nowPlaying

    private val currentMediaId: String?
        get() = musicServiceConnection.nowPlaying.value?.title?.toString()

    private fun getResourceForMediaId(mediaId: String): Int {
        val isActive = mediaId == currentMediaId
        val isPlaying = musicServiceConnection.isPlaying.value == true

        return when {
            !isActive -> 0
            isPlaying -> R.drawable.ic_pause
            else -> R.drawable.ic_play_arrow_black_24dp
        }
    }

    /**
     * [LiveData] object containing whether there's a currently playing item or not.
     */
    val isPlaying: LiveData<Boolean> = musicServiceConnection.isPlaying

    /**
     * [LiveData] object containing whether the current item is a video or not.
     */
    val isVideo: LiveData<Boolean> = nowPlaying.map { metadata ->
        false // Simplified - assume no videos for now
    }

    /**
     * [LiveData] object containing whether the current item is skippable or not.
     */
    val isSkippable: LiveData<Boolean> = nowPlaying.map { metadata ->
        true // Simplified - assume all items are skippable
    }

    /**
     * [LiveData] object containing whether the current item is playing from a local source or not.
     */
    val isLocalSource: LiveData<Boolean> = nowPlaying.map { metadata ->
        false // Simplified - assume all items are remote
    }

    /**
     * This [LiveData] object is used to notify the MainActivity that the main
     * content fragment needs to be swapped. Information about the new fragment
     * is conveniently wrapped by the [Event] class.
     */
    val navigateToFragment: LiveData<Event<FragmentNavigationRequest>> get() = _navigateToFragment
    private val _navigateToFragment = MutableLiveData<Event<FragmentNavigationRequest>>()

    /**
     * This method takes a [MediaItemData] and routes it depending on whether it's
     * browsable (i.e.: it's the parent media item of a set of other media items,
     * such as an album), or not.
     *
     * If the item is browsable, handle it by sending an event to the Activity to
     * browse to it, otherwise play it.
     */
    fun mediaItemClicked(clickedItem: MediaItem) {
        _navigateToMediaItem.value = Event(clickedItem.mediaId)
    }

    /**
     * Convenience method used to swap the fragment shown in the main activity
     *
     * @param fragment the fragment to show
     * @param backStack if true, add this transaction to the back stack
     * @param tag the name to use for this fragment in the stack
     */
    fun showFragment(fragment: Fragment, backStack: Boolean = true, tag: String? = null) {
        _navigateToFragment.value = Event(FragmentNavigationRequest(fragment, backStack, tag))
    }

    /**
     * Navigate to a browsable [MediaItemData].
     */
    private fun browseToItem(mediaItem: MediaItemData) {
        _navigateToMediaItem.value = Event(mediaItem.mediaId)
    }

    /**
     * This method takes a [MediaItemData] and calls [MusicServiceConnection.playMedia]
     * to play that item.
     */
    private fun playMedia(mediaItem: MediaItemData) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val isConnected = musicServiceConnection.isConnected.value == true

        if (isConnected) {
            if (mediaItem.mediaId == currentMediaId) {
                // Same item, toggle play/pause
                if (musicServiceConnection.isPlaying.value == true) {
                    musicServiceConnection.pause()
                } else {
                    musicServiceConnection.play()
                }
            } else {
                // Different item, play it
                musicServiceConnection.playMedia(mediaItem.mediaId)
            }
        }
    }

    class Factory(
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(musicServiceConnection) as T
        }
    }
}

/**
 * Helper class to encapsulate data for [Fragment] navigation.
 */
data class FragmentNavigationRequest(
    val fragment: Fragment,
    val backStack: Boolean = true,
    val tag: String? = null
) {
    companion object {
        fun Show(fragment: Fragment, backStack: Boolean = true) =
            FragmentNavigationRequest(fragment, backStack)

        fun Browse(mediaId: String) =
            FragmentNavigationRequest(MediaItemFragment.newInstance(mediaId))
    }
}

private const val TAG = "MainActivitytVM"

data class MediaItemData(
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val albumArtUri: Uri,
    val browsable: Boolean,
    val playbackRes: Int
)
