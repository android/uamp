/*
 * Copyright 2020 Google Inc. All rights reserved.
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
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import com.bumptech.glide.Glide
import com.example.android.uamp.media.extensions.asAlbumArtContentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PersistentStorage private constructor(val context: Context) {

    /**
     * Store any data which must persist between restarts, such as the most recently played song.
     */
    private var preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    companion object {

        @Volatile
        private var instance: PersistentStorage? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PersistentStorage(context).also { instance = it }
            }
    }

    suspend fun saveRecentSong(description: MediaDescriptionCompat, position: Long) {

        withContext(Dispatchers.IO) {

            /**
             * After booting, Android will attempt to build static media controls for the most
             * recently played song. Artwork for these media controls should not be loaded
             * from the network as it may be too slow or unavailable immediately after boot. Instead
             * we convert the iconUri to point to the Glide on-disk cache.
             */
            val localIconUri = Glide.with(context).asFile().load(description.iconUri)
                .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE).get()
                .asAlbumArtContentUri()

            preferences.edit()
                .putString(RECENT_SONG_MEDIA_ID_KEY, description.mediaId)
                .putString(RECENT_SONG_TITLE_KEY, description.title.toString())
                .putString(RECENT_SONG_SUBTITLE_KEY, description.subtitle.toString())
                .putString(RECENT_SONG_ICON_URI_KEY, localIconUri.toString())
                .putLong(RECENT_SONG_POSITION_KEY, position)
                .apply()
        }
    }

    fun loadRecentSong(): MediaBrowserCompat.MediaItem? {
        val mediaId = preferences.getString(RECENT_SONG_MEDIA_ID_KEY, null)
        return if (mediaId == null) {
            null
        } else {
            val extras = Bundle().also {
                val position = preferences.getLong(RECENT_SONG_POSITION_KEY, 0L)
                it.putLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, position)
            }

            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(preferences.getString(RECENT_SONG_TITLE_KEY, ""))
                    .setSubtitle(preferences.getString(RECENT_SONG_SUBTITLE_KEY, ""))
                    .setIconUri(Uri.parse(preferences.getString(RECENT_SONG_ICON_URI_KEY, "")))
                    .setExtras(extras)
                    .build(), FLAG_PLAYABLE
            )
        }
    }
}

private const val PREFERENCES_NAME = "uamp"
private const val RECENT_SONG_MEDIA_ID_KEY = "recent_song_media_id"
private const val RECENT_SONG_TITLE_KEY = "recent_song_title"
private const val RECENT_SONG_SUBTITLE_KEY = "recent_song_subtitle"
private const val RECENT_SONG_ICON_URI_KEY = "recent_song_icon_uri"
private const val RECENT_SONG_POSITION_KEY = "recent_song_position"