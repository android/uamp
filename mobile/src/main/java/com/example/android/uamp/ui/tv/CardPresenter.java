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
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.ViewGroup;

import com.example.android.uamp.R;
import com.example.android.uamp.ui.MediaItemViewHolder;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.QueueHelper;

public class CardPresenter extends Presenter {
    private static final String TAG = LogHelper.makeLogTag(CardPresenter.class);

    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        LogHelper.d(TAG, "onCreateViewHolder");
        mContext = parent.getContext();

        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(mContext.getResources().getColor(R.color.default_background));
        return new CardViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        MediaDescriptionCompat description;
        final CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;

        // Determine description and playing state of item based on instance type
        cardViewHolder.setState(MediaItemViewHolder.STATE_NONE);
        if (item instanceof  MediaBrowserCompat.MediaItem) {
            MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;
            LogHelper.d(TAG, "onBindViewHolder MediaItem: ", mediaItem.toString());
            description = mediaItem.getDescription();
            cardViewHolder.setState(MediaItemViewHolder.getMediaItemState(mContext, mediaItem));
        } else if (item instanceof MediaSessionCompat.QueueItem) {
            MediaSessionCompat.QueueItem queueItem = (MediaSessionCompat.QueueItem) item;
            LogHelper.d(TAG, "onBindViewHolder QueueItem: ", queueItem.toString());
            description = queueItem.getDescription();
            if (QueueHelper.isQueueItemPlaying(mContext, queueItem)) {
                cardViewHolder.setState(MediaItemViewHolder.getStateFromController(mContext));
            }
        } else {
            throw new IllegalArgumentException("Object must be MediaItem or QueueItem, not "
                    + item.getClass().getSimpleName());
        }

        cardViewHolder.setupCardView(mContext, description);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        LogHelper.d(TAG, "onUnbindViewHolder");
        final CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;
        cardViewHolder.setState(MediaItemViewHolder.STATE_NONE);
        cardViewHolder.setBadgeImage(null);
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        LogHelper.d(TAG, "onViewAttachedToWindow");
        final CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;
        cardViewHolder.attachView();
    }

    @Override
    public void onViewDetachedFromWindow(Presenter.ViewHolder viewHolder) {
        LogHelper.d(TAG, "onViewDetachedFromWindow");
        final CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;
        cardViewHolder.detachView();
    }

}


