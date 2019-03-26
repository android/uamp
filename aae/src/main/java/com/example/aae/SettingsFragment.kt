/*
 * Copyright 2019 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.aae

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/**
 * Preference fragment hosted by [SettingsActivity]. Handles events to various preference changes.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            "logout" -> {
                //TODO: Update the PlaybackState in the service to PlaybackStateCompat.STATE_ERROR
                requireActivity().finish()
                true
            } else -> {
                super.onPreferenceTreeClick(preference)
            }
        }
    }
}