package com.thewizrd.simpleweather.setup

import android.app.Activity
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
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.thewizrd.common.preferences.KeyEntryPreferenceDialogFragment
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.preferences.DevSettingsEnabler
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupProvidersBinding
import com.thewizrd.simpleweather.preferences.CustomPreferenceFragmentCompat
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.stepper.StepperFragment
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers

class SetupProviderFragment : CustomPreferenceFragmentCompat(), StepperFragment {
    companion object {
        private const val KEY_APIREGISTER = "key_apiregister"
    }

    private lateinit var binding: FragmentSetupProvidersBinding

    private lateinit var providerPref: ListPreference
    private lateinit var keyEntry: EditTextPreference
    private lateinit var registerPref: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        settingsManager.setPersonalKey(true)
        if (BuildConfig.IS_NONGMS) {
            DevSettingsEnabler.setDevSettingsEnabled(requireContext(), true)
        }
    }

    override fun createSnackManager(activity: Activity): SnackbarManager {
        val mStepperNavBar = activity.findViewById<View>(R.id.bottom_nav_bar)
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

        setDivider(ColorDrawable(root.context.getAttrColor(R.attr.colorPrimary)))
        setDividerHeight(root.context.dpToPx(1f).toInt())

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

        providers.forEachIndexed { i, it ->
            entries[i] = it.display
            entryValues[i] = it.value
        }

        providerPref.entries = entries
        providerPref.entryValues = entryValues
        providerPref.isPersistent = false
        providerPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                val selectedProvider = newValue.toString()

                val pref = preference as ListPreference
                val selectedWProv =
                    weatherModule.weatherManager.getWeatherProvider(selectedProvider)

                if (selectedWProv.isKeyRequired()) {
                    if (selectedWProv.getAPIKey().isNullOrBlank()) {
                        settingsManager.setPersonalKey(true)
                        keyEntry.isEnabled = false
                        prefGroup.removePreference(keyEntry)
                        prefGroup.removePreference(registerPref)
                    }

                    if (!settingsManager.usePersonalKey()) {
                        // We're using our own (verified) keys
                        settingsManager.setKeyVerified(selectedProvider, true)
                        keyEntry.isEnabled = false
                        prefGroup.removePreference(keyEntry)
                        prefGroup.removePreference(registerPref)
                    } else {
                        // User is using personal (unverified) keys
                        settingsManager.setKeyVerified(selectedProvider, false)

                        // Show dialog to set key
                        runWithView(Dispatchers.Main) {
                            onDisplayPreferenceDialog(keyEntry)
                        }

                        keyEntry.isEnabled = true
                        if (prefGroup.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null) prefGroup.addPreference(
                            keyEntry
                        )
                        if (prefGroup.findPreference<Preference?>(KEY_APIREGISTER) == null) prefGroup.addPreference(
                            registerPref
                        )
                    }

                    // Reset to old value if not verified
                    if (!settingsManager.isKeyVerified(selectedProvider))
                        settingsManager.setAPI(pref.value)
                    else
                        settingsManager.setAPI(selectedProvider)

                    val providerEntry =
                        providers.find { entry -> entry.value == selectedProvider }
                    updateKeySummary(providerEntry!!.display)
                    updateRegisterLink(providerEntry.value)
                } else {
                    settingsManager.setKeyVerified(selectedProvider, false)
                    keyEntry.isEnabled = false

                    settingsManager.setAPI(selectedProvider)
                    // Clear API KEY entry to avoid issues
                    settingsManager.setAPIKey(selectedProvider, "")

                    prefGroup.removePreference(keyEntry)
                    prefGroup.removePreference(registerPref)
                    updateKeySummary()
                    updateRegisterLink()
                }

                true
            }

        registerPref = findPreference(KEY_APIREGISTER)!!

        providerPref.value = settingsManager.getAPI()

        // Set key as verified if API Key is req for API and its set
        if (weatherModule.weatherManager.isKeyRequired()) {
            keyEntry.isEnabled = true

            if (!settingsManager.getAPIKey(providerPref.value).isNullOrBlank() &&
                !settingsManager.isKeyVerified(providerPref.value)
            ) {
                settingsManager.setKeyVerified(providerPref.value, true)
            }

            if (weatherModule.weatherManager.getAPIKey().isNullOrBlank()) {
                settingsManager.setPersonalKey(true)
                keyEntry.isEnabled = false
                prefGroup.removePreference(keyEntry)
                prefGroup.removePreference(registerPref)
            }

            if (!settingsManager.usePersonalKey()) {
                // We're using our own (verified) keys
                settingsManager.setKeyVerified(providerPref.value, true)
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
            settingsManager.setKeyVerified(providerPref.value, false)
            // Clear API KEY entry to avoid issues
            settingsManager.setAPIKey(providerPref.value, "")
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
            val fragment = KeyEntryPreferenceDialogFragment.newInstance(
                preference.getKey(),
                providerPref.value
            )
            fragment.setPositiveButtonOnClickListener {
                val provider = fragment.apiProvider
                val key = fragment.key

                runWithView {
                    try {
                        if (weatherModule.weatherManager.isKeyValid(key, provider)) {
                            settingsManager.setAPIKey(provider, key)
                            settingsManager.setAPI(provider)
                            settingsManager.setKeyVerified(provider, true)

                            updateKeySummary()

                            fragment.dialog?.dismiss()
                        } else {
                            showToast(R.string.message_keyinvalid, Toast.LENGTH_SHORT)
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

    private fun updateKeySummary(providerAPI: CharSequence? = providerPref.entry) {
        if (!settingsManager.getAPIKey(providerPref.value).isNullOrBlank()) {
            val keyVerified = settingsManager.isKeyVerified(providerPref.value)
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
            && settingsManager.getAPIKey(providerPref.value).isNullOrBlank()
            && weatherModule.weatherManager.isKeyRequired(providerPref.value)
        ) {
            context?.let {
                showSnackbar(
                    Snackbar.make(
                        it,
                        R.string.message_enter_apikey,
                        Snackbar.Duration.LONG
                    )
                )
            }
            return false
        }

        return true
    }
}