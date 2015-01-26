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
package com.example.android.uamp.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;
import com.example.android.uamp.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowser} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowser.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaBrowserFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserFragment.class);

    private static final String ARG_MEDIA_ID = "media_id";

    private BrowseAdapter mBrowserAdapter;
    private String mMediaId;
    private MediaFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;
    private BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
        private boolean oldOnline = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            // We don't care about network changes while this fragment is not associated
            // with a media ID (for example, while it is being initialized)
            if (mMediaId != null) {
                boolean isOnline = NetworkHelper.isOnline(context);
                if (isOnline != oldOnline) {
                    oldOnline = isOnline;
                    checkForUserVisibleErrors(false);
                    if (isOnline) {
                        mBrowserAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };


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
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "Received state change: ", state);
            checkForUserVisibleErrors(false);
            mBrowserAdapter.notifyDataSetChanged();
        }
    };

    private MediaBrowser.SubscriptionCallback mSubscriptionCallback =
        new MediaBrowser.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                try {
                    LogHelper.d(TAG, "fragment onChildrenLoaded, parentId=" + parentId +
                        "  count=" + children.size());
                    checkForUserVisibleErrors(children.isEmpty());
                    mBrowserAdapter.clear();
                    for (MediaBrowser.MediaItem item : children) {
                        mBrowserAdapter.add(item);
                    }
                    mBrowserAdapter.notifyDataSetChanged();
                } catch (Throwable t) {
                    LogHelper.e(TAG, "Error on childrenloaded", t);
                }
            }

            @Override
            public void onError(String id) {
                LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
                Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
                checkForUserVisibleErrors(true);
            }
        };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaFragmentListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.d(TAG, "fragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        mBrowserAdapter = new BrowseAdapter(getActivity());

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                checkForUserVisibleErrors(false);
                MediaBrowser.MediaItem item = mBrowserAdapter.getItem(position);
                mMediaFragmentListener.onMediaItemSelected(item);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // fetch browsing information to fill the listview:
        MediaBrowser mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        LogHelper.d(TAG, "fragment.onStart, mediaId=", mMediaId,
                "  onConnected=" + mediaBrowser.isConnected());

        if (mediaBrowser.isConnected()) {
            onConnected();
        }

        // Registers BroadcastReceiver to track network connection changes.
        this.getActivity().registerReceiver(mConnectivityChangeReceiver,
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowser mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().unregisterCallback(mMediaControllerCallback);
        }
        this.getActivity().unregisterReceiver(mConnectivityChangeReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public String getMediaId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(ARG_MEDIA_ID);
        }
        return null;
    }

    public void setMediaId(String mediaId) {
        Bundle args = new Bundle(1);
        args.putString(MediaBrowserFragment.ARG_MEDIA_ID, mediaId);
        setArguments(args);
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onConnected() {
        if (isDetached()) {
            return;
        }
        mMediaId = getMediaId();
        if (mMediaId == null) {
            mMediaId = mMediaFragmentListener.getMediaBrowser().getRoot();
        }
        updateTitle();

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        // TODO(mangini): before publishing, check if go/ag/609270 has been publicly released.
        // If yes, the unsubscribe below and the comment above can be removed.
        mMediaFragmentListener.getMediaBrowser().unsubscribe(mMediaId);

        mMediaFragmentListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().registerCallback(mMediaControllerCallback);
        }
    }

    private void checkForUserVisibleErrors(boolean forceError) {
        boolean showError = forceError;
        // If offline, message is about the lack of connectivity:
        if (!NetworkHelper.isOnline(getActivity())) {
            mErrorMessage.setText(R.string.error_no_connection);
            showError = true;
        } else {
            // otherwise, if state is ERROR and metadata!=null, use playback state error message:
            MediaController controller = getActivity().getMediaController();
            if (controller != null
                && controller.getMetadata() != null
                && controller.getPlaybackState().getState() == PlaybackState.STATE_ERROR
                && controller.getPlaybackState().getErrorMessage() != null) {
                mErrorMessage.setText(controller.getPlaybackState().getErrorMessage());
                showError = true;
            } else if (forceError) {
                // Finally, if the caller requested to show error, show a generic message:
                mErrorMessage.setText(R.string.error_loading_media);
                showError = true;
            }
        }
        mErrorView.setVisibility(showError ? View.VISIBLE : View.GONE);
        LogHelper.d(TAG, "checkForUserVisibleErrors. forceError=", forceError,
            " showError=", showError,
            " isOnline=", NetworkHelper.isOnline(getActivity()));
    }

    private void updateTitle() {
        if (MediaIDHelper.MEDIA_ID_ROOT.equals(mMediaId)) {
            mMediaFragmentListener.setToolbarTitle(null);
            return;
        }

        final String parentId = MediaIDHelper.getParentMediaID(mMediaId);

        // MediaBrowser doesn't provide metadata for a given mediaID, only for its children. Since
        // the mediaId contains the item's hierarchy, we know the item parent mediaId and we can
        // fetch and iterate over them and find the proper MediaItem, from which we get the title,

        // TODO(mangini): replace the code below by a better solution, based on b/18778785 BEFORE
        // PUBLISHING! The current API doesn't have a get(mediaID) method,
        // as described on the bug. The issue is: even if I save the entire MediaDescription when
        // navigating downwards, navigation upwards will be a problem if the content of that
        // item changes. Example: user navigates to
        //      Root -> item1 -> item1.4 -> item 1.4.2 -> item 1.4.2.3
        // then navigates back:
        //      item 1.4.2.3 -> item 1.4.2 -> item1.4
        // MediaDescription for item1.4 was saved on the navigation downwards and is restored from
        // a Bundle on navigation upwards. However, in the meantime it has changed (for example,
        // the category icon bitmap was still being downloaded async'y on the downwards navigation).
        // The fragment showing item1.4 was detached before MediaBrowser notifying it of the new
        // data. Currently, the only way to request a fresh MediaItem of a given mediaID is by
        // requesting its parent's children, like the code below.
        LogHelper.d(TAG, "on updateTitle: mediaId=", mMediaId, " parentID=", parentId);
        if (parentId != null) {
            MediaBrowser mediaBrowser = mMediaFragmentListener.getMediaBrowser();
            LogHelper.d(TAG, "on updateTitle: mediaBrowser is ",
                    mediaBrowser==null?"null":("not null, connected="+mediaBrowser.isConnected()));
            if (mediaBrowser != null && mediaBrowser.isConnected()) {
                // Unsubscribing is required to guarantee that we will get the initial values.
                // Otherwise, if there is another callback subscribed to this mediaID, mediaBrowser
                // will only call this callback when the media content change.
                mediaBrowser.unsubscribe(parentId);
                mediaBrowser.subscribe(parentId, new MediaBrowser.SubscriptionCallback() {
                    @Override
                    public void onChildrenLoaded(String parentId,
                             List<MediaBrowser.MediaItem> children) {
                        LogHelper.d(TAG, "Got ", children.size(), " children for ", parentId,
                            ". Looking for ", mMediaId);
                        for (MediaBrowser.MediaItem item: children) {
                            LogHelper.d(TAG, "child ", item.getMediaId());
                            if (item.getMediaId().equals(mMediaId)) {
                                mMediaFragmentListener.setToolbarTitle(item.getDescription().getTitle());
                                return;
                            }
                        }
                        mMediaFragmentListener.getMediaBrowser().unsubscribe(parentId);
                    }

                    @Override
                    public void onError(String id) {
                        super.onError(id);
                        LogHelper.d(TAG, "subscribe error: id=", id);
                    }
                });
            }
        }
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class BrowseAdapter extends ArrayAdapter<MediaBrowser.MediaItem> {

        public BrowseAdapter(Activity context) {
            super(context, R.layout.media_list_item, new ArrayList<MediaBrowser.MediaItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MediaBrowser.MediaItem item = getItem(position);
            int state = MediaItemViewHolder.STATE_NONE;
            if (item.isPlayable()) {
                state = MediaItemViewHolder.STATE_PLAYABLE;
                MediaController controller = ((Activity) getContext()).getMediaController();
                if (controller != null && controller.getMetadata() != null) {
                    String currentPlaying = controller.getMetadata().getDescription().getMediaId();
                    String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                            item.getDescription().getMediaId());
                    if (currentPlaying != null && currentPlaying.equals(musicId)) {
                        if (controller.getPlaybackState().getState() ==
                                PlaybackState.STATE_PLAYING) {
                            state = MediaItemViewHolder.STATE_PLAYING;
                        } else if (controller.getPlaybackState().getState() !=
                                PlaybackState.STATE_ERROR) {
                            state = MediaItemViewHolder.STATE_PAUSED;
                        }
                    }
                }
            }
            return MediaItemViewHolder.setupView((Activity) getContext(), convertView, parent,
                item.getDescription(), state);
        }
    }

    public static interface MediaFragmentListener {
        void onMediaItemSelected(MediaBrowser.MediaItem item);
        MediaBrowser getMediaBrowser();
        void setToolbarTitle(CharSequence title);
    }

}
