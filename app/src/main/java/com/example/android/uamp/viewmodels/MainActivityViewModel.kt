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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.android.uamp.MainActivity
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.fragments.NowPlayingFragment
import com.example.android.uamp.media.extensions.isEnded
import com.example.android.uamp.media.extensions.isPlayEnabled
import com.example.android.uamp.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Small [ViewModel] that watches a [MusicServiceConnection] to become connected
 * and provides the root/initial media ID of the underlying [MediaBrowserCompat].
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private lateinit var lastBrowsableMediaId: String

    val rootMediaItem: LiveData<MediaItem?> =
        musicServiceConnection.rootMediaItem.map { rootMediaItem ->
            if (rootMediaItem != MediaItem.EMPTY) rootMediaItem else null
        }

    override fun onCleared() {
        Log.d(TAG, "onCleared")
        super.onCleared()
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
            playMedia(clickedItem.mediaItem, false, clickedItem.parentMediaId)
            showFragment(NowPlayingFragment())
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
    private fun browseToItem(mediaItemData: MediaItemData) {
        if (mediaItemData.browsable) {
            lastBrowsableMediaId = mediaItemData.mediaItem.mediaId
        }
        _navigateToMediaItem.value = Event(mediaItemData.mediaItem.mediaId)
    }

    /**
     * Play media.
     *
     * @param mediaItem The media item to play.
     * @param pauseThenPlaying Whether to pause playing when the item is already being played.
     * @param parentMediaId The media item to load the its children and set them as playlist.
     */
    fun playMedia(
        mediaItem: MediaItem,
        pauseThenPlaying: Boolean = true,
        parentMediaId: String? = null
    ) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val player = musicServiceConnection.player ?: return

        val isPrepared = player.playbackState != Player.STATE_IDLE
        if (isPrepared && mediaItem.mediaId == nowPlaying?.mediaId) {
            when {
                player.isPlaying ->
                    if (pauseThenPlaying) player.pause() else Unit

                player.isPlayEnabled -> player.play()
                player.isEnded -> player.seekTo(C.TIME_UNSET)
                else -> {
                    Log.w(
                        TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                " (mediaId=${mediaItem.mediaId})"
                    )
                }
            }
        } else {
            viewModelScope.launch {
                var playlist: MutableList<MediaItem> = arrayListOf()
                // load the children of the parent if requested
                parentMediaId?.let {
                    playlist = musicServiceConnection.getChildren(parentMediaId).let { children ->
                        children.filter {
                            it.mediaMetadata.isPlayable ?: false
                        }
                    }.toMutableList()
                }
                if (playlist.isEmpty()) {
                    playlist.add(mediaItem)
                }
                val indexOf = playlist.indexOf(mediaItem)
                val startWindowIndex = if (indexOf >= 0) indexOf else 0
                player.setMediaItems(
                    playlist, startWindowIndex, /* startPositionMs= */ C.TIME_UNSET
                )
                player.prepare()
                player.play()
            }
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
