/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.uamp;

import android.app.Application;
import android.content.Context;

import com.example.android.uamp.ui.FullScreenPlayerActivity;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;

import static com.google.sample.castcompanionlibrary.cast.BaseCastManager.FEATURE_DEBUGGING;
import static com.google.sample.castcompanionlibrary.cast.BaseCastManager.FEATURE_WIFI_RECONNECT;

/**
 * The {@link Application} for the uAmp application.
 */
public class UAMPApplication extends Application {

    private String applicationId;

    private VideoCastManager mCastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationId = getResources().getString(R.string.cast_application_id);
    }

    /**
     * Get the {@link com.google.sample.castcompanionlibrary.cast.VideoCastManager} form
     * a particular context
     * @param context that is set on the castManager
     * @return VideoCastManager
     */
    public VideoCastManager getCastManager(Context context) {
        synchronized (this) {
            if (mCastManager == null) {
                mCastManager = VideoCastManager.initialize(
                        context, applicationId, FullScreenPlayerActivity.class, null);
                mCastManager.enableFeatures(FEATURE_WIFI_RECONNECT | FEATURE_DEBUGGING);
            }
        }
        return mCastManager;
    }
}
