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
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.widget.Toast;

import com.example.android.uamp.R;

public class MediaDetailsFragment extends DetailsFragment {

    public static final String MEDIA_ITEM_EXTRA = "MEDIA_ITEM_EXTRA";
    private static final int ACTION_LISTEN = 1;

    private Activity mActivity;
    private MediaBrowser.MediaItem mMediaItem;
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mRelatedAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaItem = mActivity.getIntent().getParcelableExtra(MediaDetailsFragment.MEDIA_ITEM_EXTRA);

        buildDetails();
        updateBackground();
    }

    private void buildDetails() {
        ClassPresenterSelector ps = new ClassPresenterSelector();
        mRowsAdapter = new ArrayObjectAdapter(ps);
        setAdapter(mRowsAdapter);

        // Tell presenter selector how to present a DetailsOverviewRow.
        DetailsOverviewRowPresenter dorPresenter = createDorPresenter();
        ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);

        // Tell presenter selector how to present a ListRow.
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());

        // Add DetailsOverviewRow.
        DetailsOverviewRow row = new DetailsOverviewRow(mMediaItem);
        Bitmap bitmap = mMediaItem.getDescription().getIconBitmap();
        row.setImageBitmap(mActivity, bitmap);
        row.addAction(new Action(ACTION_LISTEN, getResources().getString(R.string.action_listen)));
        mRowsAdapter.add(row);

        addRelatedContentRow();
    }

    private void addRelatedContentRow() {
        mRelatedAdapter = new ArrayObjectAdapter(new CardPresenter());
        HeaderItem headerItem = new HeaderItem(0, getString(R.string.related_content_header), null);
        ListRow relatedContentRow = new ListRow(headerItem, mRelatedAdapter);
        mRowsAdapter.add(relatedContentRow);
    }

    private void updateBackground() {
        Bitmap bitmap = mMediaItem.getDescription().getIconBitmap();
        BackgroundManager backgroundManager = BackgroundManager.getInstance(mActivity);
        backgroundManager.setBitmap(bitmap);
    }

    private DetailsOverviewRowPresenter createDorPresenter() {
        DetailsDescriptionPresenter descPresenter = new DetailsDescriptionPresenter();
        DetailsOverviewRowPresenter dorPresenter = new DetailsOverviewRowPresenter(descPresenter);
        // set detail background and style
        dorPresenter.setStyleLarge(true);
        dorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_LISTEN) {
                    Intent intent = new Intent(mActivity, TvNowPlayingActivity.class);
                    intent.putExtra(MediaDetailsFragment.MEDIA_ITEM_EXTRA, mMediaItem);
                    startActivity(intent);
                } else {
                    Toast.makeText(mActivity, action.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        return dorPresenter;
    }

    private static class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

        private DetailsDescriptionPresenter() {
            super();
        }

        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            MediaBrowser.MediaItem mediaItem = (MediaBrowser.MediaItem) item;

            if (mediaItem != null) {
                MediaDescription description = mediaItem.getDescription();
                viewHolder.getTitle().setText(description.getTitle());
                viewHolder.getSubtitle().setText(description.getSubtitle());
                viewHolder.getBody().setText(description.getDescription());
            }
        }
    }
}
