package com.example.android.uamp.fragments

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.example.android.uamp.R
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel

@Composable //stateful
fun NowPlayingDescription(
    nowPlayingFragmentViewModel: NowPlayingFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val currentMediaItem by nowPlayingFragmentViewModel.mediaItem.observeAsState()
    currentMediaItem?.let { mediaItem ->
        NowPlayingDescription(mediaItem, nowPlayingFragmentViewModel, mainActivityViewModel)
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NowPlayingDescription(
    mediaItem: MediaItem,
    nowPlayingFragmentViewModel: NowPlayingFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    if (mediaItem.equals(null)) {
        Text(text = "Media Item is null")
    } else {
        Surface() {
            NowPlaying(mediaItem, nowPlayingFragmentViewModel, mainActivityViewModel)
        }
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun NowPlaying(
    mediaItem: MediaItem,
    nowPlayingFragmentViewModel: NowPlayingFragmentViewModel,
    mainActivityViewModel: MainActivityViewModel
) {


    val position: Long? by nowPlayingFragmentViewModel.mediaPosition.observeAsState(0)
    val duration: Long? by nowPlayingFragmentViewModel.mediaDuration.observeAsState(0)
    val buttonRes: Int? by nowPlayingFragmentViewModel.mediaButtonRes.observeAsState()
    //val isPlaying by nowPlayingFragmentViewModel.isPlaying.observeAsState()

    Log.d("DURATION", "${duration}")
    Log.d("POSITION", "${position}")

    val positionMinute = (position!!.div(1000)).div(60)
    val positionSecond = "%02d".format((position!!.div(1000)).mod(60))

    val durationMinute = (duration!!.div(1000)).div(60)
    val durationSecond = "%02d".format((duration!!.div(1000)).mod(60))

    val buttonHeight = dimensionResource(id = R.dimen.exo_media_button_height)
    val buttonWidth = dimensionResource(id = R.dimen.exo_media_button_width)
    val margins = dimensionResource(id = R.dimen.text_margin)

    val mediaItemArtwork = mediaItem.mediaMetadata.artworkUri



    Column() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                },
                backgroundColor = Color.White
            )
            //AppBar()
//            Button(
//                onClick = {
//
//                },
//                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
//                border = BorderStroke(1.dp, Color.Black)
//            ) {
//
//                Image(
//                    painter = painterResource(id = R.drawable.baseline_settings_24),
//                    contentDescription = null
//                )
//            }
        }

        AsyncImage(
            model = mediaItemArtwork,
            contentDescription = stringResource(id = R.string.album_art_alt),
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = buttonRes!!),
                contentDescription = null,
                modifier = Modifier
                    //.fillMaxWidth()
                    .width(buttonWidth)
                    .clickable(){
                        mainActivityViewModel.playMedia(
                            mediaItem
                        )
                    }

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
                    text = "${positionMinute}:${positionSecond}", modifier = Modifier
                        .padding(start = margins, top = 8.dp, end = margins),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "${durationMinute}:${durationSecond}", modifier = Modifier
                        .padding(start = margins, end = margins),
                    style = MaterialTheme.typography.subtitle1

                )
            }
        }
    }

}
//@Composable
//fun AppBar(){
//    TopAppBar(
//        title = { Text("") },
//        navigationIcon = {
//            IconButton(onClick = { /* doSomething() */ }) {
//                Icon(Icons.Filled.Settings, contentDescription = null)
//            }
//        },
//        backgroundColor = Color.White
//    )
//}

//@Preview
//@Composable
//private fun NowPlayingPreview() {
//    NowPlaying()
//}


