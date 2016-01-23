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
package com.example.android.uamp.ui.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

/**
 * Main activity for the Android TV user interface.
 */
public class TvBrowseActivity extends FragmentActivity
        implements TvBrowseFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(TvBrowseActivity.class);
    public static final String SAVED_MEDIA_ID="com.example.android.uamp.MEDIA_ID";
    public static final String BROWSE_TITLE = "com.example.android.uamp.BROWSE_TITLE";

    private MediaBrowserCompat mMediaBrowser;

    private String mMediaId;
    private String mBrowseTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.tv_activity_player);

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mMediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mMediaId);
            outState.putString(BROWSE_TITLE, mBrowseTitle);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogHelper.d(TAG, "Activity onStart");
        mMediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogHelper.d(TAG, "Activity onStop");
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }
    
    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, TvBrowseActivity.class));
        return true;
    }

    protected void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        TvBrowseFragment fragment =
                (TvBrowseFragment) getSupportFragmentManager().findFragmentById(R.id.main_browse_fragment);
        fragment.initializeWithMediaId(mediaId);
        mMediaId = mediaId;
        if (mediaId == null) {
            mBrowseTitle = getResources().getString(R.string.home_title);
        }
        fragment.setTitle(mBrowseTitle);
    }

    @Override
    public MediaBrowserCompat getMediaBrowser() {
        return mMediaBrowser;
    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected: session token ",
                            mMediaBrowser.getSessionToken());
                    try {
                        MediaControllerCompat mediaController = new MediaControllerCompat(
                                TvBrowseActivity.this, mMediaBrowser.getSessionToken());
                        setSupportMediaController(mediaController);
                        navigateToBrowser(mMediaId);
                    } catch (RemoteException e) {
                        LogHelper.e(TAG, e, "could not connect media controller");
                    }
                }

                @Override
                public void onConnectionFailed() {
                    LogHelper.d(TAG, "onConnectionFailed");
                }

                @Override
                public void onConnectionSuspended() {
                    LogHelper.d(TAG, "onConnectionSuspended");
                    setSupportMediaController(null);
                }
            };
}
