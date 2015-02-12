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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.uamp.R;

public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";
    private static final int CARD_WIDTH = 313;
    private static final int CARD_HEIGHT = 176;

    private static Context mContext;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        mContext = parent.getContext();

        ImageCardView cardView = new ImageCardView(mContext);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new CardViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        MediaDescription description;
        if (item instanceof  MediaBrowser.MediaItem) {
            MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) item;
            Log.d(TAG, "onBindViewHolder");
            description = mediaItem.getDescription();
        } else if (item instanceof MediaSession.QueueItem) {
            MediaSession.QueueItem queueItem = (MediaSession.QueueItem) item;
            description = queueItem.getDescription();
        } else {
            throw new IllegalArgumentException("Object must be MediaItem or QueueItem, not "
                    + item.getClass().getSimpleName());
        }

        CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;
        cardViewHolder.mCardView.setTitleText(description.getTitle());
        cardViewHolder.mCardView.setContentText(description.getSubtitle());
        cardViewHolder.mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        if (description.getIconUri() != null) {
            Bitmap bitmap = description.getIconBitmap();
            BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
            cardViewHolder.mCardView.setMainImage(drawable);
        } else {
            cardViewHolder.mCardView.setMainImage(mContext.getDrawable(R.drawable.ic_by_genre));
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        Log.d(TAG, "onViewAttachedToWindow");
    }

    private static class CardViewHolder extends Presenter.ViewHolder {
        private ImageCardView mCardView;

        public CardViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
        }
    }
}
