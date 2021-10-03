package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.preference.EditTextPreference
import com.thewizrd.simpleweather.R

open class WearEditTextPreferenceDialogFragment : WearPreferenceDialogFragment() {
    companion object {
        private const val SAVE_STATE_TEXT = "WearEditTextPreferenceDialogFragment.text"

        fun newInstance(key: String): WearEditTextPreferenceDialogFragment {
            val fragment = WearEditTextPreferenceDialogFragment().apply {
                setStyle(STYLE_NO_FRAME, R.style.WearDialogFragmentTheme)
            }
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    private var mEditText: EditText? = null
    private var mText: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mText = if (savedInstanceState == null) {
            getEditTextPreference().text
        } else {
            savedInstanceState.getCharSequence(SAVE_STATE_TEXT)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TEXT, mText)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        mEditText = view.findViewById(android.R.id.edit)

        checkNotNull(mEditText) {
            "Dialog view must contain an EditText with id" +
                    " @android:id/edit"
        }

        mEditText?.apply {
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                    onDialogClosed(true)
                    hideInputMethod(v)
                    dismiss()
                    return@setOnEditorActionListener true
                }
                false
            }
            requestFocus()
            setText(mText)
            // Place cursor at the end
            setSelection(text.length)
        }
    }

    private fun getEditTextPreference() = getPreference() as EditTextPreference

    override fun onCreateDialogView(context: Context): View {
        return LayoutInflater.from(context).inflate(R.layout.wear_edit_text_layout, null)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val value = mEditText!!.text.toString()
            val preference = getEditTextPreference()
            if (preference.callChangeListener(value)) {
                preference.text = value
            }
        }
    }
}