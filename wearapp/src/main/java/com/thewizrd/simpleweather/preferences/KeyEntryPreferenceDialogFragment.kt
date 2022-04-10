package com.thewizrd.simpleweather.preferences

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.fragments.WearDialogInterface
import com.thewizrd.simpleweather.fragments.WearDialogParams

class KeyEntryPreferenceDialogFragment : WearEditTextPreferenceDialogFragment() {
    private var posButtonClickListener: WearDialogInterface.OnClickListener? = null
    private var negButtonClickListener: WearDialogInterface.OnClickListener? = null

    var key: String? = null
        private set
    var apiProvider: String = ""
        private set

    companion object {
        fun newInstance(prefKey: String?, apiProvider: String): KeyEntryPreferenceDialogFragment {
            val fragment = KeyEntryPreferenceDialogFragment().apply {
                setStyle(STYLE_NO_FRAME, R.style.WearDialogFragmentTheme)
            }
            fragment.arguments = Bundle(1).apply {
                putString(ARG_KEY, prefKey)
            }
            fragment.apiProvider = apiProvider
            return fragment
        }
    }

    fun setPositiveButtonOnClickListener(listener: WearDialogInterface.OnClickListener?) {
        posButtonClickListener = listener
    }

    fun setNegativeButtonOnClickListener(listener: WearDialogInterface.OnClickListener?) {
        negButtonClickListener = listener
    }

    override fun onPrepareDialogBuilder(builder: WearDialogParams.Builder) {
        super.onPrepareDialogBuilder(builder)

        builder.setOnPositiveButtonClicked(posButtonClickListener)
        builder.setOnNegativeButtonClicked(negButtonClickListener)

        key = SimpleLibrary.instance.app.settingsManager.getAPIKey(apiProvider)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        view.findViewById<EditText>(android.R.id.edit)?.doAfterTextChanged {
            key = it?.toString()
        }
    }
}