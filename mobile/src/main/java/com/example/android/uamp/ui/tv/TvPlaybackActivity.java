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
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

/**
 * Activity used to display details of the currently playing song, along with playback controls
 * and the playing queue.
 */
public class TvPlaybackActivity extends FragmentActivity {
    private static final String TAG = LogHelper.makeLogTag(TvPlaybackActivity.class);

    private MediaBrowserCompat mMediaBrowser;
    private TvPlaybackFragment mPlaybackFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);

        setContentView(R.layout.tv_playback_controls);

        mPlaybackFragment = (TvPlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_controls_fragment);
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
        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(mMediaControllerCallback);
        }
        mMediaBrowser.disconnect();

    }

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected");
                    try {
                        MediaControllerCompat mediaController = new MediaControllerCompat(
                                TvPlaybackActivity.this, mMediaBrowser.getSessionToken());
                        setSupportMediaController(mediaController);
                        mediaController.registerCallback(mMediaControllerCallback);

                        MediaMetadataCompat metadata = mediaController.getMetadata();
                        if (metadata != null) {
                            mPlaybackFragment.updateMetadata(metadata);
                            mPlaybackFragment.updatePlaybackState(mediaController.getPlaybackState());
                        }
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
                    getSupportMediaController().unregisterCallback(mMediaControllerCallback);
                    setSupportMediaController(null);
                }
            };

    /**
     * Receive callbacks from the MediaController. Here we update our state such as which queue
     * is being shown, the current title and description and the PlaybackState.
     */
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.d(TAG, "onPlaybackStateChanged, state=", state);
            if (mPlaybackFragment == null || state.getState() == PlaybackStateCompat.STATE_BUFFERING) {
                return;
            }
            mPlaybackFragment.updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            LogHelper.d(TAG, "onMetadataChanged, title=", metadata.getDescription().getTitle());
            if (mPlaybackFragment == null) {
                return;
            }
            mPlaybackFragment.updateMetadata(metadata);
        }
    };
}
