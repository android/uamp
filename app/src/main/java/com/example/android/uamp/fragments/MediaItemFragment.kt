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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_mediaitem, parent, false)
        return MediaItemViewHolder(view, itemClickedListener)
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        val item = mediaItems[position]
        holder.bind(item)
    }

    override fun getItemCount() = mediaItems.size

    fun submitList(list: List<MediaItemData>) {
        mediaItems = list
        notifyDataSetChanged()
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

    fun bind(item: MediaItemData) {
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
    }
}
