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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.android.uamp.MediaItemData.Companion.PLAYBACK_RES_CHANGED
import com.example.android.uamp.databinding.FragmentMediaitemBinding
import com.example.android.uamp.fragments.MediaItemFragment

/**
 * [RecyclerView.Adapter] of [MediaItemData]s used by the [MediaItemFragment].
 */
class MediaItemAdapter(
    private val itemClickedListener: (MediaItemData) -> Unit
) : ListAdapter<MediaItemData, MediaViewHolder>(MediaItemData.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentMediaitemBinding.inflate(inflater, parent, false)
        return MediaViewHolder(binding, itemClickedListener)
    }

    override fun onBindViewHolder(
        holder: MediaViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {

        val mediaItem = getItem(position)
        var fullRefresh = payloads.isEmpty()

        if (payloads.isNotEmpty()) {
            payloads.forEach { payload ->
                when (payload) {
                    PLAYBACK_RES_CHANGED -> {
                        holder.playbackState.setImageResource(mediaItem.playbackRes)
                    }
                    // If the payload wasn't understood, refresh the full item (to be safe).
                    else -> fullRefresh = true
                }
            }
        }

        // Normally we only fully refresh the list item if it's being initially bound, but
        // we might also do it if there was a payload that wasn't understood, just to ensure
        // there isn't a stale item.
        if (fullRefresh) {
            holder.item = mediaItem
            holder.titleView.text = mediaItem.title
            holder.subtitleView.text = mediaItem.subtitle
            holder.playbackState.setImageResource(mediaItem.playbackRes)

            Glide.with(holder.albumArt)
                .load(mediaItem.albumArtUri)
                .placeholder(R.drawable.default_art)
                .into(holder.albumArt)
        }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf())
    }
}

class MediaViewHolder(
    binding: FragmentMediaitemBinding,
    itemClickedListener: (MediaItemData) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    val titleView: TextView = binding.title
    val subtitleView: TextView = binding.subtitle
    val albumArt: ImageView = binding.albumArt
    val playbackState: ImageView = binding.itemState

    var item: MediaItemData? = null

    init {
        binding.root.setOnClickListener {
            item?.let { itemClickedListener(it) }
        }
    }
}
