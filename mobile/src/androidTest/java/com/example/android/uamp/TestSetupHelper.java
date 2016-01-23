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

import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.model.MusicProviderSource;
import com.example.android.uamp.utils.SimpleMusicProviderSource;

import java.util.concurrent.CountDownLatch;

public class TestSetupHelper {

    public static MusicProvider setupMusicProvider(MusicProviderSource source)
            throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        MusicProvider provider = new MusicProvider(source);
        provider.retrieveMediaAsync(new MusicProvider.Callback() {
            @Override
            public void onMusicCatalogReady(boolean success) {
                signal.countDown();
            }
        });
        signal.await();
        return provider;
    }

}