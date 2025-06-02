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

package com.example.android.uamp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.example.android.uamp.MainActivity
import com.example.android.uamp.R
import com.example.android.uamp.databinding.FragmentNowplayingBinding
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel

/**
 * A fragment representing a now playing screen.
 */
class NowPlayingFragment : Fragment() {

    companion object {
        fun newInstance() = NowPlayingFragment()
    }

    private val viewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    private var _binding: FragmentNowplayingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowplayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Subscribe to the current playing metadata
        viewModel.mediaMetadata.observe(viewLifecycleOwner) { metadata ->
            updateUI(metadata)
        }

        // Subscribe to the playback state
        viewModel.playbackState.observe(viewLifecycleOwner) { state: Int ->
            binding.mediaButton.isEnabled = state != Player.STATE_IDLE
        }

        // Set up click listeners
        binding.mediaButton.setOnClickListener {
            viewModel.playPause()
        }

        binding.nextButton.setOnClickListener {
            viewModel.skipNext()
        }

        binding.previousButton.setOnClickListener {
            viewModel.skipPrevious()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUI(metadata: NowPlayingFragmentViewModel.NowPlayingMetadata?) {
        // Update UI with metadata
        metadata?.let {
            binding.title.text = it.title
            binding.subtitle.text = it.subtitle
            // Load album art
            Glide.with(this)
                .load(it.albumArtUri)
                .placeholder(R.drawable.default_art)
                .into(binding.albumArt)
        }
    }
}
