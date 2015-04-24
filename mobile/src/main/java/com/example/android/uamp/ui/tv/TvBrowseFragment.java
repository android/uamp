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
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.browse.MediaBrowser.MediaItem;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.View;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

import java.util.HashSet;
import java.util.List;

/**
 * Browse media categories and current playing queue.
 *
 * WARNING: This sample's UI is implemented for a specific MediaBrowser tree structure.
 * It expects a tree that is three levels deep under root:
 *  - level 0: root
 *  - level 1: categories of categories (like "by genre", "by artist", "playlists")
 *  - level 2: song categories (like "by genre -> Rock", "by  artist -> artistname" or
 *             "playlists -> my favorite music")
 *  - level 3: the actual music
 *
 * If you are reusing this TV code, make sure you adapt it to your MediaBrowser structure, in case
 * it is not the same.
 *
 *
 * It uses a {@link MediaBrowser} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get the children of level 1 and then, for each
 * children, it adds a ListRow and subscribes for its children, which, when received, are
 * added to the ListRow. These items (like "Rock"), when clicked, will open a
 * TvVerticalGridActivity that lists all songs of the specified category on a grid-like UI.
 *
 * This fragment also shows the MediaSession queue ("now playing" list), in case there is
 * something playing.
 *
 */
public class TvBrowseFragment extends BrowseFragment {

    private static final String TAG = LogHelper.makeLogTag(TvBrowseFragment.class);

    private ArrayObjectAdapter mRowsAdapter;
    private String mMediaId;
    private MediaFragmentListener mMediaFragmentListener;

    private MediaBrowser mMediaBrowser;
    private HashSet<String> mSubscribedMediaIds;

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (metadata != null) {
                mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
            }
        }
    };

    private MediaBrowser.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowser.SubscriptionCallback() {

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowser.MediaItem> children) {

            mRowsAdapter.clear();
            CardPresenter cardPresenter = new CardPresenter();

            for (int i = 0; i < children.size(); i++) {
                MediaItem item = children.get(i);
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

            MediaController mediaController = new MediaController(
                    getActivity().getApplicationContext(), mMediaBrowser.getSessionToken());
            if (mediaController.getQueue() != null && !mediaController.getQueue().isEmpty()) {
                // add Now Playing queue to Browse Home
                HeaderItem header = new HeaderItem(
                        children.size(), getString(R.string.now_playing));
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                mRowsAdapter.add(new ListRow(header, listRowAdapter));
                listRowAdapter.addAll(0, mediaController.getQueue());
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
    private class RowSubscriptionCallback extends MediaBrowser.SubscriptionCallback {

        private final ArrayObjectAdapter mListRowAdapter;

        public RowSubscriptionCallback(ArrayObjectAdapter listRowAdapter) {
            mListRowAdapter = listRowAdapter;
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId,
                                     @NonNull List<MediaBrowser.MediaItem> children) {
            mListRowAdapter.clear();
            for (MediaBrowser.MediaItem item : children) {
                mListRowAdapter.add(item);
            }
            mListRowAdapter.notifyArrayItemRangeChanged(0, children.size());
        }

        @Override
        public void onError(@NonNull String id) {
            LogHelper.e(TAG, "RowSubscriptionCallback subscription onError, id=",  id);
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
                if (o instanceof MediaItem) {
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

                } else if (o instanceof MediaSession.QueueItem) {
                    MediaSession.QueueItem item = (MediaSession.QueueItem) o;
                    getActivity().getMediaController().getTransportControls()
                            .skipToQueueItem(item.getQueueId());
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
            for (String mediaId: mSubscribedMediaIds) {
                mMediaBrowser.unsubscribe(mediaId);
            }
            mSubscribedMediaIds.clear();
        }
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().unregisterCallback(mMediaControllerCallback);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public String getMediaId() {
        return mMediaId;
    }

    public void initializeWithMediaId(String mediaId) {
        LogHelper.d(TAG, "subscribeToData");
        mMediaId = mediaId;
        // fetch browsing information to fill the listview:
        mMediaBrowser = mMediaFragmentListener.getMediaBrowser();

        if (mMediaId == null) {
            mMediaId = mMediaBrowser.getRoot();
        }

        subscribeToMediaId(mMediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().registerCallback(mMediaControllerCallback);
        }
    }

    private void subscribeToMediaId(String mediaId, MediaBrowser.SubscriptionCallback callback) {
        if (mSubscribedMediaIds.contains(mediaId)) {
            mMediaBrowser.unsubscribe(mediaId);
        } else {
            mSubscribedMediaIds.add(mediaId);
        }
        mMediaBrowser.subscribe(mediaId, callback);
    }

    public interface MediaFragmentListener {
        MediaBrowser getMediaBrowser();
    }

}

