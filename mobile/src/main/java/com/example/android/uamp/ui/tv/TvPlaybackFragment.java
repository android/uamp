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

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.app.PlaybackOverlaySupportFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.utils.LogHelper;

import java.util.List;

/*
 * Show details of the currently playing song, along with playback controls and the playing queue.
 */
public class TvPlaybackFragment extends PlaybackOverlaySupportFragment {
    private static final String TAG = LogHelper.makeLogTag(TvPlaybackFragment.class);

    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_DARK;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 16;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    protected PlayPauseAction mPlayPauseAction;
    private SkipNextAction mSkipNextAction;
    private SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow mPlaybackControlsRow;
    private List <MediaSessionCompat.QueueItem> mPlaylistQueue;
    private int mDuration;
    private Handler mHandler;
    private Runnable mRunnable;

    private long mLastPosition;
    private long mLastPositionUpdateTime;

    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter mListRowAdapter;
    private ListRow mListRow;

    private ClassPresenterSelector mPresenterSelector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.i(TAG, "onCreate");

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mHandler = new Handler();
        mListRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        mPresenterSelector = new ClassPresenterSelector();
        mRowsAdapter = new ArrayObjectAdapter(mPresenterSelector);

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);
    }

    private void initializePlaybackControls(MediaMetadataCompat metadata) {
        setupRows();
        addPlaybackControlsRow(metadata);
        setAdapter(mRowsAdapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void setupRows() {
        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        playbackControlsRowPresenter = new PlaybackControlsRowPresenter(
                new DescriptionPresenter());

        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (getActivity() == null) {
                    return;
                }
                MediaControllerCompat controller = getActivity().getSupportMediaController();
                if (controller == null) {
                    return;
                }
                MediaControllerCompat.TransportControls controls = controller.getTransportControls();
                if (action.getId() == mPlayPauseAction.getId()) {
                    if (mPlayPauseAction.getIndex() == PlayPauseAction.PLAY) {
                        controls.play();
                    } else {
                        controls.pause();
                    }
                } else if (action.getId() == mSkipNextAction.getId()) {
                    controls.skipToNext();
                    resetPlaybackRow();
                } else if (action.getId() == mSkipPreviousAction.getId()) {
                    controls.skipToPrevious();
                    resetPlaybackRow();
                }

                if (action instanceof PlaybackControlsRow.MultiAction) {
                    ((PlaybackControlsRow.MultiAction) action).nextIndex();
                    notifyChanged(action);
                }
            }
        });

        mPresenterSelector.addClassPresenter(PlaybackControlsRow.class,
                playbackControlsRowPresenter);
    }

    private void addPlaybackControlsRow(MediaMetadataCompat metadata) {

        mPlaybackControlsRow = new PlaybackControlsRow(new MutableMediaMetadataHolder(metadata));
        mRowsAdapter.add(mPlaybackControlsRow);

        resetPlaybackRow();

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);

        mPlayPauseAction = new PlayPauseAction(getActivity());
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(getActivity());
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(getActivity());

        mPrimaryActionsAdapter.add(mSkipPreviousAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mSkipNextAction);
    }

    private boolean equalsQueue(List<MediaSessionCompat.QueueItem> list1,
                                List<MediaSessionCompat.QueueItem> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i=0; i<list1.size(); i++) {
            if (list1.get(i).getQueueId() != list2.get(i).getQueueId()) {
                return false;
            }
            if (!TextUtils.equals(list1.get(i).getDescription().getMediaId(),
                    list2.get(i).getDescription().getMediaId())) {
                return false;
            }
        }
        return true;
    }

    protected void updatePlayListRow(List<MediaSessionCompat.QueueItem> playlistQueue) {
        if (equalsQueue(mPlaylistQueue, playlistQueue)) {
            // if the playlist queue hasn't changed, we don't need to update it
            return;
        }
        LogHelper.d(TAG, "Updating playlist queue ('now playing')");
        mPlaylistQueue = playlistQueue;
        if (playlistQueue == null || playlistQueue.isEmpty()) {
            // Remove the playlist row if no items are in the playlist
            mRowsAdapter.remove(mListRow);
            mListRow = null;
            return;
        }
        mListRowAdapter.clear();
        for (int i = 0; i < playlistQueue.size(); i++) {
            MediaSessionCompat.QueueItem item = playlistQueue.get(i);
            mListRowAdapter.add(item);
        }

        if (mListRow == null) {
            int queueSize = 0;
            MediaControllerCompat controller = getActivity().getSupportMediaController();
            if (controller != null && controller.getQueue() != null) {
                queueSize = controller.getQueue().size();
            }
            HeaderItem header = new HeaderItem(0, queueSize + " song(s) in this playlist");

            mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

            mListRow = new ListRow(header, mListRowAdapter);
            mRowsAdapter.add(mListRow);
        } else {
            mRowsAdapter.notifyArrayItemRangeChanged(mRowsAdapter.indexOf(mListRow), 1);
        }
    }

    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
        }
    }

    private void resetPlaybackRow() {
        mDuration = 0;
        mPlaybackControlsRow.setTotalTime(0);
        mPlaybackControlsRow.setCurrentTime(0);
        mRowsAdapter.notifyArrayItemRangeChanged(
                mRowsAdapter.indexOf(mPlaybackControlsRow), 1);
    }

    private int getUpdatePeriod() {
        if (getView() == null || mPlaybackControlsRow.getTotalTime() <= 0) {
            return DEFAULT_UPDATE_PERIOD;
        }
        return Math.max(UPDATE_PERIOD, mPlaybackControlsRow.getTotalTime() / getView().getWidth());
    }

    protected void startProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = SystemClock.elapsedRealtime() - mLastPositionUpdateTime;
                int currentPosition = Math.min(mDuration, (int) (mLastPosition + elapsedTime));
                mPlaybackControlsRow.setCurrentTime(currentPosition);
                mHandler.postDelayed(this, getUpdatePeriod());
            }
        };
        mHandler.postDelayed(mRunnable, getUpdatePeriod());
        setFadingEnabled(true);
    }

    protected void stopProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            setFadingEnabled(false);
        }
    }

    private void updateAlbumArt(Uri artUri) {
        AlbumArtCache.getInstance().fetch(artUri.toString(), new AlbumArtCache.FetchListener() {
                    @Override
                    public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                        if (bitmap != null) {
                            Drawable artDrawable = new BitmapDrawable(
                                    TvPlaybackFragment.this.getResources(), bitmap);
                            Drawable bgDrawable = new BitmapDrawable(
                                    TvPlaybackFragment.this.getResources(), bitmap);
                            mPlaybackControlsRow.setImageDrawable(artDrawable);
                            mBackgroundManager.setDrawable(bgDrawable);
                            mRowsAdapter.notifyArrayItemRangeChanged(
                                    mRowsAdapter.indexOf(mPlaybackControlsRow), 1);
                        }
                    }
                }
        );
    }

    protected void updateMetadata(MediaMetadataCompat metadata) {
        if (mPlaybackControlsRow == null) {
            initializePlaybackControls(metadata);
        }
        mDuration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mPlaybackControlsRow.setTotalTime(mDuration);
        ((MutableMediaMetadataHolder) mPlaybackControlsRow.getItem()).metadata = metadata;
        mRowsAdapter.notifyArrayItemRangeChanged(
                mRowsAdapter.indexOf(mPlaybackControlsRow), 1);
        updateAlbumArt(metadata.getDescription().getIconUri());
    }

    protected void updatePlaybackState(PlaybackStateCompat state) {
        if (mPlaybackControlsRow == null) {
            // We only update playback state after we get a valid metadata.
            return;
        }
        mLastPosition = state.getPosition();
        mLastPositionUpdateTime = state.getLastPositionUpdateTime();
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                startProgressAutomation();
                mPlayPauseAction.setIndex(PlayPauseAction.PAUSE);
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                stopProgressAutomation();
                mPlayPauseAction.setIndex(PlayPauseAction.PLAY);
                break;
        }

        MediaControllerCompat controller = getActivity().getSupportMediaController();
        updatePlayListRow(controller.getQueue());
        mRowsAdapter.notifyArrayItemRangeChanged(
                mRowsAdapter.indexOf(mPlaybackControlsRow), 1);
    }

    private static final class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            MutableMediaMetadataHolder data = ((MutableMediaMetadataHolder) item);
            viewHolder.getTitle().setText(data.metadata.getDescription().getTitle());
            viewHolder.getSubtitle().setText(data.metadata.getDescription().getSubtitle());
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof MediaSessionCompat.QueueItem) {
                LogHelper.d(TAG, "item: ", item.toString());
                MediaControllerCompat controller = getActivity().getSupportMediaController();
                controller.getTransportControls().skipToQueueItem(
                        ((MediaSessionCompat.QueueItem) item).getQueueId());
            }
        }
    }

    private static final class MutableMediaMetadataHolder {
        MediaMetadataCompat metadata;
        public MutableMediaMetadataHolder(MediaMetadataCompat metadata) {
            this.metadata = metadata;
        }
    }
}
