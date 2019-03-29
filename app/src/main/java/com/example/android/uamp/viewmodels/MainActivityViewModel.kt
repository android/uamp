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

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.android.uamp.MainActivity
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.common.MediaSessionConnection
import com.example.android.uamp.fragments.NowPlayingFragment
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.isPlayEnabled
import com.example.android.uamp.media.extensions.isPlaying
import com.example.android.uamp.media.extensions.isPrepared
import com.example.android.uamp.utils.Event

/**
 * Small [ViewModel] that watches a [MediaSessionConnection] to become connected
 * and provides the root/initial media ID of the underlying [MediaBrowserCompat].
 */
class MainActivityViewModel(
    private val mediaSessionConnection: MediaSessionConnection
) : ViewModel() {

    val rootMediaId: LiveData<String> =
        Transformations.map(mediaSessionConnection.isConnected) { isConnected ->
            if (isConnected) {
                mediaSessionConnection.rootMediaId
            } else {
                null
            }
        }

    /**
     * [navigateToMediaItem] acts as an "event", rather than state. [Observer]s
     * are notified of the change as usual with [LiveData], but only one [Observer]
     * will actually read the data. For more information, check the [Event] class.
     */
    val navigateToMediaItem: LiveData<Event<String>> get() = _navigateToMediaItem
    private val _navigateToMediaItem = MutableLiveData<Event<String>>()

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
    fun mediaItemClicked(clickedItem: MediaItemData) {
        if (clickedItem.browsable) {
            browseToItem(clickedItem)
        } else {
            playMedia(clickedItem, pauseAllowed = false)
            showFragment(NowPlayingFragment.newInstance())
        }
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
     * This posts a browse [Event] that will be handled by the
     * observer in [MainActivity].
     */
    private fun browseToItem(mediaItem: MediaItemData) {
        _navigateToMediaItem.value = Event(mediaItem.mediaId)
    }

    /**
     * This method takes a [MediaItemData] and does one of the following:
     * - If the item is *not* the active item, then play it directly.
     * - If the item *is* the active item, check whether "pause" is a permitted command. If it is,
     *   then pause playback, otherwise send "play" to resume playback.
     */
    fun playMedia(mediaItem: MediaItemData, pauseAllowed: Boolean = true) {
        val nowPlaying = mediaSessionConnection.nowPlaying.value
        val transportControls = mediaSessionConnection.transportControls

        val isPrepared = mediaSessionConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaItem.mediaId == nowPlaying?.id) {
            mediaSessionConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying ->
                        if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=${mediaItem.mediaId})"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }

    fun playMediaId(mediaId: String) {
        val nowPlaying = mediaSessionConnection.nowPlaying.value
        val transportControls = mediaSessionConnection.transportControls

        val isPrepared = mediaSessionConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaId == nowPlaying?.id) {
            mediaSessionConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> transportControls.pause()
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=$mediaId)"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaId, null)
        }
    }

    class Factory(
        private val mediaSessionConnection: MediaSessionConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainActivityViewModel(mediaSessionConnection) as T
        }
    }
}

/**
 * Helper class used to pass fragment navigation requests between MainActivity
 * and its corresponding ViewModel.
 */
data class FragmentNavigationRequest(
    val fragment: Fragment,
    val backStack: Boolean = false,
    val tag: String? = null
)

private const val TAG = "MainActivitytVM"
