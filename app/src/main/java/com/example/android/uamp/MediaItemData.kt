/*
 * Copyright 2018 Google Inc. All rights reserved.
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

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.util.DiffUtil
import android.support.v4.media.MediaBrowserCompat.MediaItem
import com.example.android.uamp.viewmodels.MediaItemFragmentViewModel

/**
 * Data class to encapsulate properties of a [MediaItem].
 *
 * If an item is [browsable] it means that it has a list of child media items that
 * can be retrieved by passing the mediaId to [MediaBrowserCompat.subscribe].
 *
 * Objects of this class are built from [MediaItem]s in
 * [MediaItemFragmentViewModel.subscriptionCallback].
 */
data class MediaItemData(
        val mediaId: String,
        val title: String,
        val subtitle: String,
        val albumArtUri: Uri,
        val browsable: Boolean,
        var playbackRes: Int) {

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemData>() {
            override fun areItemsTheSame(oldItem: MediaItemData?,
                                         newItem: MediaItemData?): Boolean =
                    oldItem?.let { it.mediaId == newItem?.mediaId } ?: false

            override fun areContentsTheSame(oldItem: MediaItemData?, newItem: MediaItemData?) =
                    oldItem == newItem
        }
    }
}

