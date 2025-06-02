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

package com.example.android.uamp.media.extensions

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaStatus
import com.example.android.uamp.media.library.JsonSource

/**
 * Useful extensions for [MediaMetadata].
 */
inline val MediaMetadata.id: String?
    get() = this.extras?.getString("media_id")

inline val MediaMetadata.mediaId: String?
    get() = this.extras?.getString("media_id")

inline val MediaMetadata.title: CharSequence?
    get() = this.title

inline val MediaMetadata.artist: CharSequence?
    get() = this.artist

inline val MediaMetadata.duration
    get() = this.extras?.getLong("duration") ?: 0L

inline val MediaMetadata.album: String?
    get() = this.albumTitle?.toString()

inline val MediaMetadata.author: String?
    get() = this.artist?.toString()

inline val MediaMetadata.writer: String?
    get() = this.composer?.toString()

inline val MediaMetadata.albumArtist: String?
    get() = this.albumArtist?.toString()

inline val MediaMetadata.displayTitle: String?
    get() = this.title?.toString()

inline val MediaMetadata.displaySubtitle: String?
    get() = this.artist?.toString()

inline val MediaMetadata.displayDescription: String?
    get() = this.albumTitle?.toString()

inline val MediaMetadata.displayIconUri: Uri?
    get() = this.artworkUri

inline val MediaMetadata.mediaUri: Uri?
    get() = this.extras?.getString("media_uri")?.let { Uri.parse(it) }

inline val MediaMetadata.downloadStatus
    get() = this.extras?.getLong("download_status") ?: DOWNLOAD_STATUS_NOT_DOWNLOADED

/**
 * Custom property for storing whether a [MediaMetadata] item represents an
 * item that is [MediaItem.FLAG_BROWSABLE] or [MediaItem.FLAG_PLAYABLE].
 */
inline val MediaMetadata.flag
    get() = this.extras?.getLong("flag") ?: 0

/**
 * Extension method for building an [MediaItem] from a [MediaMetadata].
 */
fun MediaMetadata.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(this.extras?.getString("media_id") ?: "")
        .setUri(this.mediaUri)
        .setMediaMetadata(this)
        .build()
}

/**
 * Extension method for building a [MediaMetadata] from a [MediaQueueItem].
 */
fun MediaQueueItem.toMediaMetadata(): MediaMetadata {
    val mediaInfo = this.media
    val metadata = mediaInfo?.metadata

    return MediaMetadata.Builder()
        .setTitle(metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE))
        .setArtist(metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_ARTIST))
        .setAlbumTitle(metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_TITLE))
        .setAlbumArtist(metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_ALBUM_ARTIST))
        .setComposer(metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_COMPOSER))
        .setTrackNumber(metadata?.getInt(com.google.android.gms.cast.MediaMetadata.KEY_TRACK_NUMBER))
        .setDiscNumber(metadata?.getInt(com.google.android.gms.cast.MediaMetadata.KEY_DISC_NUMBER))
        .setArtworkUri(metadata?.images?.firstOrNull()?.url?.let { Uri.parse(it.toString()) })
        .setExtras(Bundle().apply {
            putString("media_uri", mediaInfo?.contentId)
            putLong("duration", mediaInfo?.streamDuration ?: 0L)
            putLong("download_status", DOWNLOAD_STATUS_NOT_DOWNLOADED)
            putLong("flag", 1L) // FLAG_PLAYABLE equivalent
        })
        .build()
}

const val DOWNLOAD_STATUS_NOT_DOWNLOADED = 0L
const val DOWNLOAD_STATUS_DOWNLOADING = 1L
const val DOWNLOAD_STATUS_DOWNLOADED = 2L

/**
 * Custom property that holds whether an item is [MediaItem.FLAG_BROWSABLE] or
 * [MediaItem.FLAG_PLAYABLE].
 */
const val METADATA_KEY_UAMP_FLAGS = "com.example.android.uamp.media.METADATA_KEY_UAMP_FLAGS"

