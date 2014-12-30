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
package com.example.android.uamp.ui;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.MediaStore;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicPlayerActivity extends ActionBarCastActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    public static final String EXTRA_PLAY_QUERY="com.example.android.uamp.PLAY_QUERY";
    private static final String SAVED_MEDIA_ID="com.example.android.uamp.MEDIA_ID";

    private MediaBrowser mMediaBrowser;

    private String mMediaId;
    private String mSearchQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        bootstrapFromParameters(savedInstanceState);

        setContentView(R.layout.activity_player);

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);

        if (savedInstanceState == null) {
            navigateToBrowser(mMediaId);
        }

        initializeToolbar();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mMediaId);
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
        } else {
            // Instead, check if we were started from a "Play XYZ" voice search
            Intent intent = this.getIntent();
            if (intent.getAction() != null
                    && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                    && intent.hasExtra(EXTRA_PLAY_QUERY)) {
                LogHelper.d(TAG, "Starting from play query=", mSearchQuery);
                mSearchQuery = intent.getStringExtra(EXTRA_PLAY_QUERY);
            }
        }
    }

    protected void showPlaybackControls() {
        LogHelper.d(TAG, "showPlaybackControls");
        PlaybackControlsFragment fragment = new PlaybackControlsFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.controls, fragment)
                .commit();
    }

    protected void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        this.mMediaId = mediaId;
        MediaBrowserFragment fragment = new MediaBrowserFragment();
        fragment.setMediaId(mediaId);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        if (mediaId != null) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
//        getSupportActionBar().setHomeButtonEnabled(mediaId == null);
    }

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
            showPlaybackControls();
        } else if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public MediaBrowser getMediaBrowser() {
        return mMediaBrowser;
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar title to ", title);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
    }

    private MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.d(TAG, "onConnected: session token ", mMediaBrowser.getSessionToken());

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            MediaController mediaController = new MediaController(
                    MusicPlayerActivity.this, mMediaBrowser.getSessionToken());
            setMediaController(mediaController);

            // Fire the onConnected() callback on the fragment.
            MediaBrowserFragment fragment =
                    (MediaBrowserFragment) getFragmentManager().findFragmentById(R.id.container);
            fragment.onConnected();

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
