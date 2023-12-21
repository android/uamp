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
import com.example.android.uamp.UampApplication
import com.example.android.uamp.common.MusicServiceConnection
import com.example.android.uamp.media.MusicService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Static methods used to inject classes needed for various Activities and Fragments.
 */
@Module
class InjectorUtils {

    @Singleton
    @Provides
    fun provideContext(
        application: UampApplication
    ): Context {
        return application.applicationContext
    }

    @Singleton
    @Provides
    fun providesApplication(
        application: UampApplication
    ): Application {
        return application
    }

    @Singleton // TODO comment to fix
    @Provides
    fun provideMusicServiceConnection(
        context: Context
    ): MusicServiceConnection {
        // TODO uncomment to fix
//        return MusicServiceConnection.getInstance(
        return MusicServiceConnection( // TODO comment to fix
            context,
            ComponentName(context, MusicService::class.java)
        )
    }
}