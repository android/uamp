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

package com.example.android.uamp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.android.uamp.R
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.databinding.FragmentMediaitemListBinding
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MediaItemFragmentViewModel
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.MainActivity

/**
 * A fragment representing a list of MediaItems.
 */
class MediaItemFragment : Fragment() {

    companion object {
        fun newInstance(mediaId: String): MediaItemFragment {
            return MediaItemFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEDIA_ID, mediaId)
                }
            }
        }
    }

    private lateinit var mediaId: String
    private val viewModel by viewModels<MediaItemFragmentViewModel> {
        InjectorUtils.provideMediaItemFragmentViewModel(requireContext(), mediaId)
    }

    private val mainActivityViewModel by lazy {
        (activity as? MainActivity)?.viewModel
    }

    private var _binding: FragmentMediaitemListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaId = arguments?.getString(ARG_MEDIA_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaitemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MediaItemAdapter { clickedItem ->
                viewModel.playMediaId(clickedItem.mediaId)
            }
            
            // Add scroll listener to show mini player when scrolling
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    
                    // When user starts scrolling, show the mini player if music is playing
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        showMiniPlayerIfPlaying()
                    }
                }
            })
        }

        // Subscribe to the media items
        viewModel.mediaItems.observe(viewLifecycleOwner) { list ->
            // Hide loading spinner when data is loaded
            binding.loadingSpinner.visibility = if (list.isNotEmpty()) View.GONE else View.VISIBLE
            (binding.list.adapter as MediaItemAdapter).submitList(list)
        }

        viewModel.networkError.observe(viewLifecycleOwner) { error ->
            if (error) {
                binding.networkError.visibility = View.VISIBLE
                binding.loadingSpinner.visibility = View.GONE
            } else {
                binding.networkError.visibility = View.GONE
            }
        }

        // Observe app theme changes and apply them to the list
        mainActivityViewModel?.appTheme?.observe(viewLifecycleOwner) { theme ->
            theme?.let { applyAppTheme(it) }
        }
    }

    private fun applyAppTheme(theme: MainActivityViewModel.AppTheme) {
        // Apply theme colors to the fragment background
        binding.root.setBackgroundColor(theme.backgroundColor)
        
        // Update the adapter to use the new theme colors
        (binding.list.adapter as? MediaItemAdapter)?.updateTheme(theme)
    }

    private fun showMiniPlayerIfPlaying() {
        // Find the NowPlayingFragment in the parent activity
        val activity = activity ?: return
        val nowPlayingFragment = activity.supportFragmentManager
            .findFragmentById(R.id.nowPlayingFragment) as? NowPlayingFragment
        
        // Call the method to show mini player on scroll
        nowPlayingFragment?.showMiniPlayerOnScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private const val ARG_MEDIA_ID = "media_id"

/**
 * RecyclerView Adapter for displaying media items.
 */
class MediaItemAdapter(
    private val itemClickedListener: (MediaItemData) -> Unit
) : RecyclerView.Adapter<MediaItemViewHolder>() {

    private var mediaItems = emptyList<MediaItemData>()
    private var currentTheme: MainActivityViewModel.AppTheme? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_mediaitem, parent, false)
        return MediaItemViewHolder(view, itemClickedListener)
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val item = mediaItems[position]
        holder.bind(item, currentTheme)
    }

    override fun getItemCount() = mediaItems.size

    fun submitList(list: List<MediaItemData>) {
        mediaItems = list
        notifyDataSetChanged()
    }

    fun updateTheme(theme: MainActivityViewModel.AppTheme) {
        currentTheme = theme
        notifyDataSetChanged() // Refresh all items with new theme
    }
}

class MediaItemViewHolder(
    view: View,
    private val itemClickedListener: (MediaItemData) -> Unit
) : RecyclerView.ViewHolder(view) {

    private var item: MediaItemData? = null
    private val titleView: TextView = view.findViewById(R.id.title)
    private val subtitleView: TextView = view.findViewById(R.id.subtitle)
    private val albumArt: ImageView = view.findViewById(R.id.albumArt)
    private val playButton: ImageView = view.findViewById(R.id.item_state)

    init {
        view.setOnClickListener {
            item?.let { itemClickedListener(it) }
        }
    }

    fun bind(item: MediaItemData, theme: MainActivityViewModel.AppTheme?) {
        this.item = item

        titleView.text = item.title
        subtitleView.text = item.subtitle

        if (item.browsable) {
            playButton.visibility = View.GONE
        } else {
            playButton.visibility = View.VISIBLE
            playButton.setImageResource(item.playbackRes)
            playButton.setOnClickListener {
                itemClickedListener(item)
            }
        }

        Glide.with(albumArt)
            .load(item.albumArtUri)
            .placeholder(R.drawable.default_art)
            .into(albumArt)

        // Apply theme colors to the item
        theme?.let { applyTheme(it) }
    }

    private fun applyTheme(theme: MainActivityViewModel.AppTheme) {
        // Apply theme colors to text views
        titleView.setTextColor(theme.textColor)
        subtitleView.setTextColor(theme.secondaryColor)
        
        // Apply subtle background tint to the item
        val backgroundTint = android.graphics.Color.argb(
            30, // Low alpha for subtle effect
            android.graphics.Color.red(theme.primaryColor),
            android.graphics.Color.green(theme.primaryColor),
            android.graphics.Color.blue(theme.primaryColor)
        )
        itemView.setBackgroundColor(backgroundTint)
        
        // Apply color filter to play button if it's visible
        if (playButton.visibility == View.VISIBLE) {
            playButton.colorFilter = android.graphics.BlendModeColorFilter(
                theme.primaryColor, 
                android.graphics.BlendMode.SRC_IN
            )
        }
    }
}
