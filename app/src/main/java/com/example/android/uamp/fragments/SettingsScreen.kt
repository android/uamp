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

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.navigation.NavController
import com.example.android.uamp.R
import com.example.android.uamp.viewmodels.MainActivityViewModel
import kotlinx.coroutines.launch


/**
 * SettingsScreenDescription serves to describe the UI of the settings screen, which includes a TopAppBar
 * for the user to return to the NowPlayingScreen, a switch to enable spatial audio, and spatializer
 * function outputs of the current mediaItem being played.
 *
 * @param mainActivityViewModel to reference functions in MainActivityViewModel
 * @param navController to allow navigation back to NowPlayingScreen
 */
@Composable
fun SettingsScreenDescription(
    mainActivityViewModel: MainActivityViewModel, navController: NavController
) {
    val mCheckedValue = remember { mutableStateOf(true) }
    val audioManager =
        ContextCompat.getSystemService(LocalContext.current, AudioManager::class.java)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.h5) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.arrowback_desc))
                }
            },
            backgroundColor = MaterialTheme.colors.primary
        )
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.enable_spatial_audio), modifier = Modifier.weight(2f)
                )
                Switch(
                    checked = mCheckedValue.value,
                    onCheckedChange = {
                        mCheckedValue.value = it
                        toggleSpatialAudio(mainActivityViewModel, mCheckedValue.value)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colors.primary,
                        checkedThumbColor = MaterialTheme.colors.primary,
                        uncheckedTrackColor = MaterialTheme.colors.primary,
                        uncheckedThumbColor = Color.White
                    ),
                )
            }
            SpatialAudioOutput(audioManager = audioManager!!)
        }
    }
}

/**
 * SpatialAudioOutput initializes a spatializer through which the current spatial audio characteristics
 * of the current media item being played are displayed.
 *
 * @param audioManager to declare spatializer
 */
@Composable
fun SpatialAudioOutput(audioManager: AudioManager) {

    val spatializer = audioManager.spatializer

    val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
        C.AUDIO_CONTENT_TYPE_UNKNOWN
    ).setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL).build()
    val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_5POINT1).build()

    val canBeSpatialized = spatializer.canBeSpatialized(attributes, audioFormat)
    val getImmersiveAudioLevel = spatializer.immersiveAudioLevel
    val isEnabled = spatializer.isEnabled
    val isAvailable = spatializer.isAvailable

    // Introduced in API 33, be sure to use a compatible device
    val isHeadTrackerAvailable =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) spatializer.isHeadTrackerAvailable else false

    Column(verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.spatial_audio_output)
        )
        Surface() {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Row() {
                    Text(
                        text = stringResource(R.string.can_be_spatialized),
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.h6
                    )
                    Text(text = canBeSpatialized.toString())
                }
                Row() {
                    Text(
                        text = stringResource(R.string.get_immersive_audio_level),
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = when (getImmersiveAudioLevel) {
                            0 -> stringResource(R.string.audio_level_none)
                            1 -> stringResource(R.string.audio_level_multichannel)
                            else -> stringResource(R.string.audio_level_other)
                        }
                    )
                }
                Row() {
                    Text(
                        text = stringResource(R.string.is_available),
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.h6
                    )
                    Text(text = isAvailable.toString())
                }
                Row() {
                    Text(
                        text = stringResource(R.string.is_enabled),
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.h6
                    )
                    Text(text = isEnabled.toString())
                }
                Row() {
                    Text(
                        text = stringResource(R.string.is_head_tracker_available),
                        modifier = Modifier.weight(1f),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.h6
                    )
                    Text(text = isHeadTrackerAvailable.toString())
                }
            }
        }
    }
}

/**
 * Helper function for the change in switch state which enables/disables Spatial Audio
 * Invokes the toggleSpatialization fucntion in MainActivityViewModel
 *
 * @param mainActivityViewModel to reference toggleSpatialization
 * @param enable to pass the current checked value of switch
 */
fun toggleSpatialAudio(mainActivityViewModel: MainActivityViewModel, enable: Boolean) {
    mainActivityViewModel.viewModelScope.launch { mainActivityViewModel.toggleSpatialization(enable) }
}
