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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.example.android.uamp.R
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel.NowPlayingMetadata

/**
 * A fragment representing the current media item being played.
 */
class NowPlayingFragment : Fragment() {
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var nowPlayingViewModel: NowPlayingFragmentViewModel
    private lateinit var positionTextView: TextView

    companion object {
        fun newInstance() = NowPlayingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nowplaying, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always true, but lets lint know that as well.
        val context = activity ?: return

        // Inject our activity and view models into this fragment
        mainActivityViewModel = ViewModelProviders
                .of(context, InjectorUtils.provideMainActivityViewModel(context))
                .get(MainActivityViewModel::class.java)
        nowPlayingViewModel = ViewModelProviders
                .of(context, InjectorUtils.provideNowPlayingFragmentViewModel(context))
                .get(NowPlayingFragmentViewModel::class.java)

        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaMetadata.observe(this,
                Observer { mediaItem -> updateUI(view, mediaItem) })
        nowPlayingViewModel.mediaButtonRes.observe(this,
                Observer { res -> view.findViewById<ImageView>(R.id.media_button).setImageResource(res) })
        nowPlayingViewModel.mediaPosition.observe(this,
                Observer { pos -> positionTextView.text =
                        NowPlayingMetadata.timestampToMSS(context, pos) })

        // Setup UI handlers for buttons
        view.findViewById<ImageButton>(R.id.media_button).setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { mainActivityViewModel.playMediaId(it.id) } }

        // Initialize playback duration and position to zero
        view.findViewById<TextView>(R.id.duration).text =
                NowPlayingMetadata.timestampToMSS(context, 0L)
        positionTextView = view.findViewById<TextView>(R.id.position)
                .apply { text = NowPlayingMetadata.timestampToMSS(context, 0L) }
    }

    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(view: View, metadata: NowPlayingFragmentViewModel.NowPlayingMetadata) {
        val albumArtView = view.findViewById<ImageView>(R.id.albumArt)
        if (metadata.albumArtUri == Uri.EMPTY) {
            albumArtView.setImageResource(R.drawable.ic_album_black_24dp)
        } else {
            Glide.with(view)
                    .load(metadata.albumArtUri)
                    .into(albumArtView)
        }
        view.findViewById<TextView>(R.id.title).text = metadata.title
        view.findViewById<TextView>(R.id.subtitle).text = metadata.subtitle
        view.findViewById<TextView>(R.id.duration).text = metadata.duration
    }
}