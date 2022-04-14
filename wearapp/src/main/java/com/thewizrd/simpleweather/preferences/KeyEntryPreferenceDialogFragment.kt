package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        key = SimpleLibrary.instance.app.settingsManager.getAPIKey(apiProvider)
    }

    override fun onPrepareDialogBuilder(builder: WearDialogParams.Builder) {
        super.onPrepareDialogBuilder(builder)

        builder.setOnPositiveButtonClicked(posButtonClickListener)
        builder.setOnNegativeButtonClicked(negButtonClickListener)
    }

    override fun onCreateDialogView(context: Context): View {
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(R.layout.layout_keyentry_dialog, null)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        view.findViewById<EditText>(android.R.id.edit)?.apply {
            this.text.replace(0, this.text.length, key ?: "")

            doAfterTextChanged {
                key = it?.toString()
            }
        }

        view.findViewById<TextView>(android.R.id.message)?.text = getPreference()?.dialogMessage
    }
}