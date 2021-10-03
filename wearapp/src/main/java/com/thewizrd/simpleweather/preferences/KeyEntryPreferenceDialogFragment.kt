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

    companion object {
        fun newInstance(key: String?): KeyEntryPreferenceDialogFragment {
            val fragment = KeyEntryPreferenceDialogFragment().apply {
                setStyle(STYLE_NO_FRAME, R.style.WearDialogFragmentTheme)
            }
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
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

        key = SimpleLibrary.instance.app.settingsManager.getAPIKEY()
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        view.findViewById<EditText>(android.R.id.edit)?.doAfterTextChanged {
            key = it?.toString()
        }
    }
}