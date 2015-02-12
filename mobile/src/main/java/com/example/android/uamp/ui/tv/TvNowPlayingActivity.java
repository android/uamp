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
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

import java.util.List;

public class TvNowPlayingActivity extends Activity {

    private static final String TAG = LogHelper.makeLogTag(TvNowPlayingActivity.class);

    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;

    private ImageView mImageView;
    private MusicPlaybackOverlayFragment mFragment;
    private MediaBrowser.MediaItem mMediaItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_now_playing);

        mMediaBrowser = new MediaBrowser(this,
                new ComponentName(this, MusicService.class),
                mConnectionCallback, null);

        mImageView = (ImageView) findViewById(R.id.now_playing_image);
        mImageView.setImageDrawable(getDrawable(R.drawable.banner_tv));

        mFragment = (MusicPlaybackOverlayFragment)
                getFragmentManager().findFragmentById(R.id.playback_controls_fragment);

        mMediaItem = getIntent().getParcelableExtra(MediaDetailsFragment.MEDIA_ITEM_EXTRA);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
        if (mMediaController != null) {
            mMediaController.registerCallback(mControllerCallback);
        } else {
            Log.d(TAG, "MediaController is null");
        }
        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
        }
        mMediaBrowser.disconnect();
    }

    private MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
                @Override
                public void onConnected() {
                    LogHelper.d(TAG, "onConnected: session token " + mMediaBrowser.getSessionToken());

                    if (mMediaBrowser.getSessionToken() == null) {
                        throw new IllegalArgumentException("No Session token");
                    }

                    mMediaController = new MediaController(
                            TvNowPlayingActivity.this, mMediaBrowser.getSessionToken());
                    setMediaController(mMediaController);

                    mMediaController.registerCallback(mControllerCallback);
                    mControllerCallback.onMetadataChanged(mMediaController.getMetadata());
                    mControllerCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());

                    if (mMediaItem != null) {
                        mMediaController.getTransportControls().playFromMediaId(mMediaItem.getMediaId(), null);
                        mMediaItem = null;
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

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaController.Callback mControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            Log.d(TAG, "onPlaybackStateChanged() state=" + state);
            mFragment.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            Log.d(TAG, "onMetadataChanged()");
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                return;
            }
            Bitmap bitmap = metadata.getDescription().getIconBitmap();
            Log.d(TAG, "onMetadataChanged bitmap=" + bitmap);
            mImageView.setImageBitmap(bitmap);
            mFragment.onMetadataChanged(metadata);
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
        }
    };
}
