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
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.UAMPApplication;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.PrefUtils;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicPlayerActivity extends ActionBarActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    public static final String EXTRA_PLAY_QUERY="com.example.android.uamp.PLAY_QUERY";
    private static final String SAVED_MEDIA_ID="com.example.android.uamp.MEDIA_ID";

    private static final int DELAY_MILLIS = 1000;
    private static final double VOLUME_INCREMENT = 0.05;

    private MediaBrowser mMediaBrowser;

    private String mMediaId;
    private String mSearchQuery;

    private VideoCastManager mCastManager;
    private MenuItem mMediaRouteMenuItem;
    private Toolbar mToolbar;
    private VideoCastConsumerImpl mCastConsumer = new VideoCastConsumerImpl() {

        @Override
        public void onFailed(int resourceId, int statusCode) {
            LogHelper.d(TAG, "onFailed ", resourceId, " status ", statusCode);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            LogHelper.d(TAG, "onConnectionSuspended() was called with cause: ", cause);
        }

        @Override
        public void onConnectivityRecovered() {
        }

        @Override
        public void onCastDeviceDetected(final MediaRouter.RouteInfo info) {
            if (!PrefUtils.isFtuShown(MusicPlayerActivity.this)) {
                PrefUtils.setFtuShown(MusicPlayerActivity.this, true);

                LogHelper.d(TAG, "Route is visible: ", info);
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (mMediaRouteMenuItem.isVisible()) {
                            LogHelper.d(TAG, "Cast Icon is visible: ", info.getName());
                            showFtu();
                        }
                    }
                }, DELAY_MILLIS);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        bootstrapFromParameters(savedInstanceState);

        setContentView(R.layout.activity_player);

        // Ensure that Google Play Service is available.
        VideoCastManager.checkGooglePlayServices(this);

        mCastManager = ((UAMPApplication)getApplication()).getCastManager(this);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.main);

        setSupportActionBar(mToolbar);

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);

        mCastManager.reconnectSessionIfPossible(this, false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        mMediaRouteMenuItem = mCastManager.
                addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
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
        if (mCastManager.onDispatchVolumeKeyEvent(event, VOLUME_INCREMENT)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCastManager.removeVideoCastConsumer(mCastConsumer);
        mCastManager.decrementUiCounter();
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
        if (mToolbar != null) {
            if (title == null) {
                title = getString(R.string.app_name);
            }
            mToolbar.setTitle(title);
        }
    }

    private void showFtu() {
        Menu menu = mToolbar.getMenu();
        View view = menu.findItem(R.id.media_route_menu_item).getActionView();
        if (view != null && view instanceof MediaRouteButton) {
            new ShowcaseView.Builder(this)
                    .setTarget(new ViewTarget(view))
                    .setContentTitle(R.string.touch_to_cast)
                    .hideOnTouchOutside()
                    .build();
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
