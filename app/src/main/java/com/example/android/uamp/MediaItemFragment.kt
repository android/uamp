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

package com.example.android.uamp

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.MediaItemFragmentViewModel
import kotlinx.android.synthetic.main.fragment_mediaitem_list.list
import kotlinx.android.synthetic.main.fragment_mediaitem_list.loadingSpinner

/**
 * A fragment representing a list of MediaItems.
 */
class MediaItemFragment : Fragment() {
    private lateinit var mediaId: String
    private lateinit var mainActivityViewModel: MainActivityViewModel
    private lateinit var mediaItemFragmentViewModel: MediaItemFragmentViewModel

    private val listAdapter = MediaItemAdapter { clickedItem ->
        mainActivityViewModel.mediaItemClicked(clickedItem)
    }

    companion object {
        fun newInstance(mediaId: String): MediaItemFragment {

            return MediaItemFragment().apply {
                arguments = Bundle().apply {
                    putString(MEDIA_ID_ARG, mediaId)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mediaitem_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Always true, but lets lint know that as well.
        val context = activity ?: return
        mediaId = arguments?.getString(MEDIA_ID_ARG) ?: return

        mainActivityViewModel = ViewModelProviders
                .of(context, InjectorUtils.provideMainActivityViewModel(context))
                .get(MainActivityViewModel::class.java)

        mediaItemFragmentViewModel = ViewModelProviders
                .of(this, InjectorUtils.provideMediaItemFragmentViewModel(context, mediaId))
                .get(MediaItemFragmentViewModel::class.java)
        mediaItemFragmentViewModel.mediaItems.observe(this,
                Observer<List<MediaItemData>> { list ->
                    val isEmptyList = list?.isEmpty() ?: true
                    loadingSpinner.visibility = if (isEmptyList) View.VISIBLE else View.GONE
                    listAdapter.submitList(list)
                })

        // Set the adapter
        if (list is RecyclerView) {
            list.layoutManager = LinearLayoutManager(list.context)
            list.adapter = listAdapter
        }
    }
}

private const val MEDIA_ID_ARG = "com.example.android.uamp.MediaItemFragment.MEDIA_ID"
