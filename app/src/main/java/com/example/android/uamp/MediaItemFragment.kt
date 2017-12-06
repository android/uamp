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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

private const val MEDIA_ID_ARG = "com.example.android.uamp.MediaItemFragment.MEDIA_ID"

/**
 * A fragment representing a list of MediaItems.
 */
class MediaItemFragment : Fragment(), ConnectionCallback {
    private lateinit var mediaId: String
    private lateinit var mediaBrowserConnection: MediaBrowserViewModel

    private val subscriptionCallback = SubscriptionCallback()
    private val listAdapter = MediaItemAdapter()

    companion object {

        // TODO: Customize parameter initialization
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
        mediaId = arguments.getString(MEDIA_ID_ARG)

        mediaBrowserConnection = ViewModelProviders.of(this).get(MediaBrowserViewModel::class.java)
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

    private inner class SubscriptionCallback : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String,
                                      children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)

            listAdapter.setItems(children)
        }
    }
}
