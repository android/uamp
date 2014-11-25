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
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;

import com.example.android.uamp.utils.LogHelper;

/**
 * Main activity for the music player.
 */
public class MusicPlayerActivity extends Activity
        implements BrowseFragment.FragmentDataHelper {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);

    private MediaBrowser mMediaBrowser;
    private MediaController mMediaController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, BrowseFragment.newInstance(null))
                    .commit();
        }
    }

    @Override
    public void onMediaItemSelected(MediaBrowser.MediaItem item) {
        if (item.isPlayable()) {
            getMediaController().getTransportControls().playFromMediaId(item.getMediaId(), null);
            QueueFragment queueFragment = QueueFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, queueFragment)
                    .addToBackStack(null)
                    .commit();
        } else if (item.isBrowsable()) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, BrowseFragment.newInstance(item.getMediaId()))
                    .addToBackStack(null)
                    .commit();
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }
}
