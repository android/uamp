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

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.android.uamp.media.extensions.isPauseEnabled
import com.example.android.uamp.media.extensions.isPlayEnabled

private const val MEDIA_ID_ARG = "com.example.android.uamp.MediaItemFragment.MEDIA_ID"
private const val TAG = "MediaItemFragment"

/**
 * A fragment representing a list of MediaItems.
 */
class MediaItemFragment : Fragment(), MediaBrowserStateChangeCallback, MediaAdapterInterface {
    private lateinit var mediaId: String
    private lateinit var mediaBrowserConnection: MediaBrowserViewModel

    private val subscriptionCallback = SubscriptionCallback()
    private val listAdapter = MediaItemAdapter(this)

    companion object {

        fun newInstance(mediaId: String): MediaItemFragment {
            return MediaItemFragment().apply {
                arguments = Bundle().apply {
                    putString(MEDIA_ID_ARG, mediaId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
        mediaId = arguments!!.getString(MEDIA_ID_ARG)

        mediaBrowserConnection =
                ViewModelProviders.of(activity!!).get(MediaBrowserViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mediaitem_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()

            view.layoutManager = LinearLayoutManager(context)
            view.adapter = listAdapter
        }
        return view
    }

    override fun onStart() {
        super.onStart()

        mediaBrowserConnection.registerCallback(this)
    }

    override fun onStop() {
        super.onStop()

        mediaBrowserConnection.unsubscribe(mediaId, subscriptionCallback)
        mediaBrowserConnection.unregisterCallback(this)
    }

    override fun onConnected() {
        super.onConnected()

        mediaBrowserConnection.subscribe(mediaId, subscriptionCallback)
    }

    override val currentlyPlayingId: String get() = mediaBrowserConnection.nowPlayingId

    override val playerState: PlaybackStateCompat get() = mediaBrowserConnection.playbackState

    override fun onPlayableItemClicked(mediaItem: MediaBrowserCompat.MediaItem) {
        if (mediaItem.mediaId != mediaBrowserConnection.nowPlayingId) {
            mediaBrowserConnection
                    .transportControls
                    .playFromMediaId(mediaItem.mediaId, null)
        } else {
            if (mediaBrowserConnection.playbackState.isPauseEnabled) {
                mediaBrowserConnection.transportControls.pause()
            } else if (mediaBrowserConnection.playbackState.isPlayEnabled) {
                mediaBrowserConnection.transportControls.play()
            } else {
                Log.w(TAG, "Playable item clicked but neither play nor pause are enabled!" +
                        " (mediaId=${mediaItem.mediaId})")
            }
        }
    }

    // TODO: Implement browseable items.
    override fun onBrowsableItemClicked(mediaItem: MediaBrowserCompat.MediaItem) = Unit

    override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
        listAdapter.notifyDataSetChanged()
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        listAdapter.notifyDataSetChanged()
    }

    private inner class SubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String,
                                      children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)

            listAdapter.setItems(children)
        }
    }
}
