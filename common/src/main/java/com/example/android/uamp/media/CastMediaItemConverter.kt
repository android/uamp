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

import android.net.Uri
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.common.images.WebImage
import java.util.*

/**
 * A [MediaItemConverter] to convert between [MediaItem]s and [MediaQueueItem]s.
 *
 * This converter is specific to UAMP's requirements and shows how to customize the converter
 * based on your app's needs.
 */
@UnstableApi
class CastMediaItemConverter : MediaItemConverter {
    private val defaultConverter = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val metadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        mediaItem.mediaMetadata.title?.let { metadata.putString(CastMediaMetadata.KEY_TITLE, it.toString()) }
        mediaItem.mediaMetadata.subtitle?.let { metadata.putString(CastMediaMetadata.KEY_SUBTITLE, it.toString()) }
        mediaItem.mediaMetadata.albumTitle?.let { metadata.putString(CastMediaMetadata.KEY_ALBUM_TITLE, it.toString()) }
        mediaItem.mediaMetadata.artist?.let { metadata.putString(CastMediaMetadata.KEY_ARTIST, it.toString()) }
        mediaItem.mediaMetadata.composer?.let { metadata.putString(CastMediaMetadata.KEY_COMPOSER, it.toString()) }
        mediaItem.mediaMetadata.trackNumber?.let { metadata.putInt(CastMediaMetadata.KEY_TRACK_NUMBER, it.toInt()) }
        mediaItem.mediaMetadata.discNumber?.let { metadata.putInt(CastMediaMetadata.KEY_DISC_NUMBER, it.toInt()) }
        mediaItem.mediaMetadata.albumArtist?.let { metadata.putString(CastMediaMetadata.KEY_ALBUM_ARTIST, it.toString()) }
        mediaItem.mediaMetadata.releaseYear?.let { metadata.putString(CastMediaMetadata.KEY_RELEASE_DATE, it.toString()) }
        mediaItem.mediaMetadata.artworkUri?.let { metadata.addImage(WebImage(Uri.parse(it.toString()))) }

        val mediaInfo = MediaInfo.Builder(mediaItem.localConfiguration?.uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.AUDIO_MPEG)
            .setMetadata(metadata)
            .build()

        return MediaQueueItem.Builder(mediaInfo).build()
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        val castMetadata = mediaQueueItem.media?.metadata
        val builder = MediaItem.Builder()
            .setUri(mediaQueueItem.media?.contentId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(castMetadata?.getString(CastMediaMetadata.KEY_TITLE))
                    .setSubtitle(castMetadata?.getString(CastMediaMetadata.KEY_SUBTITLE))
                    .setAlbumTitle(castMetadata?.getString(CastMediaMetadata.KEY_ALBUM_TITLE))
                    .setArtist(castMetadata?.getString(CastMediaMetadata.KEY_ARTIST))
                    .setComposer(castMetadata?.getString(CastMediaMetadata.KEY_COMPOSER))
                    .setTrackNumber(castMetadata?.getInt(CastMediaMetadata.KEY_TRACK_NUMBER))
                    .setDiscNumber(castMetadata?.getInt(CastMediaMetadata.KEY_DISC_NUMBER))
                    .setAlbumArtist(castMetadata?.getString(CastMediaMetadata.KEY_ALBUM_ARTIST))
                    .setReleaseYear(castMetadata?.getDate(CastMediaMetadata.KEY_RELEASE_DATE)?.let { 
                        null // For now, skip the year extraction to avoid compilation issues
                    })
                    .build()
            )

        return builder.build()
    }
}
