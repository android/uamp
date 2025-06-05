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
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.uamp.automotive.databinding.ActivityAutomotiveMainBinding

/**
 * Main Activity for Android Automotive that provides a modern music browsing experience
 * similar to Spotify, YouTube Music, and Apple Music.
 */
class AutomotiveMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutomotiveMainBinding
    private lateinit var browseMusicAdapter: AutomotiveBrowseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityAutomotiveMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
    }

    private fun setupUI() {
        // Set up the toolbar for automotive
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Mixtape"
            subtitle = "Music Player"
        }

        // Setup basic playback controls (these would be connected to the service in a full implementation)
        binding.playPauseButton.setOnClickListener {
            // TODO: Connect to media service
        }

        binding.previousButton.setOnClickListener {
            // TODO: Connect to media service
        }

        binding.nextButton.setOnClickListener {
            // TODO: Connect to media service
        }

        binding.shuffleButton.setOnClickListener {
            // TODO: Connect to media service
        }

        binding.repeatButton.setOnClickListener {
            // TODO: Connect to media service
        }
    }

    private fun setupRecyclerView() {
        browseMusicAdapter = AutomotiveBrowseAdapter { mediaItem ->
            // Handle item click - for now just log it
            android.util.Log.d("AutomotiveMainActivity", "Selected: ${mediaItem.mediaMetadata.title}")
        }

        binding.browseRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AutomotiveMainActivity)
            adapter = browseMusicAdapter
            
            // Add item decoration for better spacing
            addItemDecoration(AutomotiveItemDecoration(16))
        }

        // For now, show empty state
        binding.emptyStateText.visibility = View.VISIBLE
        binding.browseRecyclerView.visibility = View.GONE
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

    private fun updateNowPlayingDisplay(mediaItem: MediaItem?) {
        mediaItem?.let {
            binding.songTitle.text = it.mediaMetadata.title ?: "Unknown Title"
            binding.artistName.text = it.mediaMetadata.artist ?: "Unknown Artist"
            
            // Show now playing section
            binding.nowPlayingSection.visibility = View.VISIBLE
        } ?: run {
            binding.nowPlayingSection.visibility = View.GONE
        }
    }
} 