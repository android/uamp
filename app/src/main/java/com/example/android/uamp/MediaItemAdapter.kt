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
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        holder.item = mediaItems[position]
        holder.titleView.text = mediaItems[position].description.title
        holder.subtitleView.text = mediaItems[position].description.subtitle
    }

    override fun getItemCount(): Int = mediaItems.size
}

class MediaViewHolder(view: View, itemClicked: (MediaItem) -> Unit) : RecyclerView.ViewHolder(view) {
    val titleView: TextView = view.title
    val subtitleView: TextView = view.subtitle
    var item: MediaItem? = null

    init {
        view.setOnClickListener {
            item?.let {
                itemClicked(it)
            }
        }
    }
}
