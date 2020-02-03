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

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.annotation.ArrayRes
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import java.util.Collections
import kotlin.collections.HashSet

const val MULTI_SELECT_LIST_PREFERENCE_ARG_KEY = "AutomotiveMultiSelectListPreferenceKey"

/**
 * A {@link Preference} that displays a list of entries in a {@link ListPreferenceFragment}.
 *
 * This class is taken largely as is from {@link androidx.preference.ListPreference} other than
 * the modification to display entries in a full-screen fragment rather than a dialog.
 *
 * <p>This preference saves a string value. This string will be the value from the
 * {@link #setEntryValues(CharSequence[])} array.
 *
 * @attr name android:entries
 * @attr name android:entryValues
 */
class MultiSelectListPreference : Preference {

  private var entries: Array<CharSequence>? = null
  private var entryValues: Array<CharSequence>? = null
  private val values = HashSet<String>()

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
    : super(context, attrs, defStyleAttr, defStyleRes) {
    init(context, attrs, defStyleAttr, defStyleRes)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : this(context, attrs, defStyleAttr, R.style.AutomotivePreference_Preference)

  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.preferenceStyle)

  constructor(context: Context) : this(context, null)


  private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {

    val attributes = context.obtainStyledAttributes(
      attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes)

    entries = TypedArrayUtils.getTextArray(attributes, R.styleable.ListPreference_entries,
      R.styleable.ListPreference_android_entries)

    entryValues = TypedArrayUtils.getTextArray(attributes, R.styleable.ListPreference_entryValues,
      R.styleable.ListPreference_android_entryValues)

    attributes.recycle()

    extras.putString(MULTI_SELECT_LIST_PREFERENCE_ARG_KEY, key)
    fragment = MultiSelectListPreferenceFragment::class.qualifiedName
  }

  /**
   * Sets the human-readable entries to be shown in the list. This will be shown in subsequent
   * dialogs.
   *
   *
   * Each entry must have a corresponding index in [.setEntryValues].
   *
   * @param entries The entries
   * @see .setEntryValues
   */
  fun setEntries(entries: Array<CharSequence>) {
    this.entries = entries
  }

  /**
   * @param entriesResId The entries array as a resource
   * @see .setEntries
   */
  fun setEntries(@ArrayRes entriesResId: Int) {
    setEntries(context.resources.getTextArray(entriesResId))
  }

  /**
   * The list of entries to be shown in the list in subsequent dialogs.
   *
   * @return The list as an array
   */
  fun getEntries(): Array<CharSequence>? {
    return entries
  }

  /**
   * The array to find the value to save for a preference when an entry from entries is selected. If
   * a user clicks on the second item in entries, the second item in this array will be saved to the
   * preference.
   *
   * @param entryValues The array to be used as values to save for the preference
   */
  fun setEntryValues(entryValues: Array<CharSequence>) {
    this.entryValues = entryValues
  }

  /**
   * @param entryValuesResId The entry values array as a resource
   * @see .setEntryValues
   */
  fun setEntryValues(@ArrayRes entryValuesResId: Int) {
    setEntryValues(context.resources.getTextArray(entryValuesResId))
  }

  /**
   * Returns the array of values to be saved for the preference.
   *
   * @return The array of values
   */
  fun getEntryValues(): Array<CharSequence>? {
    return entryValues
  }

  /**
   * Sets the values for the key. This should contain entries in [.getEntryValues].
   *
   * @param values The values to set for the key
   */
  fun setValues(values: Set<String>) {
    this.values.clear()
    this.values.addAll(values)

    persistStringSet(values)
    notifyChanged()
  }

  /**
   * Retrieves the current values of the key.
   *
   * @return The set of current values
   */
  fun getValues(): Set<String> {
    return values
  }

  /**
   * Returns the index of the given value (in the entry values array).
   *
   * @param value The value whose index should be returned
   * @return The index of the value, or -1 if not found
   */
  fun findIndexOfValue(value: String?): Int {
    if (value != null && entryValues != null) {
      for (i in entryValues!!.indices.reversed()) {
        if (entryValues!![i] == value) {
          return i
        }
      }
    }
    return -1
  }

  protected fun getSelectedItems(): BooleanArray {
    val entries = entryValues
    val entryCount = entries!!.size
    val values = this.values
    val result = BooleanArray(entryCount)

    for (i in 0 until entryCount) {
      result[i] = values.contains(entries[i].toString())
    }

    return result
  }

  override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
    val defaultValues = a.getTextArray(index)
    val result = HashSet<String>()

    for (defaultValue in defaultValues) {
      result.add(defaultValue.toString())
    }

    return result
  }

  override fun onSetInitialValue(defaultValue: Any?) {
    setValues(getPersistedStringSet(defaultValue as Set<String>?))
  }

  override fun onSaveInstanceState(): Parcelable {
    val superState = super.onSaveInstanceState()
    if (isPersistent()) {
      // No need to save instance state
      return superState
    }

    val myState = SavedState(superState)
    myState.mValues = getValues()
    return myState
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    if (state == null || state.javaClass != SavedState::class.java) {
      // Didn't save state for us in onSaveInstanceState
      super.onRestoreInstanceState(state)
      return
    }

    val myState = state as SavedState?
    super.onRestoreInstanceState(myState!!.superState)
    setValues(myState.mValues)
  }

  private class SavedState : BaseSavedState {

    internal lateinit var mValues: Set<String>

    internal constructor(source: Parcel) : super(source) {
      val size = source.readInt()
      mValues = HashSet()
      val strings = arrayOfNulls<String>(size)
      source.readStringArray(strings)

      Collections.addAll<String>(mValues as HashSet<String>, *strings)
    }

    internal constructor(superState: Parcelable) : super(superState)

    override fun writeToParcel(dest: Parcel, flags: Int) {
      super.writeToParcel(dest, flags)
      dest.writeInt(mValues.size)
      dest.writeStringArray(mValues.toTypedArray())
    }

    companion object CREATOR : Parcelable.Creator<SavedState> {
      override fun createFromParcel(parcel: Parcel): SavedState {
        return SavedState(parcel)
      }

      override fun newArray(size: Int): Array<SavedState?> {
        return arrayOfNulls(size)
      }
    }
  }

}