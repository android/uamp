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

import android.content.Context;
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
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.util.Log;

import com.example.android.uamp.utils.LogHelper;

import java.util.Collections;
import java.util.List;

public class MusicPlaybackOverlayFragment extends PlaybackOverlayFragment {

    private static final String TAG = LogHelper.makeLogTag(MusicPlaybackOverlayFragment.class);

    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int MIN_UPDATE_PERIOD = 16;
    private static final int SIMULATED_BUFFERED_TIME = 10000;

    private static final int STANDARD_ACTIONS = 3;

    private ArrayObjectAdapter mRowsAdapter;

    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow mPlaybackControlsRow;

    private List<PlaybackState.CustomAction> mCustomActions = Collections.emptyList();

    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    private OnActionListener mOnActionListener = new OnActionListener();
    private long mDuration;
    private long mPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setFadingEnabled(true);
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

        mRowsAdapter = new ArrayObjectAdapter(ps);

        addPlaybackControlsRow();

        setAdapter(mRowsAdapter);
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow();
        mRowsAdapter.add(mPlaybackControlsRow);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(getActivity());
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(getActivity());
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(getActivity());
        mPrimaryActionsAdapter.add(mSkipPreviousAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mSkipNextAction);
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
                    return;
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
        updateCustomActions(state.getCustomActions());
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

    private boolean equalsCustomActionLists(List<PlaybackState.CustomAction> list1,
                                            List<PlaybackState.CustomAction> list2) {
        // check if customActions list has changed:
        if (list1.size() != list2.size()) {
            return false;
        }

        boolean equals = true;
        for (int i=0; equals && i<list1.size(); i++) {
            PlaybackState.CustomAction a1 = list1.get(i);
            PlaybackState.CustomAction a2 = list2.get(i);
            if (!a1.getAction().equals(a2.getAction()) ||
                a1.getIcon() != a2.getIcon() ||
                a1.getName().equals(a2.getName())) {
                equals = false;
            }
        }
        return equals;
    }

    private synchronized void updateCustomActions(List<PlaybackState.CustomAction> customActions) {
        if (customActions == null) {
            customActions = Collections.emptyList();
        }
        if (equalsCustomActionLists(customActions, mCustomActions)) {
            // don't do anything if actions have not changed
            return;
        }
        int toRemove = mPrimaryActionsAdapter.size() - STANDARD_ACTIONS;
        if (toRemove > 0) {
            mPrimaryActionsAdapter.removeItems(STANDARD_ACTIONS, toRemove);
        }
        for (int i=0; i<customActions.size(); i++) {
            mPrimaryActionsAdapter.add(new CustomActionWidget(i, getActivity(),
                customActions.get(i)));
        }
        mCustomActions = customActions;
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

    private void sendCustomAction(PlaybackState.CustomAction customAction) {
        MediaController controller = getActivity().getMediaController();
        if (controller != null) {
            controller.getTransportControls().sendCustomAction(customAction, null);
        }
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
            }
            if (action instanceof CustomActionWidget) {
                sendCustomAction(((CustomActionWidget) action).getCustomAction());
            }
        }
    }

    /**
     * An action widget displaying an icon for a media session custom action.
     */
    private static class CustomActionWidget extends Action {
        PlaybackState.CustomAction customAction;
        public CustomActionWidget(int id, Context context,
              PlaybackState.CustomAction customAction) {
            super(id, customAction.getName(), null, context.getDrawable(customAction.getIcon()));
            this.customAction = customAction;
        }

        public PlaybackState.CustomAction getCustomAction() {
            return customAction;
        }
    }
}
