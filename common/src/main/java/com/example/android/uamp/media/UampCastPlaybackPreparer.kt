package com.example.android.uamp.media

import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.toMediaQueueItem
import com.example.android.uamp.media.extensions.trackNumber
import com.example.android.uamp.media.library.MusicSource
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.gms.cast.MediaQueueItem

class UampCastPlaybackPreparer(
        private val musicSource: MusicSource,
        private val castPlayer: CastPlayer
) : MediaSessionConnector.PlaybackPreparer {
    override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

    /**
     * Handles callbacks to both [MediaSessionCompat.Callback.onPrepareFromMediaId]
     * *AND* [MediaSessionCompat.Callback.onPlayFromMediaId] when using [MediaSessionConnector].
     * This is done with the expectation that "play" is just "prepare" + "play".
     */
    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        musicSource.whenReady {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.id == mediaId
            }
            if (itemToPlay == null) {
                Log.w(TAG, "Content not found: MediaID=$mediaId")

                // TODO: Notify caller of the error.
            } else {

            }
        }
    }

    override fun onPrepare(playWhenReady: Boolean) = Unit

    /**
     * Do not support callbacks to both [MediaSessionCompat.Callback.onPrepareFromSearch]
     * *AND* [MediaSessionCompat.Callback.onPlayFromSearch] when using [MediaSessionConnector]
     * with cast.
     */
    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?)  = Unit

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
    ) = false



}

private const val TAG = "CastPlaybackPreparer"