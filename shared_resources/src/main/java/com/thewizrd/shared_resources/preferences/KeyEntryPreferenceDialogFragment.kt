package com.thewizrd.shared_resources.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary

class KeyEntryPreferenceDialogFragment : EditTextPreferenceDialogFragmentCompat() {
    private var posButtonClickListener: View.OnClickListener? = null
    private var negButtonClickListener: View.OnClickListener? = null

    var key: String? = null
        private set
    var apiProvider: String = ""
        private set

    companion object {
        fun newInstance(prefKey: String?, apiProvider: String): KeyEntryPreferenceDialogFragment {
            val fragment = KeyEntryPreferenceDialogFragment()
            fragment.arguments = Bundle(1).apply {
                putString(ARG_KEY, prefKey)
            }
            fragment.apiProvider = apiProvider
            return fragment
        }
    }

    override fun onCreateDialogView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(R.layout.layout_keyentry_dialog, null)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        view.findViewById<EditText>(android.R.id.edit)?.doAfterTextChanged {
            key = it?.toString()
        }

        view.findViewById<TextView>(android.R.id.message)?.text = preference?.dialogMessage
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

        key = SimpleLibrary.instance.app.settingsManager.getAPIKey(apiProvider)
    }
}