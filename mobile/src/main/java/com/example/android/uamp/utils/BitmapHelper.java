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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BitmapHelper {
    private static final String TAG = LogHelper.makeLogTag(BitmapHelper.class);

    // Bitmap size for album art in media notifications when there are more than 3 playback actions
    public static final int MEDIA_ART_SMALL_WIDTH=64;
    public static final int MEDIA_ART_SMALL_HEIGHT=64;

    // Bitmap size for album art in media notifications when there are no more than 3 playback actions
    public static final int MEDIA_ART_BIG_WIDTH=128;
    public static final int MEDIA_ART_BIG_HEIGHT=128;

    public static final Bitmap scaleBitmap(int scaleFactor, InputStream is) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeStream(is, null, bmOptions);
        return bitmap;
    }

    public static final int findScaleFactor(int targetW, int targetH, InputStream is) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bmOptions);
        int actualW = bmOptions.outWidth;
        int actualH = bmOptions.outHeight;

        // Determine how much to scale down the image
        return Math.min(actualW/targetW, actualH/targetH);
    }

    public static final Bitmap fetchAndRescaleBitmap(String uri, int width, int height)
            throws IOException {
        URL url = new URL(uri);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setDoInput(true);
        httpConnection.connect();
        InputStream inputStream = httpConnection.getInputStream();
        int scaleFactor = findScaleFactor(width, height, inputStream);
        LogHelper.d(TAG, "Scaling bitmap ", uri, " by factor ", scaleFactor, " to support ",
                width, "x", height, "requested dimension");
        httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setDoInput(true);
        httpConnection.connect();
        inputStream = httpConnection.getInputStream();
        Bitmap bitmap = scaleBitmap(scaleFactor, inputStream);
        return bitmap;
    }

}
