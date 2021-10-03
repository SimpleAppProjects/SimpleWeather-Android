package com.thewizrd.simpleweather.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.fragments.WearDialogInterface
import com.thewizrd.simpleweather.fragments.WearDialogParams

class WearListPreferenceDialogFragment : WearPreferenceDialogFragment() {
    companion object {
        private const val SAVE_STATE_INDEX = "WearListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "WearListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "WearListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String): WearListPreferenceDialogFragment {
            val fragment = WearListPreferenceDialogFragment().apply {
                setStyle(STYLE_NO_FRAME, R.style.WearDialogFragmentTheme)
            }
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    var mClickedDialogEntryIndex = 0
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = getListPreference()

            check(!(preference.entries == null || preference.entryValues == null)) {
                "ListPreference requires an entries array and an entryValues array."
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        } else {
            mClickedDialogEntryIndex =
                savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            mEntries =
                savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues =
                savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            SAVE_STATE_INDEX,
            mClickedDialogEntryIndex
        )
        outState.putCharSequenceArray(
            SAVE_STATE_ENTRIES,
            mEntries
        )
        outState.putCharSequenceArray(
            SAVE_STATE_ENTRY_VALUES,
            mEntryValues
        )
    }

    private fun getListPreference() = getPreference() as ListPreference

    override fun onPrepareDialogBuilder(builder: WearDialogParams.Builder) {
        super.onPrepareDialogBuilder(builder)

        builder.setSingleChoiceItems(
            mEntries?.toList(),
            mClickedDialogEntryIndex
        ) { dialog, which ->
            mClickedDialogEntryIndex = which

            this.onClick(dialog, WearDialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }

        builder.hidePositiveButton()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = mEntryValues!![mClickedDialogEntryIndex].toString()
            val preference = getListPreference()
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }
}