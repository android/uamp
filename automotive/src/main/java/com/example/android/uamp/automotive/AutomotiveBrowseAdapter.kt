/*
 * Copyright 2024 Mixtape Player
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

package com.example.android.uamp.automotive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.android.uamp.automotive.databinding.ItemAutomotiveBrowseBinding

/**
 * RecyclerView adapter for browsing music in Android Automotive mode.
 * Optimized for automotive UI guidelines with large touch targets and clear visual hierarchy.
 */
class AutomotiveBrowseAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, AutomotiveBrowseAdapter.ViewHolder>(MediaItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAutomotiveBrowseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAutomotiveBrowseBinding,
        private val onItemClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem) {
            binding.apply {
                // Set basic track information
                trackTitle.text = mediaItem.mediaMetadata.title ?: "Unknown Title"
                artistName.text = mediaItem.mediaMetadata.artist ?: "Unknown Artist"
                albumName.text = mediaItem.mediaMetadata.albumTitle ?: "Unknown Album"

                // For now, hide duration since we can't access it easily
                trackDuration.text = ""

                // Load album artwork with proper placeholder and error handling
                val artworkUri = mediaItem.mediaMetadata.artworkUri
                Glide.with(albumArt.context)
                    .load(artworkUri)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_album_black_24dp)
                            .error(R.drawable.ic_album_black_24dp)
                            .transform(RoundedCorners(8))
                    )
                    .into(albumArt)

                // Set click listener for the entire item
                root.setOnClickListener {
                    onItemClick(mediaItem)
                }

                // Optional: Add ripple effect or highlight for better feedback
                root.isClickable = true
                root.isFocusable = true
            }
        }

        private fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    }

    private class MediaItemDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.mediaId == newItem.mediaId
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.mediaMetadata.title == newItem.mediaMetadata.title &&
                    oldItem.mediaMetadata.artist == newItem.mediaMetadata.artist &&
                    oldItem.mediaMetadata.albumTitle == newItem.mediaMetadata.albumTitle &&
                    oldItem.mediaMetadata.artworkUri == newItem.mediaMetadata.artworkUri
        }
    }
} 