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

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_mediaitem.view.*

/**
 * [RecyclerView.Adapter] of [MediaItemData]s used by the [MediaItemFragment].
 */
class MediaItemAdapter(private val itemClicked: (MediaItemData) -> Unit
) : ListAdapter<MediaItemData, MediaViewHolder>(MediaItemData.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_mediaitem, parent, false)
        return MediaViewHolder(view, itemClicked)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = getItem(position)
        holder.item = mediaItem
        holder.titleView.text = mediaItem.title
        holder.subtitleView.text = mediaItem.subtitle
        holder.playbackState.setImageResource(mediaItem.playbackRes)

        Glide.with(holder.albumArt)
                .load(mediaItem.albumArtUri)
                .into(holder.albumArt)
    }
}

class MediaViewHolder(view: View,
                      itemClicked: (MediaItemData) -> Unit
) : RecyclerView.ViewHolder(view) {

    val titleView: TextView = view.title
    val subtitleView: TextView = view.subtitle
    val albumArt: ImageView = view.albumbArt
    val playbackState: ImageView = view.item_state

    var item: MediaItemData? = null

    init {
        view.setOnClickListener {
            item?.let { itemClicked(it) }
        }
    }
}
