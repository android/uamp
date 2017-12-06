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

/**
 * [RecyclerView.Adapter] of [MediaItem]s used by the [MediaItemFragment].
 */
class MediaItemAdapter : RecyclerView.Adapter<MediaViewHolder>() {

    private var mediaItems = emptyList<MediaItem>()

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
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.mItem = mediaItems[position]
        holder.titleView.text = mediaItems[position].description.title
        holder.subtitleView.text = mediaItems[position].description.subtitle
    }

    override fun getItemCount(): Int = mediaItems.size
}

class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titleView: TextView = view.title
    val subtitleView: TextView = view.subtitle
    var mItem: MediaItem? = null
}
