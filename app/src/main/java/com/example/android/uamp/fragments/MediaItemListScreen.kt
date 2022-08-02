/*
 * Copyright 2022 Google Inc. All rights reserved.
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.android.uamp.MediaItemData
import com.example.android.uamp.R
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.MediaItemFragmentViewModel

/**
 * This particular instance of MediaItemDescription serves as the function which keeps track of the media items state
 * and recomposes the UI upon any changes
 *
 * @param mediaItemFragmentViewModel to reference MediaItemFragmentViewModel functions
 * @param mainActivityViewModel to reference MainActivityViewModel functions
 */
@Composable
fun MediaItemDescription(
    mediaItemFragmentViewModel: MediaItemFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val currentMediaItems by mediaItemFragmentViewModel.mediaItems.observeAsState()
    currentMediaItems?.let { mediaItems ->
        MediaItemDescription(mediaItems, mainActivityViewModel)
    }
}

/**
 * This particular instance of MediaItemDescription displays the mediaItems in a vertical list scroll format
 *
 * @param mediaItems List of mediaItems in the selected branch
 * @param mainActivityViewModel to reference MainActivityViewModel functions
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MediaItemDescription(
    mediaItems: List<MediaItemData>,
    mainActivityViewModel: MainActivityViewModel
) {
    if (mediaItems.isEmpty())
        Text("Media Items Empty")
    else
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            for (mediaItem in mediaItems) {
                item { MediaItem(mediaItem, mainActivityViewModel) }
            }
        }
}

/**
 * This function describes the UI of a mediaItem instance
 *
 * @param mediaItemData to reference details of a mediaItem in the current branch
 * @param mainActivityViewModel to reference MainActivityViewModel functions
 */
@Composable
private fun MediaItem(mediaItemData: MediaItemData, mainActivityViewModel: MainActivityViewModel) {
    val mediaItemArtDimen = dimensionResource(R.dimen.media_item_art)
    val imageModel = mediaItemData.mediaItem.mediaMetadata.artworkUri
    val mediaItemTitle = mediaItemData.title
    val mediaItemArtist = mediaItemData.subtitle

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { mainActivityViewModel.mediaItemClicked(mediaItemData) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier
                .width(mediaItemArtDimen)
                .height(mediaItemArtDimen),
            placeholder = painterResource(id = R.drawable.default_art),
            error = painterResource(id = R.drawable.default_art)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = mediaItemTitle, modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.text_margin))
                    .fillMaxWidth(),
                style = MaterialTheme.typography.h6
            )
            Text(
                text = mediaItemArtist, modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.text_margin))
                    .fillMaxWidth(),
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}