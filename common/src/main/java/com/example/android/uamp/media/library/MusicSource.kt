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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.MediaMetadata
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.albumArtist
import com.example.android.uamp.media.extensions.artist
import com.example.android.uamp.media.extensions.displayDescription
import com.example.android.uamp.media.extensions.displayIconUri
import com.example.android.uamp.media.extensions.displaySubtitle
import com.example.android.uamp.media.extensions.displayTitle
import com.example.android.uamp.media.extensions.downloadStatus
import com.example.android.uamp.media.extensions.duration
import com.example.android.uamp.media.extensions.flag
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.mediaUri
import com.example.android.uamp.media.extensions.title

/**
 * Interface used by [MusicService] for looking up [MediaMetadataCompat] objects.
 *
 * Because Kotlin provides methods such as [Iterable.find] and [Iterable.filter],
 * this is a convenient interface to have on sources.
 */
interface MusicSource : Iterable<MediaMetadata> {

    /**
     * Begins loading the data for this music source.
     */
    suspend fun load()

    /**
     * Method which will perform a given action after this [MusicSource] is ready to be used.
     *
     * @param performAction A lambda expression to be called with a boolean parameter when
     * the source is ready. `true` indicates the source was successfully prepared, `false`
     * indicates an error occurred.
     */
    fun whenReady(performAction: (Boolean) -> Unit): Boolean

    /**
     * Handles searching a [MusicSource] from a focused voice search, often coming
     * from the Google Assistant.
     */
    fun search(query: String, extras: Bundle?): List<MediaMetadata>
}

abstract class AbstractMusicSource : MusicSource {
    protected var state = STATE_CREATED
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    /**
     * Performs an action when this MusicSource is ready.
     *
     * This method is *not* threadsafe. Ensure actions and state changes are only performed on a
     * single thread.
     */
    override fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }
            STATE_INITIALIZED -> {
                performAction(true)
                true
            }
            else -> {
                performAction(false)
                true
            }
        }

    /**
     * Implements [Iterable] so that collection extensions can be used on [MusicSource].
     */
    override fun iterator(): Iterator<MediaMetadata> = catalog.iterator()

    /**
     * Implementation of [MusicSource.search].
     */
    override fun search(query: String, extras: Bundle?): List<MediaMetadata> {
        // First attempt to search by artist.
        val artistSearch = searchByArtist(query)
        if (artistSearch.isNotEmpty()) return artistSearch

        // Then attempt to search by album.
        val albumSearch = searchByAlbum(query)
        if (albumSearch.isNotEmpty()) return albumSearch

        // Finally, attempt to search by title.
        return searchByTitle(query)
    }

    /**
     * Helper method for building searches.
     */
    private fun searchByArtist(query: String) = catalog.filter { metadata ->
        metadata.artist?.contains(query, true) == true ||
                metadata.albumArtist?.contains(query, true) == true
    }

    /**
     * Helper method for building searches.
     */
    private fun searchByAlbum(query: String) = catalog.filter { metadata ->
        metadata.album?.contains(query, true) == true
    }

    /**
     * Helper method for building searches.
     */
    private fun searchByTitle(query: String) = catalog.filter { metadata ->
        metadata.title?.contains(query, true) == true
    }

    protected open var catalog: List<MediaMetadata> = emptyList()
}

const val STATE_CREATED = 1
const val STATE_INITIALIZING = 2
const val STATE_INITIALIZED = 3
const val STATE_ERROR = 4
