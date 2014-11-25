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

import android.app.Activity;
import android.media.session.MediaSession;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;

/**
 * A list adapter for items in a queue
 */
public class QueueAdapter extends ArrayAdapter<MediaSession.QueueItem> {

    private static final String TAG = LogHelper.makeLogTag(QueueAdapter.class);

    // The currently selected/active queue item Id.
    private long mActiveQueueItemId = MediaSession.QueueItem.UNKNOWN_ID;

    public QueueAdapter(Activity context) {
        super(context, R.layout.media_list_item, new ArrayList<MediaSession.QueueItem>());
    }

    public void setActiveQueueItemId(long id) {
        this.mActiveQueueItemId = id;
    }

    private static class ViewHolder {
        ImageView mImageView;
        TextView mTitleView;
        TextView mDescriptionView;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.media_list_item, parent, false);
            holder = new ViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MediaSession.QueueItem item = getItem(position);
        holder.mTitleView.setText(item.getDescription().getTitle());
        if (item.getDescription().getDescription() != null) {
            holder.mDescriptionView.setText(item.getDescription().getDescription());
        }

        // If the itemId matches the active Id then use a different icon
        if (mActiveQueueItemId == item.getQueueId()) {
            holder.mImageView.setImageDrawable(
                    getContext().getDrawable(R.drawable.ic_equalizer_white_24dp));
        } else {
            holder.mImageView.setImageDrawable(
                    getContext().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        }
        return convertView;
    }
}
