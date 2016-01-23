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
package com.example.android.uamp.playback;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

public class SimpleMetadataUpdateListener implements QueueManager.MetadataUpdateListener{
    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
    }

    @Override
    public void onMetadataRetrieveError() {
    }

    @Override
    public void onCurrentQueueIndexUpdated(int queueIndex) {
    }

    @Override
    public void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue) {
    }
}
