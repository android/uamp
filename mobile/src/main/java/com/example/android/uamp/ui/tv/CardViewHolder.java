/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.media.MediaDescriptionCompat;
import android.view.View;

import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.ui.MediaItemViewHolder;

public class CardViewHolder extends Presenter.ViewHolder {

    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 250;

    private final ImageCardView mCardView;
    private int mItemState;

    public CardViewHolder(View view) {
        super(view);
        mCardView = (ImageCardView) view;
        mItemState = MediaItemViewHolder.STATE_NONE;
    }

    public void setState(int state) {
        mItemState = state;
    }

    public int getState() {
        return mItemState;
    }

    public void attachView() {
        if (mItemState == MediaItemViewHolder.STATE_PLAYING) {
            AnimationDrawable badgeDrawable = (AnimationDrawable) mCardView.getBadgeImage();
            if (badgeDrawable != null) {
                badgeDrawable.start();
            }
        }
    }

    public void detachView() {
        if (mItemState == MediaItemViewHolder.STATE_PLAYING) {
            AnimationDrawable badgeDrawable = (AnimationDrawable) mCardView.getBadgeImage();
            if (badgeDrawable != null) {
                badgeDrawable.stop();
            }
        }
    }

    public void setBadgeImage(Drawable drawable) {
        mCardView.setBadgeImage(drawable);
    }

    /**
     * Set the view in this holder to represent the media metadata in {@code description}
     *
     **/
    public void setupCardView(final Context context, MediaDescriptionCompat description) {
        mCardView.setTitleText(description.getTitle());
        mCardView.setContentText(description.getSubtitle());
        mCardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

        // Based on state of item, set or unset badge
        Drawable drawable = MediaItemViewHolder.getDrawableByState(context, mItemState);
        mCardView.setBadgeImage(drawable);

        Uri artUri = description.getIconUri();
        if (artUri == null) {
            setCardImage(context, description.getIconBitmap());
        } else {
            // IconUri potentially has a better resolution than iconBitmap.
            String artUrl = artUri.toString();
            AlbumArtCache cache = AlbumArtCache.getInstance();
            if (cache.getBigImage(artUrl) != null) {
                // So, we use it immediately if it's cached:
                setCardImage(context, cache.getBigImage(artUrl));
            } else {
                // Otherwise, we use iconBitmap if available while we wait for iconURI
                setCardImage(context, description.getIconBitmap());
                cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                    @Override
                    public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                        setCardImage(context, bitmap);
                    }
                });
            }
        }
    }

    private void setCardImage(Context context, Bitmap art) {
        if (mCardView == null) {
            return;
        }
        Drawable artDrawable = null;
        if (art != null) {
            artDrawable = new BitmapDrawable(context.getResources(), art);
        } else {
            CharSequence title = mCardView.getTitleText();
            if (title != null && title.length() > 0) {
                artDrawable = new TextDrawable(String.valueOf(title.charAt(0)));
            }
        }
        mCardView.setMainImage(artDrawable);
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