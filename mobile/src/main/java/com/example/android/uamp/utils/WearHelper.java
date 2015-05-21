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
package com.example.android.uamp.utils;

import android.os.Bundle;
import android.support.wearable.media.MediaControlConstants;

public class WearHelper {
    private static final String WEAR_APP_PACKAGE_NAME = "com.google.android.wearable.app";

    public static boolean isValidWearCompanionPackage(String packageName) {
        return WEAR_APP_PACKAGE_NAME.equals(packageName);
    }

    public static void setShowCustomActionOnWear(Bundle customActionExtras, boolean showOnWear) {
        if (showOnWear) {
            customActionExtras.putBoolean(
                    MediaControlConstants.EXTRA_CUSTOM_ACTION_SHOW_ON_WEAR, true);
        } else {
            customActionExtras.remove(MediaControlConstants.EXTRA_CUSTOM_ACTION_SHOW_ON_WEAR);
        }
    }

    public static void setUseBackgroundFromTheme(Bundle extras, boolean useBgFromTheme) {
        if (useBgFromTheme) {
            extras.putBoolean(MediaControlConstants.EXTRA_BACKGROUND_COLOR_FROM_THEME, true);
        } else {
            extras.remove(MediaControlConstants.EXTRA_BACKGROUND_COLOR_FROM_THEME);
        }
    }

    public static void setSlotReservationFlags(Bundle extras, boolean reserveSkipToNextSlot,
                                               boolean reserveSkipToPrevSlot) {
        if (reserveSkipToPrevSlot) {
            extras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, true);
        } else {
            extras.remove(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS);
        }
        if (reserveSkipToNextSlot) {
            extras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, true);
        } else {
            extras.remove(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT);
        }
    }
}
