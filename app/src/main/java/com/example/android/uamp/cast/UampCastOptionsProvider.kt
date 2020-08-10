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
import com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions


class UampCastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context?): CastOptions? {
        return CastOptions.Builder()
            // Use the Default Media Receiver.
            // See: https://developers.google.com/cast/docs/caf_receiver#default_media_receiver.
            // If your content is DRM protected you can use the ExoPlayer default receiver app id
            // which has a value of "A12D4273"
            .setReceiverApplicationId(DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(
                CastMediaOptions.Builder()
                    // We manage the media session and the notifications ourselves.
                    .setMediaSessionEnabled(false)
                    .setNotificationOptions(null)
                    .build()
            )
            .setStopReceiverApplicationWhenEndingSession(true).build()
    }

    override fun getAdditionalSessionProviders(context: Context?): List<SessionProvider?>? {
        return null
    }
}