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

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.android.uamp.media.MusicService
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
import com.example.android.uamp.media.extensions.writer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Source of [MediaMetadata] objects created from a basic JSON stream.
 *
 * The definition of the JSON is specified in the docs folder of the project.
 */
class JsonSource(private val source: Uri) : AbstractMusicSource() {

    override var catalog: List<MediaMetadata> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaMetadata> = catalog.iterator()

    override suspend fun load() {
        updateCatalog(source)?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    /**
     * Function to connect to a remote URI and download/process the JSON file that contains
     * the catalog of music tracks.
     */
    @Throws(IOException::class)
    private suspend fun updateCatalog(catalogUri: Uri): List<MediaMetadata>? {
        return withContext(Dispatchers.IO) {
            val musicCatalog = try {
                downloadJson(catalogUri)
            } catch (ioException: IOException) {
                Log.e(TAG, "Failed to download catalog", ioException)
                return@withContext null
            }

            // Get the base URI to fix up relative references later.
            val baseUri = catalogUri.toString().removeSuffix(catalogUri.lastPathSegment ?: "")

            musicCatalog.map { song ->
                // The JSON may have paths that are relative to the source of the catalog.
                // We need to fix them up here to turn them into absolute paths.
                val fullArtUri = if (song.image.startsWith("/")) {
                    baseUri + song.image
                } else {
                    song.image
                }

                val fullTrackUri = if (song.source.startsWith("/")) {
                    baseUri + song.source
                } else {
                    song.source
                }

                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setAlbumArtist(song.albumArtist)
                    .setComposer(song.composer)
                    .setTrackNumber(song.trackNumber)
                    .setDiscNumber(song.discNumber)
                    .setArtworkUri(Uri.parse(fullArtUri))
                    .setExtras(Bundle().apply {
                        putString("media_id", song.id)
                        putString("media_uri", fullTrackUri)
                        putLong("duration", TimeUnit.SECONDS.toMillis(song.duration))
                        putLong("download_status", DOWNLOAD_STATUS_NOT_DOWNLOADED)
                        putLong("flag", 1L)
                    })
                    .build()
            }
        }
    }

    /**
     * Download catalog from the network and return the transformed data.
     */
    @Throws(IOException::class)
    private fun downloadJson(catalogUri: Uri): List<JsonMusic> {
        val catalogConn = URL(catalogUri.toString())
        val reader = BufferedReader(InputStreamReader(catalogConn.openStream()))
        return Gson().fromJson(reader, object : TypeToken<List<JsonMusic>>() {}.type)
    }

    /**
     * Wrapper object for our JSON in order to be processed easily by GSON.
     */
    private class JsonMusic {
        var id: String = ""
        var title: String = ""
        var album: String = ""
        var artist: String = ""
        var albumArtist: String = ""
        var composer: String = ""
        var genre: String = ""
        var source: String = ""
        var image: String = ""
        var trackNumber: Int? = null
        var discNumber: Int? = null
        var duration: Long = -1
    }

    companion object {
        const val TAG = "JsonSource"
    }
}

const val DOWNLOAD_STATUS_NOT_DOWNLOADED = 0L
const val DOWNLOAD_STATUS_DOWNLOADING = 1L
const val DOWNLOAD_STATUS_DOWNLOADED = 2L
