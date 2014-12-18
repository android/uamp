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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Base64;

import com.example.android.uamp.utils.CarHelper;
import com.example.android.uamp.utils.LogHelper;

/**
 * Validates that the calling package is authorized to use this
 * {@link android.service.media.MediaBrowserService}.
 */
public class PackageValidator {
    private static final String TAG = LogHelper.makeLogTag(PackageValidator.class);

    /**
     * Disallow instantiation of this helper class.
     */
    private PackageValidator() {}

    /**
     * Throws when the caller is not authorized to get data from this MediaBrowserService
     */
    public static void checkCallerAllowed(Context context, String callingPackage, int callingUid) {
        if (!isCallerAllowed(context, callingPackage, callingUid)) {
            throw new SecurityException("signature check failed.");
        }
    }

    /**
     * @return false if the caller is not authorized to get data from this MediaBrowserService
     */
    public static boolean isCallerAllowed(Context context, String callingPackage, int callingUid) {
        // Always allow calls from the framework, self app or development environment.
        if (Process.SYSTEM_UID == callingUid || Process.myUid() == callingUid) {
            return true;
        }
        if (BuildConfig.DEBUG) {
            LogHelper.i(TAG, "Allowing caller '"+callingPackage+" because app was built in debug mode.");
            return true;
        }
        PackageInfo packageInfo;
        final PackageManager packageManager = context.getPackageManager();
        try {
            packageInfo = packageManager.getPackageInfo(
                    callingPackage, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException ignored) {
            LogHelper.w(TAG, "Package manager can't find package " + callingPackage
                        + ", defaulting to false");
            return false;
        }
        if (packageInfo == null) {
            LogHelper.w(TAG, "Package manager can't find package: " + callingPackage);
            return false;
        }

        if (packageInfo.signatures.length != 1) {
            LogHelper.w(TAG, "Package has more than one signature.");
            return false;
        }
        final byte[] signature = packageInfo.signatures[0].toByteArray();

        // Test for official car app signatures:
        if (CarHelper.isValidAutoPackageSignature(signature)) {
            return true;
        }

        // If you want to allow other consumers, check for their signatures here.

        LogHelper.v(TAG, "Signature not valid.  Found: \n" + Base64.encodeToString(signature, 0));
        return false;
    }

}
