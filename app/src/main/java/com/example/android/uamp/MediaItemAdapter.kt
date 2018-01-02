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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_mediaitem.view.*

interface MediaItemSelectedCallback {
    fun onPlayableItemClicked(mediaItem: MediaItem)
    fun onBrowsableItemClicked(mediaItem: MediaItem)
}

/**
 * [RecyclerView.Adapter] of [MediaItem]s used by the [MediaItemFragment].
 */
class MediaItemAdapter(val itemSelectedCallback: MediaItemSelectedCallback) :
        RecyclerView.Adapter<MediaViewHolder>() {

    private var mediaItems = emptyList<MediaItem>()

    var currentlyPlayingId: String? = null
    var playerState: PlaybackStateCompat? = null

    private val itemClickedListener: (MediaItem) -> Unit = { mediaItem ->
        if (mediaItem.isPlayable) {
            itemSelectedCallback.onPlayableItemClicked(mediaItem)
        }
        if (mediaItem.isBrowsable) {
            itemSelectedCallback.onBrowsableItemClicked(mediaItem)
        }
    }

    fun setItems(newList: List<MediaItem>) {
        // Rather than simply set the new list, use [DiffUtil] to generate changes so
        // only items that changed are updated.
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = mediaItems.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = mediaItems[oldItemPosition].description.mediaId
                val newItem = newList[newItemPosition].description.mediaId

                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    mediaItems[oldItemPosition] == newList[newItemPosition]
        })

        mediaItems = newList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_mediaitem, parent, false)
        return MediaViewHolder(view, itemClickedListener)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaItems[position]
        holder.item = mediaItem
        holder.titleView.text = mediaItem.description.title
        holder.subtitleView.text = mediaItem.description.subtitle

        if (mediaItem.mediaId == currentlyPlayingId) {

            val actions = playerState?.actions ?: 0
            val stateRes: Int = if (actions and PlaybackStateCompat.ACTION_PLAY != 0L) {
                R.drawable.ic_play_arrow_black_24dp
            } else if (actions and PlaybackStateCompat.ACTION_PAUSE != 0L) {
                R.drawable.ic_pause_black_24dp
            } else {
                0
            }

            if (stateRes == 0) {
                holder.playbackState.visibility = View.INVISIBLE
            } else {
                holder.playbackState.visibility = View.VISIBLE
                holder.playbackState.setImageResource(stateRes)
            }
        } else {
            holder.playbackState.visibility = View.INVISIBLE
        }

        Glide.with(holder.albumArt)
                .load(mediaItems[position].description.iconUri)
                .into(holder.albumArt)
    }

    override fun getItemCount(): Int = mediaItems.size
}

class MediaViewHolder(view: View, itemClicked: (MediaItem) -> Unit) : RecyclerView.ViewHolder(view) {
    val titleView: TextView = view.title
    val subtitleView: TextView = view.subtitle
    val albumArt: ImageView = view.albumb_art
    val playbackState: ImageView = view.item_state

    var item: MediaItem? = null

    init {
        view.setOnClickListener {
            item?.let { itemClicked(it) }
        }
    }
}
