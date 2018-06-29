/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package com.example.android.uamp.media.audiofocus

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.media.AudioAttributesCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * Wrapper around a [SimpleExoPlayer] simplifies playback by automatically handling
 * audio focus using [AudioFocusRequest] on Oreo+ devices, and an
 * [AudioManager.OnAudioFocusChangeListener] on previous versions.
 */
class AudioFocusExoPlayerDecorator(private val audioAttributes: AudioAttributesCompat,
                                   private val audioManager: AudioManager,
                                   private val player: SimpleExoPlayer) : ExoPlayer by player {

    /**
     * A list of listeners to control how/when state changes pass from the wrapped player to
     * the rest of the app.
     */
    private val eventListeners = mutableListOf<Player.EventListener>()

    /**
     * Similar to [Player.getPlayWhenReady], but reflects the intent to play.
     */
    private var shouldPlayWhenReady = false

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (shouldPlayWhenReady || player.playWhenReady) {
                    player.playWhenReady = true
                    player.volume = MEDIA_VOLUME_DEFAULT
                }
                shouldPlayWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (player.playWhenReady) {
                    player.volume = MEDIA_VOLUME_DUCK
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Save the current state of playback so the _intention_ to play can be properly
                // reported to the app.
                shouldPlayWhenReady = player.playWhenReady
                player.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // This will chain through to abandonAudioFocus().
                AudioFocusExoPlayerDecorator@playWhenReady = false
            }
        }
    }

    @get:RequiresApi(Build.VERSION_CODES.O)
    private val audioFocusRequest by lazy { buildFocusRequest() }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            requestAudioFocus()
        } else {
            if (shouldPlayWhenReady) {
                shouldPlayWhenReady = false
                playerEventListener.onPlayerStateChanged(false, player.playbackState)
            }
            player.playWhenReady = false
            abandonAudioFocus()
        }
    }

    /**
     * @see [Player.getPlayWhenReady]
     * @return `true` when the underlying player's `playWhenReady` is true OR when
     * it would be true, but [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT] has forced a temporary
     * pause in playback.
     */
    override fun getPlayWhenReady(): Boolean = player.playWhenReady || shouldPlayWhenReady

    override fun addListener(listener: Player.EventListener?) {
        if (listener != null && !eventListeners.contains(listener)) {
            eventListeners += listener
        }
    }

    override fun removeListener(listener: Player.EventListener?) {
        if (listener != null && eventListeners.contains(listener)) {
            eventListeners -= listener
        }
    }

    private fun requestAudioFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusOreo()
        } else {
            @Suppress("deprecation")
            audioManager.requestAudioFocus(audioFocusListener,
                    audioAttributes.legacyStreamType,
                    AudioManager.AUDIOFOCUS_GAIN)
        }

        // Call the listener whenever focus is granted - even the first time!
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            shouldPlayWhenReady = true
            audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        } else {
            Log.i(TAG, "Playback not started: Audio focus request denied")
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            abandonAudioFocusOreo()
        } else {
            @Suppress("deprecation")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusOreo(): Int = audioManager.requestAudioFocus(audioFocusRequest)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocusOreo() = audioManager.abandonAudioFocusRequest(audioFocusRequest)

    @TargetApi(Build.VERSION_CODES.O)
    private fun buildFocusRequest(): AudioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes.unwrap() as AudioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()

    /**
     * Implementation of [Player.EventListener] which passes events from [player] through,
     * with the exception of [Player.EventListener.onPlayerStateChanged].
     */
    private val playerEventListener = object : Player.EventListener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            eventListeners.forEach { it.onPlaybackParametersChanged(playbackParameters) }
        }

        override fun onSeekProcessed() {
            eventListeners.forEach { it.onSeekProcessed() }
        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?,
                                     trackSelections: TrackSelectionArray?) {
            eventListeners.forEach { it.onTracksChanged(trackGroups, trackSelections) }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            eventListeners.forEach { it.onPlayerError(error) }
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            eventListeners.forEach { it.onLoadingChanged(isLoading) }
        }

        override fun onPositionDiscontinuity(reason: Int) {
            eventListeners.forEach { it.onPositionDiscontinuity(reason) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            eventListeners.forEach { it.onRepeatModeChanged(repeatMode) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            eventListeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            eventListeners.forEach { it.onTimelineChanged(timeline, manifest, reason) }
        }

        /**
         * Handles the case where the intention is to play (so [Player.getPlayWhenReady] should
         * return `true`), but it's actually paused because the app had a temporary loss
         * of audio focus; i.e.: [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT].
         */
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            val reportPlayWhenReady = getPlayWhenReady()
            eventListeners.forEach { it.onPlayerStateChanged(reportPlayWhenReady, playbackState) }
        }
    }

    // Add the Player.EventListener wrapper (above) to the player.
    init {
        player.addListener(playerEventListener)
    }
}

private const val TAG = "AFExoPlayerDecorator"
private const val MEDIA_VOLUME_DEFAULT = 1.0f
private const val MEDIA_VOLUME_DUCK = 0.2f
