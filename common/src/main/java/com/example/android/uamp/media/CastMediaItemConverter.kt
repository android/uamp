package com.example.android.uamp.media

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import com.example.android.uamp.media.library.JsonSource
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.cast.DefaultMediaItemConverter
import com.google.android.exoplayer2.ext.cast.MediaItemConverter
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.common.images.WebImage

/**
 * A [MediaItemConverter] to convert from a [MediaItem] to a Cast [MediaQueueItem].
 *
 * It adds all audio specific metadata properties and creates a Cast metadata object of type
 * [MediaMetadata.MEDIA_TYPE_MUSIC_TRACK].
 *
 * To create an artwork for Cast we can't use the standard [MediaItem#mediaMetadata#artworkUri]
 * because UAMP uses a content provider to serve cached bitmaps. The URIs starting with `content://`
 * are useless on a Cast device, so we need to use the original HTTP URI that the [JsonSource]
 * stores in the metadata extra with key `JsonSource.ORIGINAL_ARTWORK_URI_KEY`.
 */
internal class CastMediaItemConverter : MediaItemConverter {

    private val defaultMediaItemConverter = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val castMediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        mediaItem.mediaMetadata.title?.let {
            castMediaMetadata.putString(MediaMetadata.KEY_TITLE, it.toString() )
        }
        mediaItem.mediaMetadata.subtitle?.let {
            castMediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, it.toString())
        }
        mediaItem.mediaMetadata.artist?.let {
            castMediaMetadata.putString(MediaMetadata.KEY_ARTIST, it.toString())
        }
        mediaItem.mediaMetadata.albumTitle?.let {
            castMediaMetadata.putString(MediaMetadata.KEY_ALBUM_TITLE, it.toString())
        }
        mediaItem.mediaMetadata.albumArtist?.let {
            castMediaMetadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, it.toString())
        }
        mediaItem.mediaMetadata.composer?.let {
            castMediaMetadata.putString(MediaMetadata.KEY_COMPOSER, it.toString())
        }
        mediaItem.mediaMetadata.trackNumber?.let{
            castMediaMetadata.putInt(MediaMetadata.KEY_TRACK_NUMBER, it)
        }
        mediaItem.mediaMetadata.discNumber?.let {
            castMediaMetadata.putInt(MediaMetadata.KEY_DISC_NUMBER, it)
        }
        val mediaInfo = MediaInfo.Builder(mediaItem.localConfiguration!!.uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.AUDIO_MPEG)
        mediaItem.localConfiguration?.let {
            mediaInfo.setContentUrl(it.uri.toString())
        }
        mediaItem.mediaMetadata.extras?.let { bundle ->
            // Use the original artwork URI for Cast.
            bundle.getString(JsonSource.ORIGINAL_ARTWORK_URI_KEY)?.let {
                castMediaMetadata.addImage(WebImage(Uri.parse(it)))
            }
            mediaInfo.setStreamDuration(
                bundle.getLong(MediaMetadataCompat.METADATA_KEY_DURATION,0))
        }
        mediaInfo.setMetadata(castMediaMetadata)
        return MediaQueueItem.Builder(mediaInfo.build()).build()
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        return defaultMediaItemConverter.toMediaItem(mediaQueueItem)
    }
}
