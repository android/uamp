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
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.util.Log;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

public class MusicPlaybackOverlayFragment extends PlaybackOverlayFragment {

    private static final String TAG = LogHelper.makeLogTag(MusicPlaybackOverlayFragment.class);

    private Activity mActivity;

    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int MIN_UPDATE_PERIOD = 16;
    private static final int SIMULATED_BUFFERED_TIME = 10000;
    private static final int FF_RW_SPEED_MS = 5000;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mRelatedAdapter;

    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow mPlaybackControlsRow;

    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlaybackControlsRow.ThumbsUpAction mThumbsUpAction;
    private PlaybackControlsRow.ThumbsDownAction mThumbsDownAction;
    private PlaybackControlsRow.RepeatAction mRepeatAction;
    private PlaybackControlsRow.ShuffleAction mShuffleAction;
    private PlaybackControlsRow.HighQualityAction mHighQualityAction;
    private PlaybackControlsRow.ClosedCaptioningAction mClosedCaptioningAction;

    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    private OnActionListener mOnActionListener = new OnActionListener();
    private long mDuration;
    private long mPosition;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // Keep controls visible all the time.
        setFadingEnabled(false);
        setupRows();
    }

    private void setupRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        // Add presenter for playback controls.
        PlaybackControlsRowPresenter playbackControlsRowPresenter =
                new PlaybackControlsRowPresenter();
        playbackControlsRowPresenter.setOnActionClickedListener(mOnActionListener);
        playbackControlsRowPresenter.setSecondaryActionsHidden(false);
        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        // Add presenter for related content cards.
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        mRowsAdapter = new ArrayObjectAdapter(ps);

        addPlaybackControlsRow();
        addRelatedContentRow();

        setAdapter(mRowsAdapter);
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow();
        mRowsAdapter.add(mPlaybackControlsRow);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(mActivity);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(mActivity);
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(mActivity);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(mActivity);
        mRewindAction = new PlaybackControlsRow.RewindAction(mActivity);
        mPrimaryActionsAdapter.add(mSkipPreviousAction);
        mPrimaryActionsAdapter.add(mRewindAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mFastForwardAction);
        mPrimaryActionsAdapter.add(mSkipNextAction);

        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(mActivity);
        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(mActivity);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(mActivity);
        mShuffleAction = new PlaybackControlsRow.ShuffleAction(mActivity);
        mHighQualityAction = new PlaybackControlsRow.HighQualityAction(mActivity);
        mClosedCaptioningAction = new PlaybackControlsRow.ClosedCaptioningAction(mActivity);

        mSecondaryActionsAdapter.add(mThumbsUpAction);
        mSecondaryActionsAdapter.add(mThumbsDownAction);
        mSecondaryActionsAdapter.add(mRepeatAction);
        mSecondaryActionsAdapter.add(mShuffleAction);
        mSecondaryActionsAdapter.add(mHighQualityAction);
        mSecondaryActionsAdapter.add(mClosedCaptioningAction);
    }

    private void addRelatedContentRow() {
        mRelatedAdapter = new ArrayObjectAdapter(new CardPresenter());
        HeaderItem headerItem = new HeaderItem(0, getString(R.string.related_content_header), null);
        ListRow relatedContentRow = new ListRow(headerItem, mRelatedAdapter);
        mRowsAdapter.add(relatedContentRow);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopProgressAutomation();
    }

    private void startProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            Log.e(TAG, "startProgressAutomation: Progress should not already be running");
        } else {
            Log.d(TAG, "startProgressAutomation");
        }
        stopProgressAutomation();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                int updatePeriod = getUpdatePeriod();
                int currentTime = mPlaybackControlsRow.getCurrentTime() + updatePeriod;
                int totalTime = mPlaybackControlsRow.getTotalTime();

                if (totalTime > 0 && totalTime <= currentTime) {
                    return; //TODO(cartland): Make sure playback skips to next.
                }

                mDuration = totalTime;
                mPosition = currentTime;

                updatePlaybackRow();
                mHandler.postDelayed(this, updatePeriod);
            }
        };
        mHandler.postDelayed(mRunnable, getUpdatePeriod());
    }

    private void stopProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    // Returns time in ms between progress bar UI updates.
    private int getUpdatePeriod() {
        if (getView() == null || mPlaybackControlsRow.getTotalTime() <= 0) {
            return DEFAULT_UPDATE_PERIOD;
        }
        int updatePeriod = mPlaybackControlsRow.getTotalTime() / getView().getWidth();
        return Math.max(MIN_UPDATE_PERIOD, updatePeriod);
    }

    public void onPlaybackStateChanged(PlaybackState state) {
        LogHelper.d(TAG, "onPlaybackStateChanged ", state);
        if (getActivity() == null) {
            LogHelper.w(TAG, "onPlaybackStateChanged called when getActivity null," +
                    "this should not happen if the callback was properly unregistered. Ignoring.");
            return;
        }
        if (state == null) {
            return;
        }
        boolean enablePlay = false;
        switch (state.getState()) {
            case PlaybackState.STATE_PAUSED:
            case PlaybackState.STATE_STOPPED:
                enablePlay = true;
                break;
            case PlaybackState.STATE_ERROR:
                LogHelper.e(TAG, "error playbackstate: ", state.getErrorMessage());
                // TODO: show the error message to the user
                break;
        }

        Log.d(TAG, "onPlaybackStateChanged enablePlay=" + enablePlay);
        if (enablePlay) { // Paused or Stopped
            displayPaused();
        } else {
            displayPlaying();
        }

        mPosition = state.getPosition();
        updatePlaybackRow();
    }

    public void onMetadataChanged(MediaMetadata metadata) {
        mDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
    }

    private void displayPlaying() {
        Log.d(TAG, "displayPlaying()");
        startProgressAutomation();
        mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PAUSE);
        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
    }

    private void displayPaused() {
        Log.d(TAG, "displayPaused()");
        stopProgressAutomation();
        mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PLAY);
        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
    }

    private void updatePlaybackRow() {
        mPlaybackControlsRow.setTotalTime((int) mDuration);
        mPlaybackControlsRow.setCurrentTime((int) mPosition);
        mPlaybackControlsRow.setBufferedProgress((int) (mPosition + SIMULATED_BUFFERED_TIME));
    }

    /**
     * Callback for user initiated button clicks.
     */
    private class OnActionListener implements OnActionClickedListener {
        public void onActionClicked(Action action) {
            if (action.getId() == mPlayPauseAction.getId()) {
                if (mPlayPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.PLAY) {
                    play();
                } else {
                    pause();
                }
            } else if (action.getId() == mSkipNextAction.getId()) {
                next();
            } else if (action.getId() == mSkipPreviousAction.getId()) {
                previous();
            } else if (action.getId() == mFastForwardAction.getId()) {
                fastForward();
            } else if (action.getId() == mRewindAction.getId()) {
                fastRewind();
            }
            if (action instanceof PlaybackControlsRow.MultiAction) {
                ((PlaybackControlsRow.MultiAction) action).nextIndex();
                notifyChanged(action);
            }
        }
    }

    private void play() {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().play();
        }
    }

    private void pause() {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().pause();
        }
    }

    private void next() {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().skipToNext();
        }
    }

    private void previous() {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().skipToPrevious();
        }
    }

    private void fastForward() {
        Log.d(TAG, "current time: " + mPlaybackControlsRow.getCurrentTime());
        long currentTime = mPlaybackControlsRow.getCurrentTime() + FF_RW_SPEED_MS;
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            if (currentTime > (int) mDuration) {
                currentTime = (int) mDuration;
            }
            controller.getTransportControls().seekTo(currentTime);
        }
    }

    private void fastRewind() {
        Log.d(TAG, "current time: " + mPlaybackControlsRow.getCurrentTime());
        int currentTime = mPlaybackControlsRow.getCurrentTime() - FF_RW_SPEED_MS;
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            if (currentTime < 0 || currentTime > (int) mDuration) {
                currentTime = 0;
            }
            controller.getTransportControls().seekTo(currentTime);
        }
    }

    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = mSecondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
    }
}
