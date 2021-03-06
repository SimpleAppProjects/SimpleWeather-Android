package com.thewizrd.simpleweather.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R

class KeyEntryPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {
    private var posButtonClickListener: View.OnClickListener? = null
    private var negButtonClickListener: View.OnClickListener? = null

    var key: String? = null
        private set
    private var keyEntry: EditText? = null

    companion object {
        fun newInstance(key: String?): KeyEntryPreferenceDialogFragment {
            val fragment = KeyEntryPreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }

    override fun onCreateDialogView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(R.layout.layout_keyentry_dialog, null)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        keyEntry = view.findViewById(android.R.id.edit)
        keyEntry!!.addTextChangedListener(editTextWatcher)
    }

    private val editTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            key = s.toString()
        }

        override fun afterTextChanged(s: Editable) {}
    }

    fun setPositiveButtonOnClickListener(listener: View.OnClickListener?) {
        posButtonClickListener = listener
    }

    fun setNegativeButtonOnClickListener(listener: View.OnClickListener?) {
        negButtonClickListener = listener
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val alertDialog = getDialog() as AlertDialog?
        alertDialog?.setOnShowListener { dialog ->
            val posButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val negButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            posButton.setOnClickListener(posButtonClickListener)
            if (negButtonClickListener == null) {
                negButton.setOnClickListener { dialog.dismiss() }
            } else {
                negButton.setOnClickListener(negButtonClickListener)
            }
        }

        key = App.instance.settingsManager.getAPIKEY()
    }
}