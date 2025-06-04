/*
 * Copyright 2019 Google Inc. All rights reserved.
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

package com.example.android.uamp.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.session.MediaBrowser
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.example.android.uamp.R
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.fragments.NowPlayingFragment

/**
 * [ViewModel] for [NowPlayingFragment] which displays the album art in full size.
 */
class NowPlayingFragmentViewModel(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    /**
     * Utility class used to represent the metadata necessary to display the
     * media item currently being played.
     */
    data class NowPlayingMetadata(
        val id: String,
        val albumArtUri: Uri,
        val title: String?,
        val subtitle: String?,
        val duration: String
    ) {

        companion object {
            /**
             * Utility method to convert milliseconds to a display of minutes and seconds
             */
            fun timestampToMSS(position: Long): String {
                val totalSeconds = Math.floor(position / 1E3).toInt()
                val minutes = totalSeconds / 60
                val remainingSeconds = totalSeconds - (minutes * 60)
                return String.format("%d:%02d", minutes, remainingSeconds)
            }
        }
    }

    private val _mediaMetadata = MutableLiveData<NowPlayingMetadata>()
    val mediaMetadata: LiveData<NowPlayingMetadata> = _mediaMetadata

    private val _playbackState = MutableLiveData<Int>()
    val playbackState: LiveData<Int> = _playbackState

    private val _mediaButtonRes = MutableLiveData<Int>()
    val mediaButtonRes: LiveData<Int> = _mediaButtonRes

    val mediaPosition: LiveData<Long> = musicServiceConnection.currentPosition

    val mediaMetadataText: LiveData<String> = musicServiceConnection.nowPlaying.map { metadata ->
        metadata?.title?.toString() ?: ""
    }

    val repeatMode: LiveData<Int> = musicServiceConnection.repeatMode
    val shuffleMode: LiveData<Boolean> = musicServiceConnection.shuffleMode

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())

    private val playbackStateObserver = Observer<Int> { state ->
        _playbackState.postValue(state ?: Player.STATE_IDLE)
        updateState(state)
    }

    private val mediaMetadataObserver = Observer<MediaMetadata> { metadata ->
        updateMetadata(metadata)
    }

    private val isPlayingObserver = Observer<Boolean> { 
        updatePlayPauseButton()
    }

    init {
        musicServiceConnection.playbackState.observeForever(playbackStateObserver)
        musicServiceConnection.nowPlaying.observeForever(mediaMetadataObserver)
        musicServiceConnection.isPlaying.observeForever(isPlayingObserver)
    }

    /**
     * Play or pause the current media item
     */
    fun playPause() {
        if (musicServiceConnection.isPlaying.value == true) {
            musicServiceConnection.pause()
        } else {
            musicServiceConnection.play()
        }
    }

    /**
     * Skip to the next track
     */
    fun skipNext() {
        musicServiceConnection.skipNext()
    }

    /**
     * Skip to the previous track
     */
    fun skipPrevious() {
        musicServiceConnection.skipPrevious()
    }

    /**
     * Seek to a specific position
     */
    fun seekTo(position: Long) {
        musicServiceConnection.seekTo(position)
    }

    /**
     * Toggle repeat mode between off, one, and all
     */
    fun toggleRepeatMode() {
        val currentMode = repeatMode.value ?: Player.REPEAT_MODE_OFF
        val nextMode = when (currentMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        musicServiceConnection.setRepeatMode(nextMode)
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffleMode() {
        val currentShuffle = shuffleMode.value ?: false
        musicServiceConnection.setShuffleMode(!currentShuffle)
    }

    /**
     * Internal function that recursively calls itself every [POSITION_UPDATE_INTERVAL_MILLIS] ms
     * to check the current playback position and updates the corresponding LiveData object when it
     * has changed.
     */
    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        val currPosition = musicServiceConnection.currentPosition.value ?: 0L
        if (mediaPosition.value != currPosition)
            _playbackState.postValue(playbackState.value)
        if (updatePosition)
            checkPlaybackPosition()
    }, POSITION_UPDATE_INTERVAL_MILLIS)

    /**
     * Since we use [LiveData.observeForever] above (in [musicServiceConnection]), we want
     * to call [LiveData.removeObserver] here to prevent leaking resources when the [ViewModel]
     * is not longer in use.
     *
     * For more details, see the kdoc on [musicServiceConnection] above.
     */
    override fun onCleared() {
        super.onCleared()

        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)
        musicServiceConnection.isPlaying.removeObserver(isPlayingObserver)

        // Stop updating the position
        updatePosition = false
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        metadata?.let {
            val title = it.title?.toString()
            if (title != null) {
                val duration = musicServiceConnection.duration.value ?: 0L
                val nowPlayingMetadata = NowPlayingMetadata(
                    title, // Use title as ID since mediaId doesn't exist
                    it.artworkUri ?: Uri.EMPTY,
                    it.title?.toString()?.trim(),
                    it.artist?.toString()?.trim(),
                    NowPlayingMetadata.timestampToMSS(duration)
                )
                _mediaMetadata.postValue(nowPlayingMetadata)
            }
        }

        // Update the media button when metadata changes
        updatePlayPauseButton()
    }

    private fun updateState(state: Int?) {
        // Update the media button when playback state changes
        updatePlayPauseButton()
    }

    private fun updatePlayPauseButton() {
        _mediaButtonRes.postValue(
            when {
                musicServiceConnection.isPlaying.value == true -> R.drawable.ic_pause
                else -> R.drawable.ic_play_arrow_black_24dp
            }
        )
    }

    class Factory(
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NowPlayingFragmentViewModel(musicServiceConnection) as T
        }
    }
}

private const val TAG = "NowPlayingFragmentVM"
private const val POSITION_UPDATE_INTERVAL_MILLIS = 100L
