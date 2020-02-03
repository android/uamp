/*
 * Copyright 2020 Google Inc. All rights reserved.
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

package com.example.android.uamp.automotive.lib

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Fragment that is used to display the multi-selection entries of a
 * {@link MultiSelectListPreference}.
 */
class ListPreferenceFragment : Fragment() {

  private lateinit var preference: ListPreference
  private lateinit var adapter: ListPreferenceAdapter

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.list_preference, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val recyclerView = view.findViewById<RecyclerView>(R.id.list)
    preference = getPreference()
    adapter = ListPreferenceAdapter(preference)
    recyclerView.adapter = adapter
    recyclerView.layoutManager = LinearLayoutManager(context)
  }

  private fun getPreference() : ListPreference {
    val key = arguments?.getCharSequence(LIST_PREFERENCE_ARG_KEY)
      ?: throw IllegalStateException("Preference arguments cannot be null")
    return (targetFragment as PreferenceFragmentCompat).findPreference(key)
      ?: throw IllegalStateException("Unable to find ListPreference with key: $key")
  }

  override fun onPause() {
    super.onPause()
    val selectedIndex = adapter.getSelectedEntry()
    if (selectedIndex < 0) {
      return
    }

    val entryValue = preference.getEntryValues()!![selectedIndex].toString()
    if (preference.callChangeListener(entryValue)) {
      preference.setValue(entryValue)
    }
  }
}