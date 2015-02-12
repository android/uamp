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
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaDescription;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.uamp.R;

public class MediaItemViewHolder {

    static final int STATE_INVALID = -1;
    static final int STATE_NONE = 0;
    static final int STATE_PLAYABLE = 1;
    static final int STATE_PAUSED = 2;
    static final int STATE_PLAYING = 3;

    ImageView mImageView;
    TextView mTitleView;
    TextView mDescriptionView;

    static View setupView(Activity activity, View convertView, ViewGroup parent,
                                    MediaDescription description, int state) {
        MediaItemViewHolder holder;

        Integer cachedState = STATE_INVALID;

        if (convertView == null) {
            convertView = LayoutInflater.from(activity)
                    .inflate(R.layout.media_list_item, parent, false);
            holder = new MediaItemViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(holder);
        } else {
            holder = (MediaItemViewHolder) convertView.getTag();
            cachedState = (Integer) convertView.getTag(R.id.tag_mediaitem_state_cache);
        }

        holder.mTitleView.setText(description.getTitle());
        holder.mDescriptionView.setText(description.getSubtitle());

        // If the state of convertView is different, we need to adapt the view to the
        // new state.
        if (cachedState == null || cachedState != state) {
            switch (state) {
                case STATE_PLAYABLE:
                    holder.mImageView.setImageDrawable(
                            activity.getDrawable(R.drawable.ic_play_arrow_white_24dp));
                    holder.mImageView.setVisibility(View.VISIBLE);
                    break;
                case STATE_PLAYING:
                    AnimationDrawable animation = (AnimationDrawable)
                            activity.getDrawable(R.drawable.ic_equalizer_white_24dp);
                    holder.mImageView.setImageDrawable(animation);
                    holder.mImageView.setVisibility(View.VISIBLE);
                    animation.start();
                    break;
                case STATE_PAUSED:
                    holder.mImageView.setImageDrawable(
                            activity.getDrawable(R.drawable.ic_equalizer1_white_24dp));
                    holder.mImageView.setVisibility(View.VISIBLE);
                    break;
                default:
                    holder.mImageView.setVisibility(View.GONE);
            }
            convertView.setTag(R.id.tag_mediaitem_state_cache, state);
        }

        return convertView;
    }

}
