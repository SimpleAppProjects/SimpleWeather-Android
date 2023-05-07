package com.thewizrd.simpleweather.preferences

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ListAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.extras.isPremiumWeatherAPI

class WeatherAPIPreferenceDialogFragment : PreferenceDialogFragmentCompat() {
    companion object {
        private const val SAVE_STATE_INDEX = "WeatherAPIPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "WeatherAPIPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "WeatherAPIPreferenceDialogFragment.entryValues"

        fun newInstance(key: String?): WeatherAPIPreferenceDialogFragment {
            val fragment = WeatherAPIPreferenceDialogFragment()
            val b = Bundle(1).apply {
                putString(ARG_KEY, key)
            }
            fragment.arguments = b
            return fragment
        }
    }

    private var mClickedDialogEntryIndex: Int = 0
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference

            check(preference.entries != null && preference.entryValues != null) {
                "ListPreference requires an entries array and an entryValues array."
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)

        builder.setSingleChoiceItems(
            createListAdapter(),
            mClickedDialogEntryIndex
        ) { dialog, which ->
            mClickedDialogEntryIndex = which

            // Clicking on an item simulates the positive button click, and dismisses
            // the dialog.
            this.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }

        // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
        // dialog instead of the user having to press 'Ok'.
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = mEntryValues!![mClickedDialogEntryIndex].toString()
            val preference = listPreference
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    private fun createListAdapter(): ListAdapter {
        return object : ArrayAdapter<CharSequence>(
            preference.context,
            R.layout.alertdialog_singlechoice_material,
            mEntries!!
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent)

                if (view is CheckedTextView) {
                    val drawables = view.compoundDrawablesRelative
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        drawables[0] /*Start*/,
                        drawables[1] /*Top*/,
                        if (isPremiumWeatherAPI(mEntryValues!![position].toString())) {
                            ContextCompat.getDrawable(view.context, R.drawable.ic_star_24dp)
                                ?.let {
                                    DrawableCompat.setTint(
                                        it,
                                        0xFFFFD700.toInt()
                                    )
                                    it
                                }
                        } else null /*End*/,
                        drawables[3] /*Bottom*/
                    )
                }

                return view
            }
        }
    }
}