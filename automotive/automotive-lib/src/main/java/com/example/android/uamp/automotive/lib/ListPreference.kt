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

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.ArrayRes
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference

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
class ListPreference : Preference {

  companion object {
    internal const val ARG_KEY = "AutomotiveListPreferenceKey"
  }

  private val TAG = "ListPreference"
  private var mEntries: Array<CharSequence>? = null
  private var mEntryValues: Array<CharSequence>? = null
  private var mValue: String? = null
  private var mSummary: String? = null
  private var mValueSet: Boolean = false

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
    : super(context, attrs, defStyleAttr, defStyleRes) {
    init(context, attrs, defStyleAttr, defStyleRes)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : this(context, attrs, defStyleAttr, R.style.AutomotivePreference_Preference)

  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.preferenceStyle)

  constructor(context: Context) : this(context, null)

  private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {

    val a = context.obtainStyledAttributes(
      attrs, R.styleable.ListPreference, defStyleAttr, defStyleRes)

    mEntries = TypedArrayUtils.getTextArray(a, R.styleable.ListPreference_entries,
      R.styleable.ListPreference_android_entries)

    mEntryValues = TypedArrayUtils.getTextArray(a, R.styleable.ListPreference_entryValues,
      R.styleable.ListPreference_android_entryValues)

    a.recycle()

    extras.putString(ARG_KEY, key)
    fragment = ListPreferenceFragment::class.qualifiedName
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
    mEntries = entries
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
    return mEntries
  }

  /**
   * The array to find the value to save for a preference when an entry from entries is
   * selected. If a user clicks on the second item in entries, the second item in this array
   * will be saved to the preference.
   *
   * @param entryValues The array to be used as values to save for the preference
   */
  fun setEntryValues(entryValues: Array<CharSequence>) {
    mEntryValues = entryValues
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
    return mEntryValues
  }

  override fun setSummary(summary: CharSequence?) {
    super.setSummary(summary)
    if (summary == null && mSummary != null) {
      mSummary = null
    } else if (summary != null && summary != mSummary) {
      mSummary = summary.toString()
    }
  }

  override fun getSummary(): CharSequence {
    if (summaryProvider != null) {
      return summaryProvider!!.provideSummary(this)
    }
    val entry = getEntry()
    val summary = super.getSummary()
    if (mSummary == null) {
      return summary
    }
    val formattedString = String.format(mSummary!!, entry ?: "")
    if (TextUtils.equals(formattedString, summary)) {
      return summary
    }
    Log.w(TAG,
      "Setting a summary with a String formatting marker is no longer supported." + " You should use a SummaryProvider instead.")
    return formattedString
  }

  /**
   * Sets the value of the key. This should be one of the entries in [.getEntryValues].
   *
   * @param value The value to set for the key
   */
  fun setValue(value: String?) {
    // Always persist/notify the first time.
    val changed = !TextUtils.equals(mValue, value)
    if (changed || !mValueSet) {
      mValue = value
      mValueSet = true
      persistString(value)
      if (changed) {
        notifyChanged()
      }
    }
  }

  /**
   * Returns the value of the key. This should be one of the entries in [.getEntryValues].
   *
   * @return The value of the key
   */
  fun getValue(): String? {
    return mValue
  }

  /**
   * Returns the entry corresponding to the current value.
   *
   * @return The entry corresponding to the current value, or `null`
   */
  fun getEntry(): CharSequence? {
    val index = getValueIndex()
    return if (index >= 0 && mEntries != null) mEntries!![index] else null
  }

  /**
   * Returns the index of the given value (in the entry values array).
   *
   * @param value The value whose index should be returned
   * @return The index of the value, or -1 if not found
   */
  fun findIndexOfValue(value: String?): Int {
    if (value != null && mEntryValues != null) {
      for (i in mEntryValues!!.indices.reversed()) {
        if (mEntryValues!![i] == value) {
          return i
        }
      }
    }
    return -1
  }

  /**
   * Sets the value to the given index from the entry values.
   *
   * @param index The index of the value to set
   */
  fun setValueIndex(index: Int) {
    if (mEntryValues != null) {
      setValue(mEntryValues!![index].toString())
    }
  }

  private fun getValueIndex(): Int {
    return findIndexOfValue(mValue)
  }

  protected override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
    return a.getString(index)
  }

  override fun onSetInitialValue(defaultValue: Any?) {
    setValue(getPersistedString(defaultValue as String?))
  }

  override fun onSaveInstanceState(): Parcelable {
    val superState = super.onSaveInstanceState()
    if (isPersistent) {
      // No need to save instance state since it's persistent
      return superState
    }

    val myState = SavedState(superState)
    myState.mValue = getValue()
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
    setValue(myState.mValue)
  }

  private class SavedState : BaseSavedState {

    internal var mValue: String? = null

    internal constructor(source: Parcel) : super(source) {
      mValue = source.readString()
    }

    internal constructor(superState: Parcelable) : super(superState)

    override fun writeToParcel(dest: Parcel, flags: Int) {
      super.writeToParcel(dest, flags)
      dest.writeString(mValue)
    }

    override fun describeContents(): Int {
      return 0
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

  /**
   * A simple [androidx.preference.Preference.SummaryProvider] implementation for a
   * [ListPreference]. If no value has been set, the summary displayed will be 'Not set',
   * otherwise the summary displayed will be the entry set for this preference.
   */
  class SimpleSummaryProvider : SummaryProvider<ListPreference> {
     companion object {
       val instance = SimpleSummaryProvider()
     }

    override fun provideSummary(preference: ListPreference): CharSequence? {
      return if (TextUtils.isEmpty(preference.getEntry())) {
        preference.context.getString(R.string.not_set)
      } else {
        preference.getEntry()
      }
    }

  }
}
