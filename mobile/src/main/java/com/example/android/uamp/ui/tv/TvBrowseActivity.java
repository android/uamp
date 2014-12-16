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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class TvBrowseActivity extends Activity
        implements TvBrowseFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(TvBrowseActivity.class);
    public static final String EXTRA_PLAY_QUERY="com.example.android.uamp.PLAY_QUERY";
    private static final String SAVED_MEDIA_ID="com.example.android.uamp.MEDIA_ID";
    private static final String BROWSE_TITLE = "com.example.android.uamp.BROWSE_TITLE";

    private MediaBrowser mMediaBrowser;

    private String mMediaId;
    private String mSearchQuery;
    private String mBrowseTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        bootstrapFromParameters(savedInstanceState);

        setContentView(R.layout.tv_activity_player);

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
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

    protected void bootstrapFromParameters(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // If there is a saved media ID, use it
            mMediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
            mBrowseTitle = savedInstanceState.getString(BROWSE_TITLE);
        } else {
            Intent intent = this.getIntent();
            String mediaId = intent.getStringExtra(TvBrowseActivity.SAVED_MEDIA_ID);
            if (mediaId != null) {
                mMediaId = mediaId;
                mBrowseTitle = intent.getStringExtra(TvBrowseActivity.BROWSE_TITLE);
                return;
            } else if (intent.getAction() != null &&
                    intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) &&
                    intent.hasExtra(EXTRA_PLAY_QUERY)) {
                // Instead, check if we were started from a "Play XYZ" voice search
                LogHelper.d(TAG, "Starting from play query=", mSearchQuery);
                mSearchQuery = intent.getStringExtra(EXTRA_PLAY_QUERY);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void showPlaybackControls() {
        LogHelper.d(TAG, "showPlaybackControls");
    }

    protected void navigateToPlayingQueue() {
        LogHelper.d(TAG, "navigateToPlayingQueue");
    }

    protected void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        TvBrowseFragment fragment =
                (TvBrowseFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        fragment.setMediaId(mediaId);
        if (MediaIDHelper.MEDIA_ID_ROOT.equals(mediaId)) {
            mBrowseTitle = getResources().getString(R.string.home_title);
        }
        fragment.setTitle(mBrowseTitle);
        this.mMediaId = mediaId;
    }

    @Override
    public void onMediaItemClicked(MediaBrowser.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemClicked, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            Intent intent = new Intent(this, MediaDetailsActivity.class);
            intent.putExtra(MediaDetailsFragment.MEDIA_ITEM_EXTRA, item);
            startActivity(intent);
        } else if (item.isBrowsable()) {
            Intent intent = new Intent(this, TvBrowseActivity.class);
            intent.putExtra(TvBrowseActivity.SAVED_MEDIA_ID, item.getMediaId());
            intent.putExtra(TvBrowseActivity.BROWSE_TITLE, item.getDescription().getTitle());
            startActivity(intent);
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public MediaBrowser getMediaBrowser() {
        return mMediaBrowser;
    }

    private MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected: session token ",
                            mMediaBrowser.getSessionToken());

                    if (mMediaBrowser.getSessionToken() == null) {
                        throw new IllegalArgumentException("No Session token");
                    }

                    MediaController mediaController = new MediaController(
                            TvBrowseActivity.this, mMediaBrowser.getSessionToken());
                    setMediaController(mediaController);

                    navigateToBrowser(mMediaId);

                    if (mSearchQuery != null) {
                        // If there is a bootstrap parameter to start from a search query, we
                        // send it to the media session and set it to null, so it won't play again
                        // when the activity is stopped/started or recreated:
                        mediaController.getTransportControls().playFromSearch(mSearchQuery, null);
                        mSearchQuery = null;
                    }

                    // If the service is already active and in a "playback-able" state
                    // (not NONE and not STOPPED), we need to set the proper playback controls:
                    PlaybackState state = mediaController.getPlaybackState();
                    if (state != null && state.getState() != PlaybackState.STATE_NONE &&
                            state.getState() != PlaybackState.STATE_STOPPED) {
                        showPlaybackControls();
                    }
                }

                @Override
                public void onConnectionFailed() {
                    LogHelper.d(TAG, "onConnectionFailed");
                }

                @Override
                public void onConnectionSuspended() {
                    LogHelper.d(TAG, "onConnectionSuspended");
                    setMediaController(null);
                }
            };
}
