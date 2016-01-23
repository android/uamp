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

import android.support.v4.media.session.MediaSessionCompat;

class SimplePlayback implements Playback {
    @Override
    public void start() {

    }

    @Override
    public void stop(boolean notifyListeners) {

    }

    @Override
    public void setState(int state) {

    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getCurrentStreamPosition() {
        return 0;
    }

    @Override
    public void setCurrentStreamPosition(int pos) {

    }

    @Override
    public void updateLastKnownStreamPosition() {

    }

    @Override
    public void play(MediaSessionCompat.QueueItem item) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void seekTo(int position) {

    }

    @Override
    public void setCurrentMediaId(String mediaId) {

    }

    @Override
    public String getCurrentMediaId() {
        return null;
    }

    @Override
    public void setCallback(Callback callback) {

    }
}
