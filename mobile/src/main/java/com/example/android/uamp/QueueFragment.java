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

import android.app.Fragment;
import android.content.ComponentName;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.example.android.uamp.utils.LogHelper;

import java.util.List;

/**
 * A class that shows the Media Queue to the user.
 */
public class QueueFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(QueueFragment.class);

    private ImageButton mSkipNext;
    private ImageButton mSkipPrevious;
    private ImageButton mPlayPause;

    private MediaBrowser mMediaBrowser;
    private MediaController.TransportControls mTransportControls;
    private MediaController mMediaController;
    private PlaybackState mPlaybackState;

    private QueueAdapter mQueueAdapter;

    private MediaBrowser.ConnectionCallback mConnectionCallback =
            new MediaBrowser.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.d(TAG, "onConnected: session token ", mMediaBrowser.getSessionToken());

            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            mMediaController = new MediaController(getActivity(),
                    mMediaBrowser.getSessionToken());
            mTransportControls = mMediaController.getTransportControls();
            mMediaController.registerCallback(mSessionCallback);

            getActivity().setMediaController(mMediaController);
            mPlaybackState = mMediaController.getPlaybackState();

            List<MediaSession.QueueItem> queue = mMediaController.getQueue();
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
            onPlaybackStateChanged(mPlaybackState);
        }

        @Override
        public void onConnectionFailed() {
            LogHelper.d(TAG, "onConnectionFailed");
        }

        @Override
        public void onConnectionSuspended() {
            LogHelper.d(TAG, "onConnectionSuspended");
            mMediaController.unregisterCallback(mSessionCallback);
            mTransportControls = null;
            mMediaController = null;
            getActivity().setMediaController(null);
        }
    };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaController.Callback mSessionCallback = new MediaController.Callback() {

        @Override
        public void onSessionDestroyed() {
            LogHelper.d(TAG, "Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (state == null) {
                return;
            }
            LogHelper.d(TAG, "Received playback state change to state ", state.getState());
            mPlaybackState = state;
            QueueFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            LogHelper.d(TAG, "onQueueChanged ", queue);
            if (queue != null) {
                mQueueAdapter.clear();
                mQueueAdapter.notifyDataSetInvalidated();
                mQueueAdapter.addAll(queue);
                mQueueAdapter.notifyDataSetChanged();
            }
        }
    };

    public static QueueFragment newInstance() {
        return new QueueFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mSkipPrevious = (ImageButton) rootView.findViewById(R.id.skip_previous);
        mSkipPrevious.setEnabled(false);
        mSkipPrevious.setOnClickListener(mButtonListener);

        mSkipNext = (ImageButton) rootView.findViewById(R.id.skip_next);
        mSkipNext.setEnabled(false);
        mSkipNext.setOnClickListener(mButtonListener);

        mPlayPause = (ImageButton) rootView.findViewById(R.id.play_pause);
        mPlayPause.setEnabled(true);
        mPlayPause.setOnClickListener(mButtonListener);

        mQueueAdapter = new QueueAdapter(getActivity());

        ListView mListView = (ListView) rootView.findViewById(R.id.list_view);
        mListView.setAdapter(mQueueAdapter);
        mListView.setFocusable(true);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaSession.QueueItem item = mQueueAdapter.getItem(position);
                mTransportControls.skipToQueueItem(item.getQueueId());
            }
        });

        mMediaBrowser = new MediaBrowser(getActivity(),
                new ComponentName(getActivity(), MusicService.class),
                mConnectionCallback, null);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mSessionCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }


    private void onPlaybackStateChanged(PlaybackState state) {
        LogHelper.d(TAG, "onPlaybackStateChanged ", state);
        if (state == null) {
            return;
        }
        mQueueAdapter.setActiveQueueItemId(state.getActiveQueueItemId());
        mQueueAdapter.notifyDataSetChanged();
        boolean enablePlay = false;
        StringBuilder statusBuilder = new StringBuilder();
        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                statusBuilder.append("playing");
                enablePlay = false;
                break;
            case PlaybackState.STATE_PAUSED:
                statusBuilder.append("paused");
                enablePlay = true;
                break;
            case PlaybackState.STATE_STOPPED:
                statusBuilder.append("ended");
                enablePlay = true;
                break;
            case PlaybackState.STATE_ERROR:
                statusBuilder.append("error: ").append(state.getErrorMessage());
                break;
            case PlaybackState.STATE_BUFFERING:
                statusBuilder.append("buffering");
                break;
            case PlaybackState.STATE_NONE:
                statusBuilder.append("none");
                enablePlay = false;
                break;
            case PlaybackState.STATE_CONNECTING:
                statusBuilder.append("connecting");
                break;
            default:
                statusBuilder.append(mPlaybackState);
        }
        statusBuilder.append(" -- At position: ").append(state.getPosition());
        LogHelper.d(TAG, statusBuilder.toString());

        if (enablePlay) {
            mPlayPause.setImageDrawable(
                    getActivity().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        } else {
            mPlayPause.setImageDrawable(getActivity().getDrawable(R.drawable.ic_pause_white_24dp));
        }

        mSkipPrevious.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);
        mSkipNext.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);

        LogHelper.d(TAG, "Queue From MediaController *** Title " +
                mMediaController.getQueueTitle() + "\n: Queue: " + mMediaController.getQueue() +
                "\n Metadata " + mMediaController.getMetadata());
    }

    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int state = mPlaybackState == null ?
                    PlaybackState.STATE_NONE : mPlaybackState.getState();
            switch (v.getId()) {
                case R.id.play_pause:
                    LogHelper.d(TAG, "Play button pressed, in state " + state);
                    if (state == PlaybackState.STATE_PAUSED ||
                            state == PlaybackState.STATE_STOPPED ||
                            state == PlaybackState.STATE_NONE) {
                        playMedia();
                    } else if (state == PlaybackState.STATE_PLAYING) {
                        pauseMedia();
                    }
                    break;
                case R.id.skip_previous:
                    LogHelper.d(TAG, "Start button pressed, in state " + state);
                    skipToPrevious();
                    break;
                case R.id.skip_next:
                    skipToNext();
                    break;
            }
        }
    };

    private void playMedia() {
        if (mTransportControls != null) {
            mTransportControls.play();
        }
    }

    private void pauseMedia() {
        if (mTransportControls != null) {
            mTransportControls.pause();
        }
    }

    private void skipToPrevious() {
        if (mTransportControls != null) {
            mTransportControls.skipToPrevious();
        }
    }

    private void skipToNext() {
        if (mTransportControls != null) {
            mTransportControls.skipToNext();
        }
    }
}
