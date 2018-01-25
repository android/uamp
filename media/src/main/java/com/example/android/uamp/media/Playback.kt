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

import android.app.Activity
import android.app.Service
import android.content.Context
import android.media.AudioManager
import android.support.v4.media.AudioAttributesCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.android.uamp.media.audiofocus.AudioFocusExoPlayerDecorator
import com.example.android.uamp.media.extensions.mediaUri
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

/**
 * Class to handle local playback with a [SimpleExoPlayer].
 */
class Playback(val context: Context, private val stateUpdates: (Int) -> Unit) {
    private val nothingPlaying = MediaMetadataCompat.Builder()
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

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributesCompat.Builder()
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .build()

    // Use lazy initialization of the player so it doesn't take up any resources until something
    // is about to be played.
    private val exoPlayer: ExoPlayer =
            AudioFocusExoPlayerDecorator(audioAttributes,
                    audioManager,
                    ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(context),
                            DefaultTrackSelector(),
                            DefaultLoadControl())
                            .apply {
                                addListener(playerListener)
                            })

    /**
     * ExoPlayer returns [STATE_IDLE] in two different situations: when the player has just been
     * created, and after calling [ExoPlayer.stop]. In order to differentiate between these two
     * keep track of which state the player is in.
     */
    var idleIsStopped = false

    fun play(mediaMetadata: MediaMetadataCompat) {
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "uamp.next"), null)
        // The MediaSource represents the media to be played.
        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaMetadata.mediaUri)

        // Prepares media to play (happens on background thread) and triggers
        // {@code onPlayerStateChanged} callback when the stream is ready to play.
        exoPlayer.prepare(mediaSource)

        // Update currently playing.
        currentlyPlaying = mediaMetadata

        resume()
    }

    fun resume() {
        exoPlayer.playWhenReady = true
        idleIsStopped = true
    }

    fun pause() {
        exoPlayer.playWhenReady = false
    }

    fun stop() {
        /**
         * Calling [ExoPlayer.stop] not only stops playback, but it also releases codecs and the
         * media source(s) associated with the player. The player, however, can be reused later.
         * [ExoPlayer.release] is only necessary when it's certain the player will not be used
         * again, such as in [Service.onDestroy] or [Activity.onDestroy]
         */
        exoPlayer.stop()
        exoPlayer.seekTo(0)

        currentlyPlaying = nothingPlaying
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
    }

    fun release() {
        stop()
        exoPlayer.release()
    }

    private fun updatePlaybackState(newPlaybackState: Int?) {
        stateUpdates(newPlaybackState ?: playerState)
    }

    private fun playerStateToCompatState(playWhenReady: Boolean, playbackState: Int) =
            when (playbackState) {
                STATE_IDLE ->
                    if (idleIsStopped) PlaybackStateCompat.STATE_STOPPED
                    else PlaybackStateCompat.STATE_NONE
                STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                STATE_READY -> {
                    if (playWhenReady) {
                        PlaybackStateCompat.STATE_PLAYING
                    } else {
                        PlaybackStateCompat.STATE_PAUSED
                    }
                }
                STATE_ENDED -> PlaybackStateCompat.STATE_PAUSED
                else -> PlaybackStateCompat.STATE_ERROR
            }

    inner class PlayerListener : Player.EventListener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) = Unit

        override fun onSeekProcessed() = Unit

        override fun onTracksChanged(trackGroups: TrackGroupArray?,
                                     trackSelections: TrackSelectionArray?) = Unit

        override fun onPlayerError(error: ExoPlaybackException?) = Unit

        override fun onLoadingChanged(isLoading: Boolean) = Unit

        override fun onPositionDiscontinuity(reason: Int) = Unit

        override fun onRepeatModeChanged(repeatMode: Int) = Unit

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = Unit

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) = Unit

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlaybackState(playerStateToCompatState(playWhenReady, playbackState))
        }
    }
}