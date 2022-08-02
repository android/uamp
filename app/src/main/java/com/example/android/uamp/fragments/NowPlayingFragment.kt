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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android.uamp.R
import com.example.android.uamp.theme.UAMPTheme
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel

/**
 * A fragment representing the current media item being played.
 */
class NowPlayingFragment : Fragment() {

    private val mainActivityViewModel by activityViewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(requireContext())
    }
    private val nowPlayingViewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    companion object {
        fun newInstance() = NowPlayingFragment()

        /** Converts milliseconds to a display of minutes and seconds. */
        fun timestampToMSS(context: Context, position: Long): String {
            val totalSeconds = Math.floor(position / 1E3).toInt()
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds - (minutes * 60)
            return if (position < 0) context.getString(R.string.duration_unknown)
            else context.getString(R.string.duration_format).format(minutes, remainingSeconds)
        }
    }

    /**
     * @return ComposeView of the fragment rather than xml
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): ComposeView {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                UAMPTheme {
                    // Redirect to compose
                    Scaffold() {
                        // Add navigation functionality between NowPlayingScreen and SettingsScreen
                        val navController = rememberNavController()

                        NavHost(navController = navController, startDestination = "nowplaying") {
                            composable(route = "nowplaying") {
                                NowPlayingDescription(
                                    nowPlayingViewModel,
                                    mainActivityViewModel,
                                    navController
                                )
                            }
                            composable(route = "settings") {
                                SettingsScreenDescription(mainActivityViewModel, navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
