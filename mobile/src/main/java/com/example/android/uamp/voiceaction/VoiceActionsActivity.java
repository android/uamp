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
package com.example.android.uamp.voiceaction;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;

import com.example.android.uamp.ui.MusicPlayerActivity;
import com.example.android.uamp.utils.LogHelper;

/**
 * Activity that handles the voice action searches and delegates them to the active
 * {@link android.media.session.MediaSession}, if any, or starts a new
 * {@link com.example.android.uamp.ui.MusicPlayerActivity} if necessary.
 */
public class VoiceActionsActivity extends Activity {

    private static final String TAG = LogHelper.makeLogTag(VoiceActionsActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "VoiceActionsActivity created");
        processVoiceQuery();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processVoiceQuery();
    }

    private void processVoiceQuery() {
        Intent intent = this.getIntent();
        String action = intent.getAction();
        if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            LogHelper.d(TAG, "Play from search, query=", query);
            Intent uiIntent = new Intent(this, MusicPlayerActivity.class);
            uiIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            uiIntent.putExtra(MusicPlayerActivity.EXTRA_PLAY_QUERY, query);
            startActivity(uiIntent);
        } else {
            LogHelper.w(TAG, "Unsupported action: " + action);
        }
        finish();
    }
}
