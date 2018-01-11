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
import android.media.AudioManager
import android.support.v4.media.AudioAttributesCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.android.uamp.media.audiofocus.AudioFocusAwarePlayer
import com.example.android.uamp.media.audiofocus.AudioFocusHelper
import com.example.android.uamp.media.audiofocus.AudioFocusRequestCompat
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

    // Use lazy initialization of the player so it doesn't take up any resources until something
    // is about to be played.
    private val exoPlayer: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(context),
                DefaultTrackSelector(),
                DefaultLoadControl())
                .apply {
                    addListener(playerListener)
                }
    }

    private val audioFocusHelper = AudioFocusHelper(context)
    private var audioFocusRequest: AudioFocusRequestCompat? = null

    private val exoPlayerWrapper = object : AudioFocusAwarePlayer {
        override fun isPlaying(): Boolean {
            return exoPlayer.playWhenReady
        }

        override fun play() {
            exoPlayer.playWhenReady = true
        }

        override fun pause() {
            exoPlayer.playWhenReady = false
        }

        override fun stop() {
            this@Playback.stop()
        }

        override fun setVolume(volume: Float) {
            exoPlayer.volume = volume
        }
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

        resume()
    }

    fun resume() {
        if (audioFocusRequest == null) {
            val listener = audioFocusHelper.createListenerForPlayer(exoPlayerWrapper)
            val audioAttributes = AudioAttributesCompat.Builder()
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .build()

            audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(listener)
                    .setAudioAttributes(audioAttributes)
                    .build()
        }

        if (audioFocusHelper.requestAudioFocus(audioFocusRequest)) {
            exoPlayer.playWhenReady = true
        }
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        audioFocusRequest?.let {
            audioFocusHelper.abandonAudioFocus(it)
        }
    }

    fun stop() {
        pause()
        exoPlayer.release()

        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
    }

    private fun updatePlaybackState(newPlaybackState: Int?) {
        stateUpdates(newPlaybackState ?: playerState)
    }

    private fun playerStateToCompatState(playWhenReady: Boolean, playbackState: Int) =
            when (playbackState) {
                STATE_IDLE -> PlaybackStateCompat.STATE_NONE
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