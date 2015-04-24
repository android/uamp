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
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

/**
 * Activity used to display details of the currently playing song, along with playback controls
 * and the playing queue.
 */
public class TvPlaybackActivity extends Activity {
    private static final String TAG = LogHelper.makeLogTag(TvPlaybackActivity.class);

    private MediaBrowser mMediaBrowser;
    private TvPlaybackFragment mPlaybackFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);

        setContentView(R.layout.tv_playback_controls);

        mPlaybackFragment = (TvPlaybackFragment) getFragmentManager()
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
        if (getMediaController() != null) {
            getMediaController().unregisterCallback(mMediaControllerCallback);
        }
        mMediaBrowser.disconnect();

    }

    private MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected");
                    MediaController mediaController = new MediaController(
                            TvPlaybackActivity.this, mMediaBrowser.getSessionToken());
                    setMediaController(mediaController);
                    mediaController.registerCallback(mMediaControllerCallback);

                    MediaMetadata metadata = mediaController.getMetadata();
                    if (metadata != null) {
                        mPlaybackFragment.updateMetadata(metadata);
                        mPlaybackFragment.updatePlaybackState(mediaController.getPlaybackState());
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

    /**
     * Receive callbacks from the MediaController. Here we update our state such as which queue
     * is being shown, the current title and description and the PlaybackState.
     */
    private MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            LogHelper.d(TAG, "onPlaybackStateChanged, state=", state);
            if (mPlaybackFragment == null || state.getState() == PlaybackState.STATE_BUFFERING) {
                return;
            }
            mPlaybackFragment.updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            LogHelper.d(TAG, "onMetadataChanged, title=", metadata.getDescription().getTitle());
            if (mPlaybackFragment == null) {
                return;
            }
            mPlaybackFragment.updateMetadata(metadata);
        }
    };
}
