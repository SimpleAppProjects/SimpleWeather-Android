package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.weatherdata.auth.*
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.fragments.WearDialogInterface
import com.thewizrd.simpleweather.fragments.WearDialogParams
import com.thewizrd.weather_api.weatherModule

class KeyEntryPreferenceDialogFragment : WearPreferenceDialogFragment() {
    private var posButtonClickListener: WearDialogInterface.OnClickListener? = null
    private var negButtonClickListener: WearDialogInterface.OnClickListener? = null

    val key: String?
        get() = providerKey?.toString()
    var apiProvider: String = ""
        private set
    private var authType: AuthType = AuthType.APIKEY
    private var providerKey: ProviderKey? = null

    companion object {
        private const val SAVE_SATE_PROVIDER = "KeyEntryPreferenceDialogFragment.provider"
        private const val SAVE_SATE_AUTHTYPE = "KeyEntryPreferenceDialogFragment.authType"
        private const val SAVE_SATE_KEY = "KeyEntryPreferenceDialogFragment.key"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            authType = weatherModule.weatherManager.getAuthType(apiProvider)
            val key = settingsManager.getAPIKey(apiProvider)
            providerKey = when (authType) {
                AuthType.APPID_APPCODE -> {
                    ProviderAppKey().apply {
                        key?.let { fromString(it) }
                    }
                }
                AuthType.BASIC -> {
                    BasicAuthProviderKey().apply {
                        key?.let { fromString(it) }
                    }
                }
                else -> {
                    ProviderApiKey().apply {
                        key?.let { fromString(it) }
                    }
                }
            }
        } else {
            apiProvider = savedInstanceState.getString(SAVE_SATE_PROVIDER) ?: ""
            authType = savedInstanceState.getSerializable(SAVE_SATE_AUTHTYPE) as AuthType
            val key = savedInstanceState.getString(SAVE_SATE_KEY)
            providerKey = when (authType) {
                AuthType.APPID_APPCODE -> {
                    ProviderAppKey().apply {
                        key?.let { fromString(it) }
                    }
                }
                AuthType.BASIC -> {
                    BasicAuthProviderKey().apply {
                        key?.let { fromString(it) }
                    }
                }
                else -> {
                    ProviderApiKey().apply {
                        key?.let { fromString(it) }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SAVE_SATE_PROVIDER, apiProvider)
        outState.putSerializable(SAVE_SATE_AUTHTYPE, authType)
        outState.putString(SAVE_SATE_KEY, key)
    }

    override fun onPrepareDialogBuilder(builder: WearDialogParams.Builder) {
        super.onPrepareDialogBuilder(builder)

        builder.setOnPositiveButtonClicked(posButtonClickListener)
        builder.setOnNegativeButtonClicked(negButtonClickListener)
    }

    override fun onCreateDialogView(context: Context): View {
        return LayoutInflater.from(context).run {
            when (authType) {
                AuthType.APPID_APPCODE,
                AuthType.BASIC -> {
                    inflate(R.layout.layout_keyentry2_dialog, null)
                }
                else -> {
                    inflate(R.layout.layout_keyentry_dialog, null)
                }
            }
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        view.findViewById<TextView>(android.R.id.message)?.text = getPreference()?.dialogMessage

        when (authType) {
            AuthType.APPID_APPCODE -> {
                val credentials = providerKey as? ProviderAppKey

                view.findViewById<TextInputLayout>(R.id.keyentry1_layout)?.apply {
                    this.setHint(R.string.hint_appid)
                    editText?.let { editText ->
                        editText.requestFocus()
                        editText.setText(credentials?.appId ?: "")
                        editText.setSelection(editText.text.length)

                        editText.doAfterTextChanged {
                            credentials?.appId = it?.toString() ?: ""
                        }
                    }
                }
                view.findViewById<TextInputLayout>(R.id.keyentry2_layout)?.apply {
                    this.setHint(R.string.hint_appcode)
                    editText?.let { editText ->
                        editText.setText(credentials?.appCode ?: "")

                        editText.doAfterTextChanged {
                            credentials?.appCode = it?.toString() ?: ""
                        }
                    }
                }
            }
            AuthType.BASIC -> {
                val credentials = providerKey as? BasicAuthProviderKey

                view.findViewById<TextInputLayout>(R.id.keyentry1_layout)?.apply {
                    this.setHint(R.string.hint_username)
                    editText?.let { editText ->
                        editText.requestFocus()
                        editText.setText(credentials?.username ?: "")
                        editText.setSelection(editText.text.length)

                        editText.doAfterTextChanged {
                            credentials?.username = it?.toString() ?: ""
                        }
                    }
                }
                view.findViewById<TextInputLayout>(R.id.keyentry2_layout)?.apply {
                    this.setHint(R.string.hint_password)
                    endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                    passwordVisibilityToggleRequested(true)

                    editText?.let { editText ->
                        editText.setText(credentials?.password ?: "")

                        editText.doAfterTextChanged {
                            credentials?.password = it?.toString() ?: ""
                        }
                    }
                }
            }
            else -> {
                view.findViewById<TextInputLayout>(R.id.keyentry1_layout)?.apply {
                    this.setHint(R.string.key_hint)
                    editText?.let { editText ->
                        editText.requestFocus()
                        editText.setText(key ?: "")
                        editText.setSelection(editText.text.length)

                        editText.doAfterTextChanged {
                            (providerKey as? ProviderApiKey)?.run {
                                key = it?.toString() ?: ""
                            }
                        }
                    }
                }
            }
        }
    }

    fun setPositiveButtonOnClickListener(listener: WearDialogInterface.OnClickListener?) {
        posButtonClickListener = listener
    }

    fun setNegativeButtonOnClickListener(listener: WearDialogInterface.OnClickListener?) {
        negButtonClickListener = listener
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            if (getPreference()?.callChangeListener(key) == true) {
                (getPreference() as? WearEditTextPreference)?.let {
                    it.text = key
                }
            }
        }
    }
}