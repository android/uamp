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
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage

/**
 * Useful extensions for [MediaMetadataCompat].
 */
inline val MediaMetadataCompat.id: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

inline val MediaMetadataCompat.title: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_TITLE)

inline val MediaMetadataCompat.artist: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

inline val MediaMetadataCompat.duration
    get() = getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

inline val MediaMetadataCompat.album: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_ALBUM)

inline val MediaMetadataCompat.author: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)

inline val MediaMetadataCompat.writer: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_WRITER)

inline val MediaMetadataCompat.composer: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_COMPOSER)

inline val MediaMetadataCompat.compilation: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_COMPILATION)

inline val MediaMetadataCompat.date: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_DATE)

inline val MediaMetadataCompat.year: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_YEAR)

inline val MediaMetadataCompat.genre: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_GENRE)

inline val MediaMetadataCompat.trackNumber
    get() = getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)

inline val MediaMetadataCompat.trackCount
    get() = getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS)

inline val MediaMetadataCompat.discNumber
    get() = getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER)

inline val MediaMetadataCompat.albumArtist: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)

inline val MediaMetadataCompat.art: Bitmap
    get() = getBitmap(MediaMetadataCompat.METADATA_KEY_ART)

inline val MediaMetadataCompat.artUri: Uri
    get() = this.getString(MediaMetadataCompat.METADATA_KEY_ART_URI).toUri()

inline val MediaMetadataCompat.albumArt: Bitmap?
    get() = getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

inline val MediaMetadataCompat.albumArtUri: Uri
    get() = this.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI).toUri()

inline val MediaMetadataCompat.userRating
    get() = getLong(MediaMetadataCompat.METADATA_KEY_USER_RATING)

inline val MediaMetadataCompat.rating
    get() = getLong(MediaMetadataCompat.METADATA_KEY_RATING)

inline val MediaMetadataCompat.displayTitle: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)

inline val MediaMetadataCompat.displaySubtitle: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)

inline val MediaMetadataCompat.displayDescription: String?
    get() = getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION)

inline val MediaMetadataCompat.displayIcon: Bitmap
    get() = getBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON)

inline val MediaMetadataCompat.displayIconUri: Uri
    get() = this.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI).toUri()

inline val MediaMetadataCompat.mediaUri: Uri
    get() = this.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI).toUri()

inline val MediaMetadataCompat.downloadStatus
    get() = getLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS)

/**
 * Custom property for storing whether a [MediaMetadataCompat] item represents an
 * item that is [MediaItem.FLAG_BROWSABLE] or [MediaItem.FLAG_PLAYABLE].
 */
inline val MediaMetadataCompat.flag
    get() = this.getLong(METADATA_KEY_UAMP_FLAGS).toInt()

/**
 * Useful extensions for [MediaMetadataCompat.Builder].
 */

// These do not have getters, so create a message for the error.
const val NO_GET = "Property does not have a 'get'"

inline var MediaMetadataCompat.Builder.id: String
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, value)
    }

inline var MediaMetadataCompat.Builder.title: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, value)
    }

inline var MediaMetadataCompat.Builder.artist: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, value)
    }

inline var MediaMetadataCompat.Builder.album: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_ALBUM, value)
    }

inline var MediaMetadataCompat.Builder.duration: Long
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, value)
    }

inline var MediaMetadataCompat.Builder.genre: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_GENRE, value)
    }

inline var MediaMetadataCompat.Builder.mediaUri: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, value)
    }

inline var MediaMetadataCompat.Builder.albumArtUri: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, value)
    }

inline var MediaMetadataCompat.Builder.albumArt: Bitmap?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, value)
    }

inline var MediaMetadataCompat.Builder.trackNumber: Long
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, value)
    }

inline var MediaMetadataCompat.Builder.trackCount: Long
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, value)
    }

inline var MediaMetadataCompat.Builder.displayTitle: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, value)
    }

inline var MediaMetadataCompat.Builder.displaySubtitle: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, value)
    }

inline var MediaMetadataCompat.Builder.displayDescription: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, value)
    }

inline var MediaMetadataCompat.Builder.displayIconUri: String?
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, value)
    }

inline var MediaMetadataCompat.Builder.downloadStatus: Long
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putLong(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS, value)
    }

/**
 * Custom property for storing whether a [MediaMetadataCompat] item represents an
 * item that is [MediaItem.FLAG_BROWSABLE] or [MediaItem.FLAG_PLAYABLE].
 */
inline var MediaMetadataCompat.Builder.flag: Int
    @Deprecated(NO_GET, level = DeprecationLevel.ERROR)
    get() = throw IllegalAccessException("Cannot get from MediaMetadataCompat.Builder")
    set(value) {
        putLong(METADATA_KEY_UAMP_FLAGS, value.toLong())
    }

/**
 * Extension method for building an [ExtractorMediaSource] from a [MediaMetadataCompat] object.
 *
 * For convenience, place the [MediaDescriptionCompat] into the tag so it can be retrieved later.
 */
fun MediaMetadataCompat.toMediaSource(dataSourceFactory: DataSource.Factory) =
    ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaUri)

/**
 * Extension method for building a [ConcatenatingMediaSource] given a [List]
 * of [MediaMetadataCompat] objects.
 */
fun List<MediaMetadataCompat>.toMediaSource(
    dataSourceFactory: DataSource.Factory
): ConcatenatingMediaSource {

    val concatenatingMediaSource = ConcatenatingMediaSource()
    forEach {
        concatenatingMediaSource.addMediaSource(it.toMediaSource(dataSourceFactory))
    }
    return concatenatingMediaSource
}

fun MediaMetadataCompat.toMediaQueueItem(): MediaQueueItem {
    val metadata: MediaMetadata = toCastMediaMetadata()
    val mediaInfo = MediaInfo.Builder(this.mediaUri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.AUDIO_MPEG)
            .setStreamDuration(this.duration)
            .setMetadata(metadata)
            .build()
    return MediaQueueItem.Builder(mediaInfo).build()
}

private fun MediaMetadataCompat.toCastMediaMetadata(): MediaMetadata {
    val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
    mediaMetadata.putString(MediaMetadata.KEY_TITLE, this.title)
    mediaMetadata.putString(MediaMetadata.KEY_ARTIST, this.artist)
    mediaMetadata.putString(MediaMetadata.KEY_ALBUM_TITLE, this.album)
    mediaMetadata.addImage(WebImage(this.albumArtUri))
    mediaMetadata.addImage(WebImage(this.displayIconUri))
    mediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, this.albumArtist)
    mediaMetadata.putString(MediaMetadata.KEY_COMPOSER, this.composer)
    this.date?.let { date -> mediaMetadata.putString(MediaMetadata.KEY_RELEASE_DATE, date) }
    mediaMetadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, this.trackNumber.toInt())
    mediaMetadata.putInt(MediaMetadata.KEY_DISC_NUMBER, this.discNumber.toInt())
    return mediaMetadata
}


/**
 * Custom property that holds whether an item is [MediaItem.FLAG_BROWSABLE] or
 * [MediaItem.FLAG_PLAYABLE].
 */
const val METADATA_KEY_UAMP_FLAGS = "com.example.android.uamp.media.METADATA_KEY_UAMP_FLAGS"

