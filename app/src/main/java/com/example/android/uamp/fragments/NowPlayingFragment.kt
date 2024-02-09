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

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import com.bumptech.glide.Glide
import com.example.android.uamp.R
import com.example.android.uamp.databinding.FragmentNowplayingBinding
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * A fragment representing the current media item being played.
 */
@AndroidEntryPoint
class NowPlayingFragment : Fragment() {

    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()
    private val nowPlayingViewModel by viewModels<NowPlayingFragmentViewModel>()

    lateinit var binding: FragmentNowplayingBinding

    companion object {
        /** Converts milliseconds to a display of minutes and seconds. */
        fun timestampToMSS(context: Context, position: Long): String {
            val totalSeconds = Math.floor(position / 1E3).toInt()
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds - (minutes * 60)
            return if (position < 0) context.getString(R.string.duration_unknown)
            else context.getString(R.string.duration_format).format(minutes, remainingSeconds)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNowplayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always true, but lets lint know that as well.
        val context = activity ?: return

        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaItem.observe(viewLifecycleOwner,
            Observer { mediaItem -> updateUI(view, mediaItem) })
        nowPlayingViewModel.mediaButtonRes.observe(viewLifecycleOwner,
            Observer { res ->
                binding.mediaButton.setImageResource(res)
            })
        nowPlayingViewModel.mediaPosition.observe(viewLifecycleOwner,
            Observer { pos ->
                binding.position.text = timestampToMSS(context, pos)
            })
        nowPlayingViewModel.mediaDuration.observe(viewLifecycleOwner,
            Observer { duration ->
                binding.duration.text = timestampToMSS(context, duration)
            })

        // Setup UI handlers for buttons
        binding.mediaButton.setOnClickListener {
            nowPlayingViewModel.mediaItem.value?.let {
                mainActivityViewModel.playMedia(it, pauseThenPlaying = true)
            }
        }

        // Initialize playback duration and position to zero
        binding.duration.text = timestampToMSS(context, 0L)
        binding.position.text = timestampToMSS(context, 0L)
    }

    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(view: View, mediaItem: MediaItem) = with(binding) {
        val metadata = mediaItem.mediaMetadata
        if (metadata.artworkUri == Uri.EMPTY) {
            albumArt.setImageResource(R.drawable.ic_album_black_24dp)
        } else {
            Glide.with(view)
                .load(metadata.artworkUri)
                .into(albumArt)
        }
        title.text = metadata.title
        subtitle.text = metadata.albumTitle
    }
}
