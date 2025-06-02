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

package com.example.android.uamp.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.artist
import com.example.android.uamp.media.extensions.duration
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.mediaUri
import com.example.android.uamp.media.extensions.title
import com.example.android.uamp.media.extensions.toMediaItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Utility class to persist the last played media item and position.
 */
class PersistentStorage private constructor(context: Context) {

    private var preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFERENCES_NAME = "uamp"
        private const val MEDIA_METADATA_KEY = "media_metadata"
        private const val POSITION_KEY = "position"

        @Volatile
        private var instance: PersistentStorage? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PersistentStorage(context).also { instance = it }
            }
    }

    fun loadRecentSong(): MediaItem? {
        val mediaMetadataString = preferences.getString(MEDIA_METADATA_KEY, null)
        if (mediaMetadataString == null) {
            return null
        }

        val mediaMetadata = gson.fromJson<MediaMetadata>(
            mediaMetadataString,
            object : TypeToken<MediaMetadata>() {}.type
        )

        val position = preferences.getLong(POSITION_KEY, 0L)

        // Add the saved position to the metadata object so it can be used by the player.
        val extras = Bundle().apply {
            putLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, position)
        }

        return mediaMetadata.toMediaItem()
    }

    fun saveRecentSong(metadata: MediaMetadata, position: Long) {
        val mediaMetadataString = gson.toJson(
            metadata,
            object : TypeToken<MediaMetadata>() {}.type
        )

        preferences.edit()
            .putString(MEDIA_METADATA_KEY, mediaMetadataString)
            .putLong(POSITION_KEY, position)
            .apply()
    }
}