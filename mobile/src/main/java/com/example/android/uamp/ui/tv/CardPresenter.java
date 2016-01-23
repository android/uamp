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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

public class CardPresenter extends Presenter {
    private static final String TAG = LogHelper.makeLogTag(CardPresenter.class);
    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 250;

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
        if (item instanceof  MediaBrowserCompat.MediaItem) {
            MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;
            LogHelper.d(TAG, "onBindViewHolder MediaItem: ", mediaItem.toString());
            description = mediaItem.getDescription();
        } else if (item instanceof MediaSessionCompat.QueueItem) {
            MediaSessionCompat.QueueItem queueItem = (MediaSessionCompat.QueueItem) item;
            description = queueItem.getDescription();
        } else {
            throw new IllegalArgumentException("Object must be MediaItem or QueueItem, not "
                    + item.getClass().getSimpleName());
        }

        final CardViewHolder cardViewHolder = (CardViewHolder) viewHolder;
        cardViewHolder.mCardView.setTitleText(description.getTitle());
        cardViewHolder.mCardView.setContentText(description.getSubtitle());
        cardViewHolder.mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

        Uri artUri = description.getIconUri();
        if (artUri == null) {
            setCardImage(cardViewHolder, description.getIconBitmap());
        } else {
            // IconUri potentially has a better resolution than iconBitmap.
            String artUrl = artUri.toString();
            AlbumArtCache cache = AlbumArtCache.getInstance();
            if (cache.getBigImage(artUrl) != null) {
                // So, we use it immediately if it's cached:
                setCardImage(cardViewHolder, cache.getBigImage(artUrl));
            } else {
                // Otherwise, we use iconBitmap if available while we wait for iconURI
                setCardImage(cardViewHolder, description.getIconBitmap());
                cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                    @Override
                    public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                        setCardImage(cardViewHolder, bitmap);
                    }
                });
            }
        }
    }

    private void setCardImage(CardViewHolder cardViewHolder, Bitmap art) {
        if (cardViewHolder.mCardView == null) {
            return;
        }
        Drawable artDrawable = null;
        if (art != null) {
            artDrawable = new BitmapDrawable(mContext.getResources(), art);
        } else {
            CharSequence title = cardViewHolder.mCardView.getTitleText();
            if (title != null && title.length() > 0) {
                artDrawable = new TextDrawable(String.valueOf(title.charAt(0)));
            }
        }
        cardViewHolder.mCardView.setMainImage(artDrawable);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        LogHelper.d(TAG, "onUnbindViewHolder");
    }

    @Override
    public void onViewAttachedToWindow(Presenter.ViewHolder viewHolder) {
        LogHelper.d(TAG, "onViewAttachedToWindow");
    }

    private static class CardViewHolder extends Presenter.ViewHolder {
        private final ImageCardView mCardView;

        public CardViewHolder(View view) {
            super(view);
            mCardView = (ImageCardView) view;
        }
    }

    /**
     * Simple drawable that draws a text (letter, in this case). Used with the media title when
     * the MediaDescription has no corresponding album art.
     */
    private static class TextDrawable extends Drawable {

        private final String text;
        private final Paint paint;

        public TextDrawable(String text) {
            this.text = text;
            this.paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(280f);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect r = getBounds();
            int count = canvas.save();
            canvas.translate(r.left, r.top);
            float midW = r.width() / 2;
            float midH = r.height() / 2 - ((paint.descent() + paint.ascent()) / 2);
            canvas.drawText(text, midW, midH, paint);
            canvas.restoreToCount(count);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

}


