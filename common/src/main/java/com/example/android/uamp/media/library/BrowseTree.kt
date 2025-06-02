/*
 * Copyright 2019 Google Inc. All rights reserved.
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

package com.example.android.uamp.media.library

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.android.uamp.media.MusicService
import com.example.android.uamp.media.R
import com.example.android.uamp.media.extensions.album

/**
 * Represents a tree of media that's used by [MusicService.onLoadChildren].
 *
 * [BrowseTree] maps a media id (see: [MediaMetadata.mediaId]) to one (or more)
 * [MediaMetadata] objects.
 *
 * @param context The context to use when building the tree.
 * @param musicSource The [MusicSource] to use when building the tree.
 */
class BrowseTree(
    context: Context,
    private val musicSource: MusicSource
) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadata>>()

    /**
     * Whether to allow clients which are unknown (not on the allowed list) to browse the tree.
     */
    val searchableByUnknownCaller = true

    /**
     * Provide access to the list of children with the `get` operator.
     * i.e.: `browseTree\[UAMP_BROWSABLE_ROOT\]`
     */
    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    init {
        val rootList = mediaIdToChildren.getOrPut(UAMP_BROWSABLE_ROOT) { mutableListOf() }

        val recommendedMetadata = MediaMetadata.Builder()
            .setTitle(context.getString(R.string.recommended_title))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setExtras(Bundle().apply {
                putString("media_id", UAMP_RECOMMENDED)
            })
            .build()

        val albumsMetadata = MediaMetadata.Builder()
            .setTitle(context.getString(R.string.albums_title))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setExtras(Bundle().apply {
                putString("media_id", UAMP_ALBUMS)
            })
            .build()

        rootList.add(recommendedMetadata)
        rootList.add(albumsMetadata)

        // Create the recommended list
        val recommendedList = mediaIdToChildren.getOrPut(UAMP_RECOMMENDED) { mutableListOf() }
        musicSource.forEach { metadata ->
            // Only add music to the 'recommended' list if it has an album
            if (metadata.album != null) {
                recommendedList.add(metadata)
            }
        }

        // Build the albums list
        val albumList = mediaIdToChildren.getOrPut(UAMP_ALBUMS) { mutableListOf() }
        val albumIds = mutableSetOf<String>()
        musicSource.forEach { metadata ->
            val albumId = metadata.album
            if (albumId != null && albumIds.add(albumId)) {
                val albumMetadata = MediaMetadata.Builder()
                    .setTitle(albumId)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(Bundle().apply {
                        putString("media_id", UAMP_ALBUMS_PREFIX + albumId)
                    })
                    .build()
                albumList.add(albumMetadata)
            }
        }

        // Build the songs list for each album
        musicSource.forEach { metadata ->
            val albumId = metadata.album
            if (albumId != null) {
                val albumSongList = mediaIdToChildren.getOrPut(UAMP_ALBUMS_PREFIX + albumId) { mutableListOf() }
                albumSongList.add(metadata)
            }
        }
    }
}

const val UAMP_BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"
const val UAMP_RECOMMENDED = "__RECOMMENDED__"
const val UAMP_ALBUMS = "__ALBUMS__"
const val UAMP_ALBUMS_PREFIX = "__ALBUMS_PREFIX__"
const val UAMP_RECENT_ROOT = "__RECENT__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.example.android.uamp.next/drawable/"
