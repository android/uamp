/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.android.uamp.automotive.lib

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter that provides bindings between a {@link ListPreference} and set of views to display and
 * allow for selection of the entries of the preference.
 */
internal class ListPreferenceAdapter(preference: ListPreference) :
  RecyclerView.Adapter<ListPreferenceAdapter.ViewHolder>() {

  private var mEntries: Array<CharSequence>? = preference.getEntries()
  private var mSelectedEntry: Int = preference.findIndexOfValue(preference.getValue())

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(LayoutInflater.from(parent.context)
      .inflate(R.layout.radio_button_list_item, parent, false))
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.title.text = mEntries?.get(position)
    holder.radioButton.isChecked = position == mSelectedEntry
    holder.holderView.setOnClickListener {
      val previousIndex = mSelectedEntry
      mSelectedEntry = position
      notifyItemChanged(previousIndex)
      notifyItemChanged(position)
    }
  }

  fun getSelectedEntry(): Int {
    return mSelectedEntry
  }

  override fun getItemCount(): Int {
    mEntries?.let { return mEntries!!.size }
    return 0
  }

  class ViewHolder(val holderView: View) : RecyclerView.ViewHolder(holderView) {
    val title: TextView = holderView.findViewById(R.id.title)
    val radioButton: RadioButton = holderView.findViewById(R.id.radio_button_widget)
  }
}