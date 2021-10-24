package com.thewizrd.simpleweather.widgets

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.thewizrd.simpleweather.R
import java.util.*
import kotlin.collections.LinkedHashSet

internal class MultiLocationPreferenceDialogFragment : PreferenceDialogFragmentCompat() {
    private var mNewValues: MutableSet<String> = LinkedHashSet()
    private var mPreferenceChanged = false
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    companion object {
        private const val MAX_LOCATION_ITEMS = 5

        private const val SAVE_STATE_VALUES = "MultiLocationPreferenceDialogFragment.values"
        private const val SAVE_STATE_CHANGED = "MultiLocationPreferenceDialogFragment.changed"
        private const val SAVE_STATE_ENTRIES = "MultiLocationPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES =
            "MultiLocationPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): MultiLocationPreferenceDialogFragment {
            val fragment = MultiLocationPreferenceDialogFragment()
            val b = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
            fragment.arguments = b
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val preference = listPreference

            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                    "MultiSelectListPreference requires an entries array and " +
                            "an entryValues array."
                )
            }

            mNewValues.clear()
            mNewValues.addAll(preference.values)
            mPreferenceChanged = false
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        } else {
            mNewValues.clear()
            mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES)!!)
            mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false)
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(SAVE_STATE_VALUES, ArrayList(mNewValues))
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    protected val listPreference: MultiSelectListPreference
        get() = preference as MultiSelectListPreference

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val entryCount = listPreference.entryValues.size
        val checkedItems = BooleanArray(entryCount)
        for (i in 0 until entryCount) {
            checkedItems[i] = mNewValues.contains(mEntryValues!![i].toString())
        }

        builder.setMultiChoiceItems(mEntries, checkedItems) { dialog, which, isChecked ->
            val listView = (dialog as? AlertDialog)?.listView
            val checkedItemCount = mNewValues.size

            mPreferenceChanged = if (isChecked) {
                if (checkedItemCount >= MAX_LOCATION_ITEMS) {
                    Toast.makeText(
                        requireContext(),
                        R.string.prompt_max_locations_allowed,
                        Toast.LENGTH_SHORT
                    ).show()
                    checkedItems[which] = false
                    listView?.setItemChecked(which, false)
                    mPreferenceChanged or false
                } else {
                    mPreferenceChanged or mNewValues.add(
                        mEntryValues!![which].toString()
                    )
                }
            } else {
                mPreferenceChanged or mNewValues.remove(
                    mEntryValues!![which].toString()
                )
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mPreferenceChanged) {
            val preference = listPreference
            if (preference.callChangeListener(mNewValues)) {
                preference.values = mNewValues
            }
        }
        mPreferenceChanged = false
    }
}