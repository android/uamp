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
package com.example.android.uamp.ui;

import android.app.ActivityManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.ResourceHelper;

/**
 * Placeholder activity for features that are not implemented in this sample, but
 * are in the navigation drawer.
 */
public class PlaceholderActivity extends ActionBarCastActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);

        initializeToolbar();
        // Since our app icon has the same color as colorPrimary, our entry in the Recents Apps
        // list gets weird. We need to change either the icon or the color of the TaskDescription.
        ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
            getTitle().toString(),
            BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_white),
            ResourceHelper.getThemeColor(this, R.attr.colorPrimary, android.R.color.darker_gray));
        setTaskDescription(taskDesc);

    }

}
