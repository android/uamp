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

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouter;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.PrefUtils;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;

/**
 * Main activity for the music player.
 */
public class MusicPlayerActivity extends ActionBarActivity
        implements BrowseFragment.FragmentDataHelper {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final int DELAY_MILLIS = 1000;
    private static final double VOLUME_INCREMENT = 0.05;

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
        setContentView(R.layout.activity_player);

        // Ensure that Google Play Service is available.
        VideoCastManager.checkGooglePlayServices(this);

        mCastManager = ((UAMPApplication)getApplication()).getCastManager(this);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getString(R.string.app_name));
        mToolbar.setLogo(R.drawable.ic_launcher);
        mToolbar.inflateMenu(R.menu.main);

        setSupportActionBar(mToolbar);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, BrowseFragment.newInstance(null))
                    .commit();
        }

        mCastManager.reconnectSessionIfPossible(this, false);
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

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem item) {
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
            QueueFragment queueFragment = QueueFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, queueFragment)
                    .addToBackStack(null)
                    .commit();
        } else if (item.isBrowsable()) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, BrowseFragment.newInstance(item.getMediaId()))
                    .addToBackStack(null)
                    .commit();
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
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
}
