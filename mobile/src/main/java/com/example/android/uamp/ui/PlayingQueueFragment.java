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
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that lists the QueueItems in the queue of the current Activity's MediaController.
 * <p/>
 */
public class PlayingQueueFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(PlayingQueueFragment.class);

    private QueueAdapter mQueueAdapter;

    private MediaController.Callback mCallback = new MediaController.Callback() {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            mQueueAdapter.notifyDataSetChanged();
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            updateQueue(queue);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.d(TAG, "fragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mQueueAdapter = new QueueAdapter(getActivity());

        View controls = rootView.findViewById(R.id.controls);
        controls.setVisibility(View.GONE);

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(mQueueAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaSession.QueueItem item = mQueueAdapter.getItem(position);
                LogHelper.d(TAG, "Skipping to queue item "+item.getQueueId());
                getActivity().getMediaController().getTransportControls()
                        .skipToQueueItem(item.getQueueId());
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        LogHelper.d(TAG, "fragment.onStart");
        getActivity().getMediaController().registerCallback(mCallback);
        updateQueue(getActivity().getMediaController().getQueue());
    }

    @Override
    public void onStop() {
        super.onStop();
        LogHelper.d(TAG, "fragment.onStop");
        if (getActivity().getMediaController() != null) {
            getActivity().getMediaController().unregisterCallback(mCallback);
        }
    }

    protected void updateQueue(List<MediaSession.QueueItem> queue) {
        LogHelper.d(TAG, "fragment onQueueChanged, queue.size=" + queue.size());
        mQueueAdapter.clear();
        for (MediaSession.QueueItem item : queue) {
            mQueueAdapter.add(item);
        }
        mQueueAdapter.notifyDataSetChanged();
    }

    // An adapter for showing the queue of musics playing
    static private class QueueAdapter extends ArrayAdapter<MediaSession.QueueItem> {

        public QueueAdapter(Activity context) {
            super(context, R.layout.media_list_item, new ArrayList<MediaSession.QueueItem>());

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MediaSession.QueueItem item = getItem(position);
            View view = MediaItemViewHolder.setupView(
                    (Activity) getContext(), convertView, parent,
                    item.getDescription());
            MediaItemViewHolder holder = (MediaItemViewHolder) view.getTag();

            MediaController controller = ((Activity) getContext()).getMediaController();
            if (controller == null || controller.getMetadata() == null) {
                return view;
            }

            String currentPlaying = controller.getMetadata().getDescription().getMediaId();

            holder.mImageView.setVisibility(View.VISIBLE);

            if (currentPlaying != null && currentPlaying.equals(
                    item.getDescription().getMediaId())) {
                holder.mImageView.setImageDrawable(
                        getContext().getDrawable(R.drawable.ic_equalizer_white_24dp));
            } else {
                holder.mImageView.setImageDrawable(
                        getContext().getDrawable(R.drawable.ic_play_arrow_white_24dp));
            }
            return view;
        }
    }

}
