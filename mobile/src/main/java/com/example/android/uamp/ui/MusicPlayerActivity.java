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

import android.app.ActivityManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.NetworkHelper;
import com.example.android.uamp.utils.ResourceHelper;

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

    private String mSearchQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_player);

        initializeToolbar();
        // Since our app icon has the same color as colorPrimary, our entry in the Recents Apps
        // list gets weird. We need to change either the icon or the color of the TaskDescription.
        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
            getTitle().toString(),
            BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_white),
            ResourceHelper.getThemeColor(this, R.attr.colorPrimary, android.R.color.darker_gray));
        setTaskDescription(taskDesc);

        initializeFromParams(savedInstanceState);

        mMediaBrowser = new MediaBrowser(this,
            new ComponentName(this, MusicService.class),
            mConnectionCallback, null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogHelper.d(TAG, "Activity onStart");
        mMediaBrowser.connect();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogHelper.d(TAG, "Activity onStop");
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        if (getMediaController() != null) {
            getMediaController().unregisterCallback(mMediaControllerCallback);
        }
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
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar title to ", title);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
    }

    @Override
    public MediaBrowser getMediaBrowser() {
        return mMediaBrowser;
    }

    protected void initializeFromParams(Bundle savedInstanceState) {
        String mediaId = null;
        // check if we were started from a "Play XYZ" voice search
        Intent intent = this.getIntent();
        if (intent.getAction() != null
            && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            && intent.hasExtra(EXTRA_PLAY_QUERY)) {
            LogHelper.d(TAG, "Starting from play query=", mSearchQuery);
            mSearchQuery = intent.getStringExtra(EXTRA_PLAY_QUERY);
        } else {
            if (savedInstanceState != null) {
                // If there is a saved media ID, use it
                mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
            }
        }
        navigateToBrowser(mediaId);
    }

    private void showPlaybackControls() {
        LogHelper.d(TAG, "showPlaybackControls");
        PlaybackControlsFragment controlsFragment = (PlaybackControlsFragment)
            getFragmentManager().findFragmentById(R.id.controls);
        if (controlsFragment == null && NetworkHelper.isOnline(this)) {
            PlaybackControlsFragment fragment = new PlaybackControlsFragment();
            getFragmentManager().beginTransaction()
                .replace(R.id.controls, fragment)
                .commit();
        }
    }

    private void hidePlaybackControls() {
        LogHelper.d(TAG, "hidePlaybackControls");
        PlaybackControlsFragment controlsFragment = (PlaybackControlsFragment)
                getFragmentManager().findFragmentById(R.id.controls);
        if (controlsFragment != null) {
            getFragmentManager().beginTransaction()
                    .remove(controlsFragment)
                    .commit();
        }
    }

    private void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        MediaBrowserFragment fragment = getBrowseFragment();

        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.container, fragment);
            if (mediaId != null) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    public String getMediaId() {
        MediaBrowserFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getFragmentManager().findFragmentById(R.id.container);
    }

    private boolean shouldShowControls() {
        MediaController mediaController = getMediaController();
        if (mediaController == null ||
            mediaController.getMetadata() == null ||
            mediaController.getPlaybackState() == null) {
            return false;
        }
        switch (mediaController.getPlaybackState().getState()) {
            case PlaybackState.STATE_ERROR:
            case PlaybackState.STATE_NONE:
            case PlaybackState.STATE_STOPPED:
                return false;
            default:
                return true;
        }
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
            mediaController.registerCallback(mMediaControllerCallback);
            getBrowseFragment().onConnected();

            if (shouldShowControls()) {
                showPlaybackControls();
            } else {
                LogHelper.d(TAG, "connectionCallback.onConnected: " +
                    "hiding controls because metadata is null");
                hidePlaybackControls();
            }

            PlaybackControlsFragment playbackFragment = (PlaybackControlsFragment)
                    getFragmentManager().findFragmentById(R.id.controls);
            if (playbackFragment != null) {
                playbackFragment.onConnected();
            }

            if (mSearchQuery != null) {
                // If there is a bootstrap parameter to start from a search query, we
                // send it to the media session and set it to null, so it won't play again
                // when the activity is stopped/started or recreated:
                mediaController.getTransportControls().playFromSearch(mSearchQuery, null);
                mSearchQuery = null;
            }
        }

        @Override
        public void onConnectionFailed() {
            LogHelper.d(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            LogHelper.d(TAG, "onConnectionSuspended");
            getMediaController().unregisterCallback(mMediaControllerCallback);
            setMediaController(null);
        }
    };

    // Callback that ensures that we are showing the controls
    private final MediaController.Callback mMediaControllerCallback =
            new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            // If the service is already active and in a "playback-able" state
            // (not NONE and not STOPPED), we need to set the proper playback controls:
            if (shouldShowControls()) {
                showPlaybackControls();
            } else {
                LogHelper.d(TAG, "mediaControllerCallback.onPlaybackStateChanged: " +
                    "hiding controls because state is ",
                    state == null ? "null" : state.getState());
                hidePlaybackControls();
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (shouldShowControls()) {
                showPlaybackControls();
            } else {
                LogHelper.d(TAG, "mediaControllerCallback.onMetadataChanged: " +
                        "hiding controls because metadata is null");
                hidePlaybackControls();
            }
        }
    };
}
