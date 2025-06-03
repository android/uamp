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

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
        fun newInstance(isFullScreen: Boolean = false) = NowPlayingFragment().apply {
            arguments = Bundle().apply {
                putBoolean("isFullScreen", isFullScreen)
            }
        }
    }

    private val viewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    private var _binding: FragmentNowplayingBinding? = null
    private val binding get() = _binding!!
    
    private val isFullScreen: Boolean
        get() = arguments?.getBoolean("isFullScreen", false) ?: false

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

        // Subscribe to the media button resource to update play/pause icon
        viewModel.mediaButtonRes.observe(viewLifecycleOwner) { resId ->
            binding.mediaButton.setImageResource(resId)
        }

        // Subscribe to the playback state
        viewModel.playbackState.observe(viewLifecycleOwner) { state: Int ->
            binding.mediaButton.isEnabled = state != Player.STATE_IDLE
            // Show/hide the fragment based on playback state
            binding.root.visibility = if (state != Player.STATE_IDLE) View.VISIBLE else View.GONE
        }

        // Show/hide collapse button based on mode
        binding.collapseButton.visibility = if (isFullScreen) View.VISIBLE else View.GONE

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

        // Collapse button click listener
        binding.collapseButton.setOnClickListener {
            collapseFromFullScreen()
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

        // Click on the entire mini player to expand to full screen (only in mini mode)
        if (!isFullScreen) {
            binding.root.setOnClickListener {
                expandToFullScreen()
            }
        } else {
            // In full-screen mode, block all touch events from reaching underlying fragments
            binding.root.setOnTouchListener { _, _ ->
                true // Consume all touch events to prevent them from reaching fragments underneath
            }
        }
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
            binding.duration.text = it.duration
            
            // Load album art with dynamic color extraction
            Glide.with(this)
                .asBitmap()
                .load(it.albumArtUri)
                .placeholder(R.drawable.default_art)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Check if fragment is still valid before accessing binding
                        if (_binding == null) return
                        
                        // Set the album art
                        binding.albumArt.setImageBitmap(resource)
                        
                        // Generate color palette from the bitmap
                        Palette.from(resource).generate { palette ->
                            // Check again in case fragment was destroyed during palette generation
                            if (_binding == null) return@generate
                            
                            palette?.let { extractedPalette ->
                                applyPaletteColors(extractedPalette)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Check if fragment is still valid before accessing binding
                        if (_binding == null) return
                        
                        // Set placeholder and use default white colors
                        binding.albumArt.setImageDrawable(placeholder)
                        applyDefaultColors()
                    }
                })
        }
    }

    private fun applyPaletteColors(palette: Palette) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        // Extract colors with fallbacks
        val vibrantColor = palette.vibrantSwatch?.rgb
        val darkVibrantColor = palette.darkVibrantSwatch?.rgb
        val lightVibrantColor = palette.lightVibrantSwatch?.rgb
        val mutedColor = palette.mutedSwatch?.rgb
        val darkMutedColor = palette.darkMutedSwatch?.rgb

        // Choose the best color for text and controls
        val primaryColor = vibrantColor 
            ?: lightVibrantColor 
            ?: mutedColor 
            ?: ContextCompat.getColor(requireContext(), android.R.color.white)

        val secondaryColor = darkVibrantColor 
            ?: darkMutedColor 
            ?: vibrantColor 
            ?: ContextCompat.getColor(requireContext(), android.R.color.white)

        // Apply colors to UI elements
        applyColorsToUI(primaryColor, secondaryColor)
    }

    private fun applyDefaultColors() {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        applyColorsToUI(whiteColor, whiteColor)
    }

    private fun applyColorsToUI(primaryColor: Int, secondaryColor: Int) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        // Apply colors to text
        binding.title.setTextColor(primaryColor)
        binding.subtitle.setTextColor(secondaryColor)
        binding.duration.setTextColor(secondaryColor)

        // Apply colors to control buttons using modern ColorFilter approach
        binding.mediaButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)
        binding.previousButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(secondaryColor, android.graphics.BlendMode.SRC_IN)
        binding.nextButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(secondaryColor, android.graphics.BlendMode.SRC_IN)
        binding.collapseButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(primaryColor, android.graphics.BlendMode.SRC_IN)

        // Apply colors to seek bar
        binding.seekBar.progressTintList = android.content.res.ColorStateList.valueOf(primaryColor)
        binding.seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(primaryColor)
        binding.seekBar.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.argb(100, 
                android.graphics.Color.red(secondaryColor),
                android.graphics.Color.green(secondaryColor),
                android.graphics.Color.blue(secondaryColor)
            )
        )
    }

    private fun expandToFullScreen() {
        // Create a new instance for full screen - this will use the same layout
        // but without height constraints, showing the full album art
        val fullScreenFragment = NowPlayingFragment.newInstance(true)
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fullScreenFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun collapseFromFullScreen() {
        // Navigate back to the previous fragment (MediaItemFragment)
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            // Fallback: if no back stack, finish the activity
            activity?.finish()
        }
    }
}
