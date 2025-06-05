/*
 * Copyright 2024 Mixtape Player
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

package com.example.android.uamp.automotive

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.R
import com.example.android.uamp.databinding.ActivityAutomotiveMainBinding
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel

/**
 * Main Activity for Android Auto projection that provides a modern music browsing experience
 * similar to Spotify, YouTube Music, and Apple Music.
 */
class AutomotiveMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutomotiveMainBinding
    
    private val mainViewModel: MainActivityViewModel by viewModels {
        InjectorUtils.provideMainActivityViewModel(this)
    }
    
    private val nowPlayingViewModel: NowPlayingFragmentViewModel by viewModels {
        InjectorUtils.provideNowPlayingFragmentViewModel(this)
    }
    
    private lateinit var browseMusicAdapter: AutomotiveBrowseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAutomotiveMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupViewModel()
        setupRecyclerView()
    }

    private fun setupUI() {
        // Set up the toolbar for automotive
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Mixtape"
            subtitle = "Music Player"
        }

        // Setup playback controls using viewmodel's public methods
        binding.playPauseButton.setOnClickListener {
            nowPlayingViewModel.playPause()
        }

        binding.previousButton.setOnClickListener {
            nowPlayingViewModel.skipPrevious()
        }

        binding.nextButton.setOnClickListener {
            nowPlayingViewModel.skipNext()
        }

        binding.shuffleButton.setOnClickListener {
            nowPlayingViewModel.toggleShuffleMode()
        }

        binding.repeatButton.setOnClickListener {
            nowPlayingViewModel.toggleRepeatMode()
        }
    }

    private fun setupViewModel() {
        // Observe playback state through viewmodel's public properties
        mainViewModel.musicServiceConnection.isPlaying.observe(this) { isPlaying ->
            updatePlayPauseButton(isPlaying)
        }

        // Observe currently playing media item
        mainViewModel.musicServiceConnection.nowPlaying.observe(this) { mediaMetadata ->
            updateNowPlayingDisplay(mediaMetadata)
        }

        // Observe repeat mode
        nowPlayingViewModel.repeatMode.observe(this) { repeatMode ->
            updateRepeatButton(repeatMode)
        }

        // Observe shuffle mode
        nowPlayingViewModel.shuffleMode.observe(this) { shuffleEnabled ->
            updateShuffleButton(shuffleEnabled)
        }

        // Observe media items for browsing
        mainViewModel.mediaItems.observe(this) { mediaItems ->
            val mediaItemList = mediaItems.map { mediaItemData ->
                mediaItemData.toMediaItem()
            }
            browseMusicAdapter.submitList(mediaItemList)
            
            // Show/hide empty state
            if (mediaItems.isEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.browseRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.browseRecyclerView.visibility = View.VISIBLE
            }
        }

        // Observe network errors
        mainViewModel.networkError.observe(this) { errorThrowable ->
            errorThrowable?.let {
                // Show error message to user
                binding.emptyStateText.text = "Network error occurred"
                binding.emptyStateText.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        browseMusicAdapter = AutomotiveBrowseAdapter { mediaItem ->
            // Handle item click - play the selected media item
            Log.d(TAG, "Playing media item: ${mediaItem.mediaId}")
            mainViewModel.musicServiceConnection.playMedia(mediaItem.mediaId)
        }

        binding.browseRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AutomotiveMainActivity)
            adapter = browseMusicAdapter
            
            // Add item decoration for better spacing
            addItemDecoration(AutomotiveItemDecoration(
                resources.getDimensionPixelSize(R.dimen.automotive_spacing_medium)
            ))
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.playPauseButton.setImageResource(
            if (isPlaying) {
                R.drawable.ic_pause_black_24dp
            } else {
                R.drawable.ic_play_arrow_black_24dp
            }
        )
    }

    private fun updateRepeatButton(repeatMode: Int) {
        val (iconRes, alpha) = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> R.drawable.ic_repeat_black_24dp to 0.5f
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_black_24dp to 1.0f
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_black_24dp to 1.0f
            else -> R.drawable.ic_repeat_black_24dp to 0.5f
        }
        
        binding.repeatButton.setImageResource(iconRes)
        binding.repeatButton.alpha = alpha
    }

    private fun updateShuffleButton(shuffleEnabled: Boolean) {
        binding.shuffleButton.alpha = if (shuffleEnabled) 1.0f else 0.5f
    }

    private fun updateNowPlayingDisplay(mediaMetadata: androidx.media3.common.MediaMetadata) {
        binding.songTitle.text = mediaMetadata.title ?: "Unknown Title"
        binding.artistName.text = mediaMetadata.artist ?: "Unknown Artist"
        
        // Show now playing section
        binding.nowPlayingSection.visibility = View.VISIBLE
    }

    // Extension function to convert MediaItemData to MediaItem
    private fun MediaItemData.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(this.mediaId)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(this.title)
                    .setArtist(this.subtitle)
                    .setAlbumTitle(this.subtitle) // Using subtitle as album for now
                    .setArtworkUri(this.albumArtUri)
                    .build()
            )
            .build()
    }

    companion object {
        private const val TAG = "AutomotiveMainActivity"
    }
} 