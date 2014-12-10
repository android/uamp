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

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;

import com.example.android.uamp.utils.BitmapHelper;
import com.example.android.uamp.utils.LogHelper;

import java.io.IOException;

/**
 * Implements a basic cache of album arts, with async loading support.
 */
public class AlbumArtCache {
    private static final String TAG = LogHelper.makeLogTag(AlbumArtCache.class);

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 1024*1024;

    private final LruCache<String, Bitmap> mAlbumArtCache;

    public AlbumArtCache() {
        // holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes:
        mAlbumArtCache = new LruCache<String, Bitmap>(MAX_ALBUM_ART_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }


    public Bitmap get(String artUrl) {
        return mAlbumArtCache.get(artUrl);
    }

    public void fetch(final String artUrl, final FetchListener listener) {
        Bitmap bitmap = get(artUrl);
        if (bitmap != null) {
            LogHelper.d(TAG, "getOrFetch: album art is in cache, using it", artUrl);
            listener.onFetched(artUrl, bitmap);
            return;
        }
        LogHelper.d(TAG, "getOrFetch: starting asynctask to fetch ", artUrl);

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void[] objects) {
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapHelper.fetchAndRescaleBitmap(artUrl,
                            BitmapHelper.MEDIA_ART_BIG_WIDTH, BitmapHelper.MEDIA_ART_BIG_HEIGHT);
                    mAlbumArtCache.put(artUrl, bitmap);
                } catch (IOException e) {
                    listener.onError(artUrl, e);
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null) {
                    listener.onError(artUrl, new IllegalArgumentException("got null bitmap"));
                } else {
                    listener.onFetched(artUrl, bitmap);
                }
            }
        }.execute();
    }

    public static abstract class FetchListener {
        public abstract void onFetched(String artUrl, Bitmap result);
        public void onError(String artUrl, Exception e) {
            LogHelper.e(TAG, e, "AlbumArtFetchListener: error while downloading " + artUrl);
        }
    }


}
