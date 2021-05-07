package com.thewizrd.simpleweather.setup

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupProvidersBinding
import com.thewizrd.simpleweather.preferences.CustomPreferenceFragmentCompat
import com.thewizrd.simpleweather.preferences.KeyEntryPreferenceDialogFragment
import com.thewizrd.simpleweather.preferences.SettingsFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.stepper.StepperDataManager
import com.thewizrd.simpleweather.stepper.StepperFragment

class SetupProviderFragment : CustomPreferenceFragmentCompat(), StepperFragment {
    private val KEY_APIREGISTER = "key_apiregister"

    private lateinit var binding: FragmentSetupProvidersBinding

    private lateinit var providerPref: ListPreference
    private lateinit var keyEntry: EditTextPreference
    private lateinit var registerPref: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        settingsManager.setPersonalKey(true)
    }

    override fun createSnackManager(): SnackbarManager {
        val mStepperNavBar = appCompatActivity.findViewById<View>(R.id.bottom_nav_bar)
        val mSnackMgr = SnackbarManager(binding.root)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        mSnackMgr.setAnchorView(mStepperNavBar)
        return mSnackMgr
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupProvidersBinding.inflate(inflater, container, false)
        val root = binding.root as ViewGroup
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        binding.fragmentContainer.addView(inflatedView)

        setDivider(ColorDrawable(ContextUtils.getColor(root.context, R.attr.colorPrimary)))
        setDividerHeight(ContextUtils.dpToPx(root.context, 1f).toInt())

        return root
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_provider_setup, null)

        keyEntry = findPreference(SettingsManager.KEY_APIKEY)!!

        val prefGroup = keyEntry.parent!!

        val providers = WeatherAPI.APIs
        providerPref = findPreference(SettingsManager.KEY_API)!!

        val entries = arrayOfNulls<String>(providers.size)
        val entryValues = arrayOfNulls<String>(providers.size)

        for (i in providers.indices) {
            entries[i] = providers[i].display
            entryValues[i] = providers[i].value
        }

        providerPref.entries = entries
        providerPref.entryValues = entryValues
        providerPref.isPersistent = false
        providerPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val pref = preference as ListPreference
                val selectedWProv = WeatherManager.getProvider(newValue.toString())

                if (selectedWProv.isKeyRequired()) {
                    if (selectedWProv.getAPIKey().isNullOrBlank()) {
                        settingsManager.setPersonalKey(true)
                        keyEntry.isEnabled = false
                        prefGroup.removePreference(keyEntry)
                        prefGroup.removePreference(registerPref)
                    }

                    if (!settingsManager.usePersonalKey()) {
                        // We're using our own (verified) keys
                        settingsManager.setKeyVerified(true)
                        keyEntry.isEnabled = false
                        prefGroup.removePreference(keyEntry)
                        prefGroup.removePreference(registerPref)
                    } else {
                        // User is using personal (unverified) keys
                        settingsManager.setKeyVerified(false)
                        // Clear API KEY entry to avoid issues
                        settingsManager.setAPIKEY("")
                        keyEntry.isEnabled = true
                        if (prefGroup.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null) prefGroup.addPreference(
                            keyEntry
                        )
                        if (prefGroup.findPreference<Preference?>(KEY_APIREGISTER) == null) prefGroup.addPreference(
                            registerPref
                        )
                    }

                    // Reset to old value if not verified
                    if (!settingsManager.isKeyVerified())
                        settingsManager.setAPI(pref.value)
                    else
                        settingsManager.setAPI(newValue.toString())

                    val providerEntry =
                        providers.find { entry -> entry.value == newValue.toString() }
                    updateKeySummary(providerEntry!!.display)
                    updateRegisterLink(providerEntry.value)
                } else {
                    settingsManager.setKeyVerified(false)
                    keyEntry.isEnabled = false

                    settingsManager.setAPI(newValue.toString())
                    // Clear API KEY entry to avoid issues
                    settingsManager.setAPIKEY("")

                    prefGroup.removePreference(keyEntry)
                    prefGroup.removePreference(registerPref)
                    updateKeySummary()
                    updateRegisterLink()
                }

                true
            }

        registerPref = findPreference(KEY_APIREGISTER)!!

        // Set key as verified if API Key is req for API and its set
        if (WeatherManager.instance.isKeyRequired()) {
            keyEntry.isEnabled = true

            if (!settingsManager.getAPIKEY().isNullOrBlank() && !settingsManager.isKeyVerified())
                settingsManager.setKeyVerified(true)

            if (WeatherManager.instance.getAPIKey().isNullOrBlank()) {
                settingsManager.setPersonalKey(true)
                keyEntry.isEnabled = false
                prefGroup.removePreference(keyEntry)
                prefGroup.removePreference(registerPref)
            }

            if (!settingsManager.usePersonalKey()) {
                // We're using our own (verified) keys
                settingsManager.setKeyVerified(true)
                keyEntry.isEnabled = false
                prefGroup.removePreference(keyEntry)
                prefGroup.removePreference(registerPref)
            } else {
                keyEntry.isEnabled = true

                if (prefGroup.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null)
                    prefGroup.addPreference(keyEntry)
                if (prefGroup.findPreference<Preference?>(KEY_APIREGISTER) == null)
                    prefGroup.addPreference(registerPref)
            }
        } else {
            keyEntry.isEnabled = false
            prefGroup.removePreference(keyEntry)
            prefGroup.removePreference(registerPref)
            settingsManager.setKeyVerified(false)
            // Clear API KEY entry to avoid issues
            settingsManager.setAPIKEY("")
        }

        updateKeySummary()
        updateRegisterLink()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val TAG = "KeyEntryPreferenceDialogFragment"

        if (parentFragmentManager.findFragmentByTag(TAG) != null) {
            return
        }

        if (preference is EditTextPreference && (SettingsManager.KEY_APIKEY == preference.getKey())) {
            val fragment = KeyEntryPreferenceDialogFragment.newInstance(preference.getKey())
            fragment.setPositiveButtonOnClickListener {
                runWithView {
                    val key = fragment.key

                    val API = providerPref.value
                    try {
                        if (WeatherManager.isKeyValid(key, API)) {
                            settingsManager.setAPIKEY(key)
                            settingsManager.setAPI(API)

                            settingsManager.setKeyVerified(true)
                            updateKeySummary()

                            fragment.dialog!!.dismiss()
                        } else {
                            Toast.makeText(
                                appCompatActivity,
                                R.string.message_keyinvalid,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: WeatherException) {
                        Logger.writeLine(Log.ERROR, e)
                    }
                }
            }

            fragment.setTargetFragment(this, 0)
            fragment.show(parentFragmentManager, TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun updateKeySummary(providerAPI: CharSequence = providerPref.entry) {
        if (!settingsManager.getAPIKEY().isNullOrBlank()) {
            val keyVerified = settingsManager.isKeyVerified()
            val colorSpan = ForegroundColorSpan(if (keyVerified) Color.GREEN else Color.RED)
            val summary = SpannableString(
                if (keyVerified) getString(R.string.message_keyverified) else getString(R.string.message_keyinvalid)
            )
            summary.setSpan(colorSpan, 0, summary.length, 0)
            keyEntry.summary = summary
        } else {
            val colorSpan = ForegroundColorSpan(Color.RED)
            val summary = SpannableString(getString(R.string.pref_summary_apikey, providerAPI))
            summary.setSpan(colorSpan, 0, summary.length, 0)
            keyEntry.summary = summary
        }
    }

    private fun updateRegisterLink(providerAPI: CharSequence = providerPref.value) {
        var prov: ProviderEntry? = null
        for (provider: ProviderEntry in WeatherAPI.APIs) {
            if ((provider.value == providerAPI.toString())) {
                prov = provider
                break
            }
        }

        if (prov != null) {
            registerPref.intent = Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(prov.apiRegisterURL))
        }
    }

    override fun canGoNext(): Boolean {
        if (settingsManager.usePersonalKey()
            && settingsManager.getAPIKEY().isNullOrBlank()
            && WeatherManager.isKeyRequired(providerPref.value)
        ) {
            showSnackbar(Snackbar.make(R.string.message_enter_apikey, Snackbar.Duration.LONG), null)
            return false
        }

        return true
    }
}