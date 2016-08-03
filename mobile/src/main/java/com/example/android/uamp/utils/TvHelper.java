/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.uamp.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

public class TvHelper {
    private static final String TAG = LogHelper.makeLogTag(TvHelper.class);

    /**
     * Returns true when running Android TV
     *
     * @param c Context to detect UI Mode.
     * @return true when device is running in tv mode, false otherwise.
     */
    public static boolean isTvUiMode(Context c) {
        UiModeManager uiModeManager = (UiModeManager) c.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            LogHelper.d(TAG, "Running in TV mode");
            return true;
        } else {
            LogHelper.d(TAG, "Running on a non-TV mode");
            return false;
        }
    }
}
