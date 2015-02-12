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
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.widget.Toast;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

import java.util.List;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowser} to connect to the {@link com.example.android.uamp.MusicService}. Once connected,
 * the fragment subscribes to get all the children. All {@link MediaBrowser.MediaItem}'s
 * that can be browsed are shown in a ListView.
 */
public class TvBrowseFragment extends BrowseFragment {

    private static final String TAG = LogHelper.makeLogTag(TvBrowseFragment.class);

    private ArrayObjectAdapter mRowsAdapter;
    private String mMediaId;
    private MediaFragmentListener mSupportActivity;

    private MediaBrowser mMediaBrowser;
    private CardPresenter mCardPresenter = new CardPresenter();

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private MediaController.Callback mMediaControllerCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                return;
            }
            LogHelper.d(TAG, "Received metadata change to media ",
                    metadata.getDescription().getMediaId());
            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
        }
    };

    private MediaBrowser.SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback();

    private class SubscriptionCallback extends MediaBrowser.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
            try {
                LogHelper.d(TAG, "fragment onChildrenLoaded, parentId=" + parentId
                        + "  count=" + children.size());
                mRowsAdapter.clear();
                for (int i = 0; i < children.size(); i++) {
                    MediaBrowser.MediaItem item = children.get(i);
                    String title = item.getDescription().getTitle().toString();
                    HeaderItem header = new HeaderItem(i, title, null);
                    ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(mCardPresenter);
                    mRowsAdapter.add(new ListRow(header, listRowAdapter));

                    if (item.isPlayable()) {
                        listRowAdapter.add(item);
                    } else if (item.isBrowsable()) {
                        String itemMediaId = item.getMediaId();
                        // Unsubscribing before subscribing is required if this mediaId already has
                        // a subscriber on this MediaBrowser instance. Subscribing to an already
                        // subscribed mediaId will replace the callback, but won't trigger the
                        // initial callback.onChildrenLoaded.
                        mMediaBrowser.unsubscribe(itemMediaId);

                        mMediaBrowser.subscribe(itemMediaId,
                                new RowSubscriptionCallback(listRowAdapter));
                    } else {
                        Log.wtf(TAG, "Item should be playable or browsable.");
                    }
                }
                mRowsAdapter.notifyArrayItemRangeChanged(0, children.size());
            } catch (Throwable t) {
                LogHelper.e(TAG, "Error on childrenloaded", t);
            }
        }

        @Override
        public void onError(String id) {
            LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
            Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This callback fills content for a single Row in the BrowseFragment.
     */
    private class RowSubscriptionCallback extends MediaBrowser.SubscriptionCallback {

        private final ArrayObjectAdapter mListRowAdapter;

        public RowSubscriptionCallback(ArrayObjectAdapter listRowAdapter) {
            mListRowAdapter = listRowAdapter;
        }

        @Override
        public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
            try {
                LogHelper.d(TAG, "fragment onChildrenLoaded, parentId=" + parentId
                        + "  count=" + children.size());
                mListRowAdapter.clear();
                for (MediaBrowser.MediaItem item : children) {
                    mListRowAdapter.add(item);
                }
                mListRowAdapter.notifyArrayItemRangeChanged(0, children.size());
            } catch (Throwable t) {
                LogHelper.e(TAG, "Error on childrenloaded", t);
            }
        }

        @Override
        public void onError(String id) {
            LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
            Toast.makeText(TvBrowseFragment.this.getActivity(),
                    R.string.error_loading_media, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

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
                MediaBrowser.MediaItem item = (MediaBrowser.MediaItem) o;
                mSupportActivity.onMediaItemClicked(item);
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mSupportActivity = (MediaFragmentListener) activity;
        } catch (ClassCastException ex) {
            LogHelper.e(TAG, "Exception trying to cast to MediaFragmentSupportActivity", ex);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");

        subscribeToData();
    }

    private void subscribeToData() {
        Log.d(TAG, "subscribeToData");
        // fetch browsing information to fill the listview:
        if (mSupportActivity == null) {
            Log.e(TAG, "mSupportActivity == null");
        }
        mMediaBrowser = mSupportActivity.getMediaBrowser();

        if (mMediaBrowser == null || !mMediaBrowser.isConnected()) {
            Log.d(TAG, "mMediaBrowser: " + mMediaBrowser);
            if (mMediaBrowser != null) {
                Log.d(TAG, "MediaBrowser not connected");
            }
            return;
        }

        if (mMediaId == null) {
            mMediaId = mMediaBrowser.getRoot();
        }
        LogHelper.d(TAG, "fragment.onStart, mediaId=" + mMediaId +
                "  onConnected=" + mMediaBrowser.isConnected());

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        mMediaBrowser.unsubscribe(mMediaId);

        mMediaBrowser.subscribe(mMediaId, mSubscriptionCallback);

        Log.d(TAG, "Subscribed to mediaId: " + mMediaId);
        // Add MediaController callback so we can redraw the list when metadata changes:
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().registerCallback(mMediaControllerCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowser mediaBrowser = mSupportActivity.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().unregisterCallback(mMediaControllerCallback);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSupportActivity = null;
    }

    public String getMediaId() {
        return mMediaId;
    }

    public void setMediaId(String mediaId) {
        mMediaId = mediaId;
        subscribeToData();
    }

    // An adapter for showing the list of browsed MediaItem's

    public static interface MediaFragmentListener {
        void onMediaItemClicked(MediaBrowser.MediaItem item);

        MediaBrowser getMediaBrowser();
    }


}
