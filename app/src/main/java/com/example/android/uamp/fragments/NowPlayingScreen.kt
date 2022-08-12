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

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.android.uamp.R
import com.example.android.uamp.theme.buttonColor
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel

/**
 * This particular instance of NowPlayingDescription keeps track of a mediaItem's state such that
 * the correct mediaItemData is displayed i.e. the correct song being played, song's title, author,
 * its respective album art, duration, and current position.
 *
 * Recomposes upon change in mediaItem's state.
 *
 * @param nowPlayingFragmentViewModel to reference NowPlayingFragmentViewModel functions
 * @param mainActivityViewModel to reference MainActivityViewModel functions
 * @param navController to navigate to SettingsScreen
 */
@Composable
fun NowPlayingDescription(
    nowPlayingFragmentViewModel: NowPlayingFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel,
    navController: NavController
) {
    val currentMediaItem by nowPlayingFragmentViewModel.mediaItem.observeAsState()
    currentMediaItem?.let { mediaItem ->
        NowPlayingDescription(
            mediaItem,
            nowPlayingFragmentViewModel,
            mainActivityViewModel,
            navController
        )
    }
}

/**
 * This particular instance of NowPlayingDescription serves to showcase the current media item
 * being played on the screen
 *
 * @param mediaItem current mediaItem being played
 * @param nowPlayingFragmentViewModel to reference NowPlayingFragmentViewModel functions
 */
@Composable
private fun NowPlayingDescription(
    mediaItem: MediaItem,
    nowPlayingFragmentViewModel: NowPlayingFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel,
    navController: NavController
) {
    Surface {
        NowPlaying(mediaItem, nowPlayingFragmentViewModel, mainActivityViewModel, navController)
    }
}

/**
 * NowPlaying describes the UI of the NowPlayingScreen which recomposes upon changes in the state of
 * currentMediaItem's position, duration, and change in playback (play/pause).
 *
 * @param mediaItem current mediaItem being played
 * @param nowPlayingFragmentViewModel to reference NowPlayingFragmentViewModel functions
 * @param mainActivityViewModel to reference MainActivityViewModel functions
 * @param navController to navigate to SettingsScreen
 */
@Composable
private fun NowPlaying(
    mediaItem: MediaItem,
    nowPlayingFragmentViewModel: NowPlayingFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel,
    navController: NavController
) {
    val position: Long by nowPlayingFragmentViewModel.mediaPositionSeconds.observeAsState(0)
    val duration: Long by nowPlayingFragmentViewModel.mediaDurationSeconds.observeAsState(0)
    val isPlaying: Boolean by nowPlayingFragmentViewModel.mediaIsPlaying.observeAsState(true)

    val buttonWidth = dimensionResource(id = R.dimen.exo_media_button_width)
    val margins = dimensionResource(id = R.dimen.text_margin)

    val play = painterResource(id = R.drawable.ic_play_arrow_black_24dp)
    val pause = painterResource(id = R.drawable.ic_pause_black_24dp)

    val settingsRoute = stringResource(id = R.string.route_settings)

    val contentDesc =
        if (isPlaying) {
            stringResource(R.string.pause_content_desc)
        } else {
            stringResource(R.string.play_content_desc)
        }

    val mediaItemArtwork = mediaItem.mediaMetadata.artworkUri

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.h5
                )
            },
            actions = {
                IconButton(onClick = {
                    navController.navigate(route = settingsRoute)
                }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_button_desc)
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.primary
        )

        AsyncImage(
            model = mediaItemArtwork,
            contentDescription = stringResource(id = R.string.album_art_alt),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.primary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = if (isPlaying) pause else play,
                contentDescription = contentDesc,
                modifier = Modifier
                    .clickable {
                        mainActivityViewModel.playMedia(
                            mediaItem
                        )
                    }
                    .width(buttonWidth),
                tint = MaterialTheme.colors.buttonColor
            )
            Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.Top) {
                Text(
                    text = mediaItem.mediaMetadata.title.toString(),
                    modifier = Modifier
                        .padding(start = margins, top = 8.dp, end = margins)
                        .wrapContentHeight(Alignment.CenterVertically),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = mediaItem.mediaMetadata.artist.toString(),
                    modifier = Modifier.padding(start = margins, end = margins),
                    style = MaterialTheme.typography.h6
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = timestampToMSS(LocalContext.current, position), modifier = Modifier
                        .padding(start = margins, top = 8.dp, end = margins),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = timestampToMSS(LocalContext.current, duration), modifier = Modifier
                        .padding(start = margins, end = margins),
                    style = MaterialTheme.typography.subtitle1
                )
            }
        }
    }
}

/** Converts milliseconds to a display of minutes and seconds. */
fun timestampToMSS(context: Context, seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds - (minutes * 60)
    return if (seconds < 0) context.getString(R.string.duration_unknown)
    else context.getString(R.string.duration_format).format(minutes, remainingSeconds)
}


