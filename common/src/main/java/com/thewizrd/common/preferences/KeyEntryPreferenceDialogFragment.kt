package com.thewizrd.common.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.textfield.TextInputLayout
import com.thewizrd.common.R
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.weatherdata.auth.*
import com.thewizrd.weather_api.weatherModule

class KeyEntryPreferenceDialogFragment : PreferenceDialogFragmentCompat() {
    private var posButtonClickListener: View.OnClickListener? = null
    private var negButtonClickListener: View.OnClickListener? = null

    val key: String?
        get() = providerKey?.toString()
    var apiProvider: String = ""
        private set
    private var authType: AuthType = AuthType.APIKEY
    private var providerKey: ProviderKey? = null

    private var mEditText: EditText? = null

    private val mShowSoftInputRunnable = Runnable {
        scheduleShowSoftInputInner()
    }
    private var mShowRequestTime: Long = -1

    companion object {
        private const val SAVE_SATE_PROVIDER = "KeyEntryPreferenceDialogFragment.provider"
        private const val SAVE_SATE_AUTHTYPE = "KeyEntryPreferenceDialogFragment.authType"
        private const val SAVE_SATE_KEY = "KeyEntryPreferenceDialogFragment.key"

        private const val SHOW_REQUEST_TIMEOUT = 1000

        fun newInstance(prefKey: String?, apiProvider: String): KeyEntryPreferenceDialogFragment {
            val fragment = KeyEntryPreferenceDialogFragment()
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

    @SuppressLint("InflateParams")
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

        mEditText = view.findViewById(R.id.keyentry1)
        view.findViewById<TextView>(android.R.id.message)?.text = preference?.dialogMessage

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

    fun setPositiveButtonOnClickListener(listener: View.OnClickListener?) {
        posButtonClickListener = listener
    }

    fun setNegativeButtonOnClickListener(listener: View.OnClickListener?) {
        negButtonClickListener = listener
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val alertDialog = getDialog() as? AlertDialog
        alertDialog?.setOnShowListener { d ->
            val posButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val negButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)

            posButton.setOnClickListener {
                posButtonClickListener?.onClick(it)
            }

            negButton.setOnClickListener {
                negButtonClickListener?.onClick(it) ?: d.dismiss()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun needInputMethod(): Boolean {
        return true
    }

    private fun hasPendingShowSoftInputRequest(): Boolean {
        return (mShowRequestTime != -1L && ((mShowRequestTime + SHOW_REQUEST_TIMEOUT)
                > SystemClock.currentThreadTimeMillis()))
    }

    private fun setPendingShowSoftInputRequest(pendingShowSoftInputRequest: Boolean) {
        mShowRequestTime = if (pendingShowSoftInputRequest) {
            SystemClock.currentThreadTimeMillis()
        } else {
            -1
        }
    }

    @SuppressLint("RestrictedApi")
    override fun scheduleShowSoftInput() {
        setPendingShowSoftInputRequest(true)
        scheduleShowSoftInputInner()
    }

    internal fun scheduleShowSoftInputInner() {
        if (hasPendingShowSoftInputRequest()) {
            mEditText?.let {
                if (!it.isFocused) {
                    setPendingShowSoftInputRequest(false)
                    return
                }
                val imm =
                    it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // Schedule showSoftInput once the input connection of the editor established.
                if (imm.showSoftInput(it, 0)) {
                    setPendingShowSoftInputRequest(false)
                } else {
                    it.removeCallbacks(mShowSoftInputRunnable)
                    it.postDelayed(mShowSoftInputRunnable, 50)
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            if (preference.callChangeListener(key)) {
                (preference as? EditTextPreference)?.let {
                    it.text = key
                }
            }
        }
    }
}