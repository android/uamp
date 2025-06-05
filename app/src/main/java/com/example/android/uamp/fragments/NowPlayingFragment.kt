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

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import kotlin.math.abs

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
        
        // Gesture thresholds
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        
        // Auto-hide settings
        private const val AUTO_HIDE_DELAY_MS = 5000L // 5 seconds
        private const val CONTROLS_HIDE_DELAY_MS = 5000L // 5 seconds for controls
    }

    private val viewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    private val mainActivityViewModel by lazy {
        (activity as? MainActivity)?.viewModel
    }

    private var _binding: FragmentNowplayingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var gestureDetector: GestureDetector
    
    // Auto-hide functionality
    private val hideHandler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null
    
    // Controls auto-hide functionality (for full-screen mode)
    private val controlsHideHandler = Handler(Looper.getMainLooper())
    private var controlsHideRunnable: Runnable? = null
    
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

        // Initialize gesture detector
        setupGestureDetector()

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
            val shouldShow = state != Player.STATE_IDLE
            binding.root.visibility = if (shouldShow) View.VISIBLE else View.GONE
            
            // Start auto-hide timer when mini player becomes visible (both contexts)
            if (shouldShow && !isFullScreen) {
                // Add a small delay to ensure the view is fully visible before starting timer
                Handler(Looper.getMainLooper()).postDelayed({
                    if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE) {
                        resetAutoHideTimer()
                    }
                }, 100) // 100ms delay
            } else if (!shouldShow) {
                cancelAutoHideTimer()
            }
        }

        // Subscribe to repeat mode changes
        viewModel.repeatMode.observe(viewLifecycleOwner) { repeatMode ->
            updateRepeatButton(repeatMode)
        }

        // Subscribe to shuffle mode changes
        viewModel.shuffleMode.observe(viewLifecycleOwner) { shuffleEnabled ->
            updateShuffleButton(shuffleEnabled)
        }

        // Show/hide collapse button based on mode
        binding.collapseButton.visibility = if (isFullScreen) View.VISIBLE else View.GONE

        // Set up click listeners with interaction tracking
        binding.mediaButton.setOnClickListener {
            onUserInteraction()
            viewModel.playPause()
        }

        binding.nextButton.setOnClickListener {
            onUserInteraction()
            viewModel.skipNext()
        }

        binding.previousButton.setOnClickListener {
            onUserInteraction()
            viewModel.skipPrevious()
        }

        // Collapse button click listener
        binding.collapseButton.setOnClickListener {
            if (isFullScreen) {
                cancelControlsHideTimer() // Cancel controls timer when collapsing
            }
            collapseFromFullScreen()
        }

        // Repeat button click listener
        binding.repeatButton.setOnClickListener {
            onUserInteraction()
            viewModel.toggleRepeatMode()
        }

        // Shuffle button click listener
        binding.shuffleButton.setOnClickListener {
            onUserInteraction()
            viewModel.toggleShuffleMode()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onUserInteraction()
                    viewModel.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                onUserInteraction()
            }
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                // Reset controls timer when user finishes seeking
                if (isFullScreen) {
                    resetControlsHideTimer()
                }
            }
        })

        // Set up touch handling with gesture detection
        setupTouchHandling()
        
        // Start auto-hide timer if mini player is already visible when fragment is created
        if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            // Add a small delay to ensure everything is properly set up
            Handler(Looper.getMainLooper()).postDelayed({
                if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE) {
                    resetAutoHideTimer()
                }
            }, 200) // 200ms delay for initial setup
        }
        
        // Start controls auto-hide timer if in full-screen mode
        if (isFullScreen) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (_binding != null && isFullScreen) {
                    resetControlsHideTimer()
                }
            }, 1000) // 1 second delay to let user see controls initially
        }
    }

    override fun onResume() {
        super.onResume()
        // Start auto-hide timer when fragment resumes in mini player mode
        // This ensures auto-hide works when returning from full-screen or when the fragment becomes active
        if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            resetAutoHideTimer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelAutoHideTimer()
        cancelControlsHideTimer()
        _binding = null
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                
                // Determine if this is a horizontal or vertical swipe
                if (abs(diffX) > abs(diffY)) {
                    // Horizontal swipe - check threshold and velocity
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - previous track
                            onSwipeRight()
                        } else {
                            // Swipe left - next track
                            onSwipeLeft()
                        }
                        return true
                    }
                } else {
                    // Vertical swipe - check threshold and velocity
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            // Swipe down - navigate back (full screen only)
                            onSwipeDown()
                        } else {
                            // Swipe up - expand to full screen (mini player only)
                            onSwipeUp()
                        }
                        return true
                    }
                }
                return false
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Handle single tap based on mode
                if (!isFullScreen) {
                    // Mini player mode - expand to full screen
                    onUserInteraction()
                    expandToFullScreen()
                    return true
                } else {
                    // Full-screen mode - show controls
                    showControls()
                    return true
                }
            }
            
            override fun onDown(e: MotionEvent): Boolean {
                // Track any touch interaction and enable gesture detection
                onUserInteraction()
                return true // Must return true to enable gesture detection
            }
        })
    }

    private fun setupTouchHandling() {
        if (!isFullScreen) {
            // In mini player mode, allow click to expand and swipe gestures
            binding.root.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
        } else {
            // In full-screen mode, handle gestures and show controls on tap
            binding.root.setOnTouchListener { _, event ->
                // Always let gesture detector handle the event first
                gestureDetector.onTouchEvent(event)
                
                // Always return true to consume all touch events and prevent them from reaching fragments underneath
                true
            }
            
            // Remove the separate album art click listener since gestures handle this
        }
    }

    private fun onSwipeLeft() {
        // Swipe left - next track
        onUserInteraction()
        viewModel.skipNext()
    }

    private fun onSwipeRight() {
        // Swipe right - previous track
        onUserInteraction()
        viewModel.skipPrevious()
    }

    private fun onSwipeDown() {
        // Swipe down - navigate back to MediaItemFragment (full screen only)
        if (isFullScreen) {
            onUserInteraction()
            collapseFromFullScreen()
        }
    }

    private fun onSwipeUp() {
        // Swipe up - expand to full screen (mini player only)
        if (!isFullScreen) {
            onUserInteraction()
            expandToFullScreen()
        }
    }

    private fun onUserInteraction() {
        // Track user interaction and reset auto-hide timer (mini player only)
        if (!isFullScreen) {
            resetAutoHideTimer()
        } else {
            // In full-screen mode, reset controls hide timer and show controls if hidden
            showControls()
        }
    }

    private fun resetAutoHideTimer() {
        cancelAutoHideTimer()
        
        hideRunnable = Runnable {
            // Only hide if we're still in mini player mode and fragment is still valid
            if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE) {
                hideMiniPlayer()
            }
        }
        
        hideHandler.postDelayed(hideRunnable!!, AUTO_HIDE_DELAY_MS)
    }

    private fun cancelAutoHideTimer() {
        hideRunnable?.let { runnable ->
            hideHandler.removeCallbacks(runnable)
            hideRunnable = null
        }
    }

    private fun hideMiniPlayer() {
        // Animate the mini player out with a fade effect
        if (_binding != null) {
            val fadeOut = ObjectAnimator.ofFloat(binding.root, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.start()
            
            // Hide after animation completes
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (_binding != null) {
                        binding.root.visibility = View.GONE
                        binding.root.alpha = 1f // Reset alpha for next show
                    }
                }
            })
        }
    }

    private fun showMiniPlayer() {
        // Show mini player with fade in effect
        if (_binding != null && !isFullScreen) {
            binding.root.alpha = 0f
            binding.root.visibility = View.VISIBLE
            
            val fadeIn = ObjectAnimator.ofFloat(binding.root, "alpha", 0f, 1f)
            fadeIn.duration = 300
            fadeIn.start()
            
            resetAutoHideTimer()
        }
    }

    private fun updateUI(metadata: NowPlayingFragmentViewModel.NowPlayingMetadata?) {
        // Update UI with metadata
        metadata?.let {
            binding.title.text = it.title
            binding.subtitle.text = it.subtitle
            binding.duration.text = it.duration
            
            // If mini player was hidden and we get new metadata, show it
            if (!isFullScreen && binding.root.visibility != View.VISIBLE) {
                showMiniPlayer()
            } else if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
                // If mini player is already visible, reset the auto-hide timer
                // Add a small delay to ensure the UI update is complete
                Handler(Looper.getMainLooper()).postDelayed({
                    if (_binding != null && !isFullScreen && binding.root.visibility == View.VISIBLE) {
                        resetAutoHideTimer()
                    }
                }, 50) // Small delay for UI update completion
            }
            
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

        // Determine background and text colors for app theme
        val backgroundColor = darkMutedColor 
            ?: darkVibrantColor 
            ?: android.graphics.Color.argb(200, 
                android.graphics.Color.red(primaryColor),
                android.graphics.Color.green(primaryColor),
                android.graphics.Color.blue(primaryColor)
            )

        val textColor = if (isColorDark(backgroundColor)) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.BLACK
        }

        // Update app-wide theme
        mainActivityViewModel?.updateAppTheme(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            backgroundColor = backgroundColor,
            textColor = textColor
        )

        // Apply colors to UI elements
        applyColorsToUI(primaryColor, secondaryColor)
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 
                           0.587 * android.graphics.Color.green(color) + 
                           0.114 * android.graphics.Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun applyDefaultColors() {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        
        // Reset app theme to default colors
        mainActivityViewModel?.resetAppThemeToDefault()
        
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
        binding.repeatButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(secondaryColor, android.graphics.BlendMode.SRC_IN)
        binding.shuffleButton.drawable?.colorFilter = android.graphics.BlendModeColorFilter(secondaryColor, android.graphics.BlendMode.SRC_IN)

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
        // Cancel auto-hide timer when expanding to full-screen
        cancelAutoHideTimer()
        
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

    /**
     * Public method to show the mini player when called from other fragments
     * (e.g., when scrolling in MediaItemFragment)
     */
    fun showMiniPlayerOnScroll() {
        // Only show if music is playing and we're in mini player mode
        val isPlayingMusic = viewModel.playbackState.value != Player.STATE_IDLE &&
                           viewModel.mediaMetadata.value != null
        
        if (!isFullScreen && isPlayingMusic && binding.root.visibility != View.VISIBLE) {
            showMiniPlayer()
        } else if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            // If already visible, just reset the auto-hide timer
            resetAutoHideTimer()
        }
    }

    /**
     * Public method to start auto-hide timer (useful for external calls)
     */
    fun startAutoHideTimer() {
        if (!isFullScreen && binding.root.visibility == View.VISIBLE) {
            resetAutoHideTimer()
        }
    }

    private fun updateRepeatButton(repeatMode: Int) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                binding.repeatButton.setImageResource(R.drawable.ic_repeat_black_24dp)
                binding.repeatButton.alpha = 0.5f // Dimmed when off
            }
            Player.REPEAT_MODE_ONE -> {
                binding.repeatButton.setImageResource(R.drawable.ic_repeat_one_black_24dp)
                binding.repeatButton.alpha = 1.0f // Full opacity when on
            }
            Player.REPEAT_MODE_ALL -> {
                binding.repeatButton.setImageResource(R.drawable.ic_repeat_black_24dp)
                binding.repeatButton.alpha = 1.0f // Full opacity when on
            }
        }
    }

    private fun updateShuffleButton(shuffleEnabled: Boolean) {
        // Check if fragment is still valid before accessing binding
        if (_binding == null) return
        
        binding.shuffleButton.alpha = if (shuffleEnabled) 1.0f else 0.5f
    }

    private fun resetControlsHideTimer() {
        // Only hide controls in full-screen mode
        if (!isFullScreen) return
        
        cancelControlsHideTimer()
        
        controlsHideRunnable = Runnable {
            // Only hide if we're still in full-screen mode and fragment is still valid
            if (_binding != null && isFullScreen) {
                hideControls()
            }
        }
        
        controlsHideHandler.postDelayed(controlsHideRunnable!!, CONTROLS_HIDE_DELAY_MS)
    }

    private fun cancelControlsHideTimer() {
        controlsHideRunnable?.let { runnable ->
            controlsHideHandler.removeCallbacks(runnable)
            controlsHideRunnable = null
        }
    }

    private fun hideControls() {
        // Hide the controls container in full-screen mode
        if (_binding != null && isFullScreen) {
            // Find the LinearLayout with controls at the bottom
            val controlsContainer = binding.root.getChildAt(binding.root.childCount - 1) as? android.widget.LinearLayout
            
            controlsContainer?.let { container ->
                // Fade out the controls container
                val fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f)
                fadeOut.duration = 300
                fadeOut.start()
                
                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (_binding != null && isFullScreen) {
                            container.visibility = View.GONE
                        }
                    }
                })
            }
        }
    }

    private fun showControls() {
        // Show controls container in full-screen mode
        if (_binding != null && isFullScreen) {
            // Find the LinearLayout with controls at the bottom
            val controlsContainer = binding.root.getChildAt(binding.root.childCount - 1) as? android.widget.LinearLayout
            
            controlsContainer?.let { container ->
                if (container.visibility != View.VISIBLE) {
                    container.alpha = 0f
                    container.visibility = View.VISIBLE
                    
                    val fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f)
                    fadeIn.duration = 300
                    fadeIn.start()
                }
                
                // Reset the controls hide timer
                resetControlsHideTimer()
            }
        }
    }
}
