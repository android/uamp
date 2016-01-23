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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BrowseSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static android.support.v4.media.MediaBrowserCompat.MediaItem;

/**
 * Browse media categories and current playing queue.
 * <p/>
 * WARNING: This sample's UI is implemented for a specific MediaBrowser tree structure.
 * It expects a tree that is three levels deep under root:
 * - level 0: root
 * - level 1: categories of categories (like "by genre", "by artist", "playlists")
 * - level 2: song categories (like "by genre -> Rock", "by  artist -> artistname" or
 * "playlists -> my favorite music")
 * - level 3: the actual music
 * <p/>
 * If you are reusing this TV code, make sure you adapt it to your MediaBrowser structure, in case
 * it is not the same.
 * <p/>
 * <p/>
 * It uses a {@link android.support.v4.media.MediaBrowserCompat} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get the children of level 1 and then, for each
 * children, it adds a ListRow and subscribes for its children, which, when received, are
 * added to the ListRow. These items (like "Rock"), when clicked, will open a
 * TvVerticalGridActivity that lists all songs of the specified category on a grid-like UI.
 * <p/>
 * This fragment also shows the MediaSession queue ("now playing" list), in case there is
 * something playing.
 */
public class TvBrowseFragment extends BrowseSupportFragment {

    private static final String TAG = LogHelper.makeLogTag(TvBrowseFragment.class);

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mListRowAdapter;
    private MediaFragmentListener mMediaFragmentListener;

    private MediaBrowserCompat mMediaBrowser;
    private HashSet<String> mSubscribedMediaIds;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                MediaControllerCompat mediaController = getActivity().getSupportMediaController();
                long activeQueueId;
                if (mediaController.getPlaybackState() == null) {
                    activeQueueId = MediaSessionCompat.QueueItem.UNKNOWN_ID;
                } else {
                    activeQueueId = mediaController.getPlaybackState().getActiveQueueItemId();
                }
                updateNowPlayingList(mediaController.getQueue(), activeQueueId);
                mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
            }
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            // queue has changed somehow
            MediaControllerCompat mediaController = getActivity().getSupportMediaController();

            long activeQueueId;
            if (mediaController.getPlaybackState() == null) {
                activeQueueId = MediaSessionCompat.QueueItem.UNKNOWN_ID;
            } else {
                activeQueueId = mediaController.getPlaybackState().getActiveQueueItemId();
            }
            updateNowPlayingList(queue, activeQueueId);
            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
        }
    };

    private void updateNowPlayingList(List<MediaSessionCompat.QueueItem> queue, long activeQueueId) {
        mListRowAdapter.clear();
        if (activeQueueId != MediaSessionCompat.QueueItem.UNKNOWN_ID) {
            Iterator<MediaSessionCompat.QueueItem> iterator = queue.iterator();
            while (iterator.hasNext()) {
                MediaSessionCompat.QueueItem queueItem = iterator.next();
                if (activeQueueId != queueItem.getQueueId()) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
        mListRowAdapter.addAll(0, queue);
    }

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {

                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {

                    mRowsAdapter.clear();
                    CardPresenter cardPresenter = new CardPresenter();

                    for (int i = 0; i < children.size(); i++) {
                        MediaBrowserCompat.MediaItem item = children.get(i);
                        String title = (String) item.getDescription().getTitle();
                        HeaderItem header = new HeaderItem(i, title);
                        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                        mRowsAdapter.add(new ListRow(header, listRowAdapter));

                        if (item.isPlayable()) {
                            listRowAdapter.add(item);
                        } else if (item.isBrowsable()) {
                            subscribeToMediaId(item.getMediaId(),
                                    new RowSubscriptionCallback(listRowAdapter));
                        } else {
                            LogHelper.e(TAG, "Item should be playable or browsable.");
                        }
                    }

                    MediaControllerCompat mediaController = getActivity().getSupportMediaController();

                    if (mediaController.getQueue() != null
                            && !mediaController.getQueue().isEmpty()) {
                        // add Now Playing queue to Browse Home
                        HeaderItem header = new HeaderItem(
                                children.size(), getString(R.string.now_playing));
                        mListRowAdapter = new ArrayObjectAdapter(cardPresenter);
                        mRowsAdapter.add(new ListRow(header, mListRowAdapter));
                        long activeQueueId;
                        if (mediaController.getPlaybackState() == null) {
                            activeQueueId = MediaSessionCompat.QueueItem.UNKNOWN_ID;
                        } else {
                            activeQueueId = mediaController.getPlaybackState()
                                    .getActiveQueueItemId();
                        }
                        updateNowPlayingList(mediaController.getQueue(), activeQueueId);
                    }

                    mRowsAdapter.notifyArrayItemRangeChanged(0, children.size());
                }

                @Override
                public void onError(@NonNull String id) {
                    LogHelper.e(TAG, "SubscriptionCallback subscription onError, id=" + id);
                }
            };

    /**
     * This callback fills content for a single Row in the BrowseFragment.
     */
    private class RowSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        private final ArrayObjectAdapter mListRowAdapter;

        public RowSubscriptionCallback(ArrayObjectAdapter listRowAdapter) {
            mListRowAdapter = listRowAdapter;
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowserCompat.MediaItem> children) {
            mListRowAdapter.clear();
            for (MediaBrowserCompat.MediaItem item : children) {
                mListRowAdapter.add(item);
            }
            mListRowAdapter.notifyArrayItemRangeChanged(0, children.size());
        }

        @Override
        public void onError(@NonNull String id) {
            LogHelper.e(TAG, "RowSubscriptionCallback subscription onError, id=", id);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LogHelper.d(TAG, "onActivityCreated");

        mSubscribedMediaIds = new HashSet<>();

        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.tv_search_button));

        loadRows();
        setupEventListeners();
    }

    private void loadRows() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object o,
                                      RowPresenter.ViewHolder viewHolder2, Row row) {
                if (o instanceof MediaBrowserCompat.MediaItem) {
                    MediaItem item = (MediaItem) o;
                    if (item.isPlayable()) {
                        LogHelper.w(TAG, "Ignoring click on PLAYABLE MediaItem in" +
                                "TvBrowseFragment. mediaId=", item.getMediaId());
                        return;
                    }
                    Intent intent = new Intent(getActivity(), TvVerticalGridActivity.class);
                    intent.putExtra(TvBrowseActivity.SAVED_MEDIA_ID, item.getMediaId());
                    intent.putExtra(TvBrowseActivity.BROWSE_TITLE,
                            item.getDescription().getTitle());
                    startActivity(intent);

                } else if (o instanceof MediaSessionCompat.QueueItem) {
                    MediaSessionCompat.QueueItem item = (MediaSessionCompat.QueueItem) o;
                    MediaControllerCompat mediaController = getActivity()
                            .getSupportMediaController();
                    mediaController.getTransportControls().skipToQueueItem(item.getQueueId());
                    Intent intent = new Intent(getActivity(), TvPlaybackActivity.class);
                    startActivity(intent);
                }
            }
        });

        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogHelper.d(TAG, "In-app search");
                // TODO: implement in-app search
                Intent intent = new Intent(getActivity(), TvBrowseActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mMediaFragmentListener = (MediaFragmentListener) activity;
        } catch (ClassCastException ex) {
            LogHelper.e(TAG, "TVBrowseFragment can only be attached to an activity that " +
                    "implements MediaFragmentListener", ex);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            for (String mediaId : mSubscribedMediaIds) {
                mMediaBrowser.unsubscribe(mediaId);
            }
            mSubscribedMediaIds.clear();
        }
        MediaControllerCompat mediaController = getActivity().getSupportMediaController();
        if (mediaController != null) {
            mediaController.unregisterCallback(mMediaControllerCallback);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public void initializeWithMediaId(String mediaId) {
        LogHelper.d(TAG, "subscribeToData");
        // fetch browsing information to fill the listview:
        mMediaBrowser = mMediaFragmentListener.getMediaBrowser();

        if (mediaId == null) {
            mediaId = mMediaBrowser.getRoot();
        }

        subscribeToMediaId(mediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        MediaControllerCompat mediaController = getActivity().getSupportMediaController();
        if (mediaController != null) {
            mediaController.registerCallback(mMediaControllerCallback);
        }
    }

    private void subscribeToMediaId(String mediaId, MediaBrowserCompat.SubscriptionCallback callback) {
        if (mSubscribedMediaIds.contains(mediaId)) {
            mMediaBrowser.unsubscribe(mediaId);
        } else {
            mSubscribedMediaIds.add(mediaId);
        }
        mMediaBrowser.subscribe(mediaId, callback);
    }

    public interface MediaFragmentListener {
        MediaBrowserCompat getMediaBrowser();
    }

}

