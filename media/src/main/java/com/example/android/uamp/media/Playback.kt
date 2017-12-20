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

package com.example.android.uamp.media

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.android.uamp.media.extensions.mediaUri
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

/**
 * Class to handle local playback with a [SimpleExoPlayer].
 */
class Playback(val context: Context, val stateUpdates: (Int) -> Unit) {
    val nothingPlaying = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "EMPTY_ID")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
            .build()

    // TODO: Support a queue.
    var currentlyPlaying: MediaMetadataCompat = nothingPlaying

    @PlaybackStateCompat.State
    val playerState: Int
        get() = playerStateToCompatState(exoPlayer.playWhenReady, exoPlayer.playbackState)
    val playerPosition get() = exoPlayer.currentPosition
    val playerSpeed get() = exoPlayer.playbackParameters.speed

    private val playerListener = PlayerListener()
    private val exoPlayer: SimpleExoPlayer =
            ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(context),
                    DefaultTrackSelector(),
                    DefaultLoadControl())
    private val playbackStateBuilder = PlaybackStateCompat.Builder()

    init {
        exoPlayer.addListener(playerListener)
    }

    fun play(mediaMetadata: MediaMetadataCompat) {
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "uamp.next"), null)
        // Produces Extractor instances for parsing the media data.
        val extractorsFactory = DefaultExtractorsFactory()
        // The MediaSource represents the media to be played.
        val mediaSource =
                ExtractorMediaSource(mediaMetadata.mediaUri, dataSourceFactory,
                        extractorsFactory, null, null)

        // Prepares media to play (happens on background thread) and triggers
        // {@code onPlayerStateChanged} callback when the stream is ready to play.
        exoPlayer.prepare(mediaSource)

        // Update currently playing.
        currentlyPlaying = mediaMetadata

        // TODO: Should actually request audio focus and only play once we receive it.
        exoPlayer.playWhenReady = true
    }

    fun resume() {
        exoPlayer.playWhenReady = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    private fun updatePlaybackState(newPlaybackState: Int?) {
        val updateState = newPlaybackState ?: playerState
        stateUpdates(updateState)
    }

    private fun playerStateToCompatState(playWhenReady: Boolean, playbackState: Int): Int {
        return when (exoPlayer.playbackState) {
            STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            STATE_READY -> {
                if (exoPlayer.playWhenReady) {
                    PlaybackStateCompat.STATE_PLAYING
                } else {
                    PlaybackStateCompat.STATE_PAUSED
                }
            }
            STATE_ENDED -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_ERROR
        }
    }

    inner class PlayerListener : Player.EventListener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onSeekProcessed() {
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?,
                                     trackSelections: TrackSelectionArray?) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
        }

        override fun onLoadingChanged(isLoading: Boolean) {
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlaybackState(playerStateToCompatState(playWhenReady, playbackState))
        }
    }
}