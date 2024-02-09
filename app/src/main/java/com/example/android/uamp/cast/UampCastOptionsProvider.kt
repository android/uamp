/*
 * Copyright 2020 Google Inc. All rights reserved.
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

package com.example.android.uamp.cast

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.cast.DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER_WITH_DRM
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

class UampCastOptionsProvider : OptionsProvider {

    @OptIn(UnstableApi::class)
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            // Use the Default Media Receiver with DRM support.
            .setReceiverApplicationId(APP_ID_DEFAULT_RECEIVER_WITH_DRM)
            .setCastMediaOptions(
                CastMediaOptions.Builder()
                    // We manage the media session and the notifications ourselves.
                    .setMediaSessionEnabled(false)
                    .setNotificationOptions(null)
                    .build()
            )
            .setStopReceiverApplicationWhenEndingSession(true).build()
    }

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider> {
        return mutableListOf()
    }
}