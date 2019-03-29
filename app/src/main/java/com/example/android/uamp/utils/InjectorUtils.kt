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

package com.example.android.uamp.utils

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.example.android.uamp.common.MediaSessionConnection
import com.example.android.uamp.media.MusicService
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.MediaItemFragmentViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel

/**
 * Static methods used to inject classes needed for various Activities and Fragments.
 */
object InjectorUtils {
    private fun provideMediaSessionConnection(context: Context): MediaSessionConnection {
        return MediaSessionConnection.getInstance(context,
                ComponentName(context, MusicService::class.java))
    }

    fun provideMainActivityViewModel(context: Context): MainActivityViewModel.Factory {
        val applicationContext = context.applicationContext
        val mediaSessionConnection = provideMediaSessionConnection(applicationContext)
        return MainActivityViewModel.Factory(mediaSessionConnection)
    }

    fun provideMediaItemFragmentViewModel(context: Context, mediaId: String)
            : MediaItemFragmentViewModel.Factory {
        val applicationContext = context.applicationContext
        val mediaSessionConnection = provideMediaSessionConnection(applicationContext)
        return MediaItemFragmentViewModel.Factory(mediaId, mediaSessionConnection)
    }

    fun provideNowPlayingFragmentViewModel(context: Context)
            : NowPlayingFragmentViewModel.Factory {
        val applicationContext = context.applicationContext
        val mediaSessionConnection = provideMediaSessionConnection(applicationContext)
        return NowPlayingFragmentViewModel.Factory(
                applicationContext as Application, mediaSessionConnection)
    }
}