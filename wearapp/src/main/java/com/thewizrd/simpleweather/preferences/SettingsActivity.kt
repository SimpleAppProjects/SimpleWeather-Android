package com.thewizrd.simpleweather.preferences

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.FilterComparison
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.arch.core.util.Function
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.WearableLinearLayoutManager
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.wearable.WearConnectionStatus
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.AnalyticsProps
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.extras.enableAdditionalRefreshIntervals
import com.thewizrd.simpleweather.extras.isIconPackSupported
import com.thewizrd.simpleweather.extras.isWeatherAPISupported
import com.thewizrd.simpleweather.extras.navigateToPremiumFragment
import com.thewizrd.simpleweather.extras.navigateUnsupportedIconPack
import com.thewizrd.simpleweather.fragments.WearDialogFragment
import com.thewizrd.simpleweather.fragments.WearDialogParams
import com.thewizrd.simpleweather.helpers.AcceptDenyDialog
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.preferences.iconpreference.WearIconProviderPickerFragment
import com.thewizrd.simpleweather.preferences.radiopreference.CandidateInfo
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference
import com.thewizrd.simpleweather.ui.components.ConfirmationOverlay
import com.thewizrd.simpleweather.ui.theme.WearAppTheme
import com.thewizrd.simpleweather.viewmodels.ConfirmationData
import com.thewizrd.simpleweather.viewmodels.ConfirmationViewModel
import com.thewizrd.simpleweather.viewmodels.SettingsViewModel
import com.thewizrd.simpleweather.wearable.WearableListenerActions
import com.thewizrd.simpleweather.wearable.WearableListenerActions.ACTION_UPDATECONNECTIONSTATUS
import com.thewizrd.simpleweather.wearable.WearableListenerActions.EXTRA_CONNECTIONSTATUS
import com.thewizrd.simpleweather.wearable.complications.WeatherComplicationHelper
import com.thewizrd.simpleweather.wearable.tiles.WeatherTileHelper
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.thewizrd.shared_resources.R as sharedRes

class SettingsActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var fragmentOnBackPressedCallback: OnBackPressedCallback

    override fun attachBaseContext(newBase: Context) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(newBase.getThemeContextOverride(false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("$TAG: onCreate")

        fragmentOnBackPressedCallback =
            object : OnBackPressedCallback(supportFragmentManager.backStackEntryCount > 0) {
                override fun handleOnBackPressed() {
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    }
                }
            }

        supportFragmentManager.addOnBackStackChangedListener {
            fragmentOnBackPressedCallback.isEnabled = supportFragmentManager.backStackEntryCount > 0

            supportFragmentManager.findFragmentById(android.R.id.content)?.let { f ->
                f.view?.requestFocus()
            }
        }

        // Display the fragment as the main content.
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content)

        // Check if fragment exists
        if (fragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : SwipeDismissPreferenceFragment(), OnSharedPreferenceChangeListener {
        companion object {
            // Preference Keys
            private const val KEY_ABOUTAPP = "key_aboutapp"
            private const val KEY_BGLOCATIONACCESS = "key_bglocationaccess"
            private const val KEY_CONNSTATUS = "key_connectionstatus"
            private const val KEY_APIREGISTER = "key_apiregister"
            private const val KEY_UNITS = "key_units"
            private const val KEY_ICONS = "key_icons"
            private const val CATEGORY_GENERAL = "category_general"
            private const val CATEGORY_API = "category_api"
            private const val CATEGORY_SYNC = "category_sync"
        }

        // Preferences
        private lateinit var followGps: SwitchPreference
        private lateinit var intervalPref: ListPreference
        private lateinit var bgLocationPref: Preference
        private lateinit var languagePref: ListPreference
        private lateinit var providerPref: ListPreference
        private lateinit var personalKeyPref: SwitchPreference
        private lateinit var keyEntry: EditTextPreference
        private lateinit var syncPreference: ListPreference
        private lateinit var unitsPref: Preference
        private lateinit var iconsPref: Preference
        private lateinit var connStatusPref: Preference
        private lateinit var registerPref: Preference
        private lateinit var generalCategory: PreferenceCategory
        private lateinit var apiCategory: PreferenceCategory
        private lateinit var syncCategory: PreferenceCategory

        // Intent queue
        private val intentQueue = mutableSetOf<FilterComparison>()

        // Wearable status
        private val settingsViewModel: SettingsViewModel by viewModels()
        private val confirmationViewModel: ConfirmationViewModel by viewModels()

        private lateinit var locationPermissionLauncher: LocationPermissionLauncher

        private lateinit var onBackPressedCallback: OnBackPressedCallback

        override val titleResId: Int
            get() = sharedRes.string.title_activity_settings

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            locationPermissionLauncher = LocationPermissionLauncher(
                this,
                locationCallback = { granted ->
                    if (granted) {
                        // permission was granted, yay!
                        // Do the task you need to do.
                        followGps.isChecked = true
                        settingsManager.setFollowGPS(true)
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        followGps.isChecked = false
                        settingsManager.setFollowGPS(false)
                        showToast(sharedRes.string.error_location_denied, Toast.LENGTH_SHORT)
                    }
                }
            )

            onBackPressedCallback = object : OnBackPressedCallback(isProviderAndKeyInvalid()) {
                override fun handleOnBackPressed() {
                    if (isProviderAndKeyInvalid()) {
                        // Set keyentrypref color to red
                        showToast(sharedRes.string.message_enter_apikey, Toast.LENGTH_SHORT)
                    }
                }
            }

            requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val root = super.onCreateView(inflater, container, savedInstanceState)

            if (root is ViewGroup) {
                root.addView(
                    ComposeView(root.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setContent {
                            val confirmationData by confirmationViewModel.confirmationEventsFlow.collectAsState()

                            WearAppTheme {
                                ConfirmationOverlay(
                                    confirmationData = confirmationData,
                                    onTimeout = { confirmationViewModel.clearFlow() },
                                )
                            }
                        }
                    }
                )
            }

            return root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            viewLifecycleOwner.lifecycleScope.launch {
                settingsViewModel.errorMessagesFlow.collect { error ->
                    when (error) {
                        is ErrorMessage.Resource -> {
                            showToast(error.stringId, Toast.LENGTH_SHORT)
                        }

                        is ErrorMessage.String -> {
                            showToast(error.message, Toast.LENGTH_SHORT)
                        }

                        is ErrorMessage.WeatherError -> {
                            showToast(error.exception.message, Toast.LENGTH_SHORT)
                        }
                    }
                }
            }
        }

        private fun checkBackPressedCallback() {
            onBackPressedCallback.isEnabled = isProviderAndKeyInvalid()
        }

        private fun isProviderAndKeyInvalid(): Boolean {
            return settingsManager.usePersonalKey(providerPref.value) &&
                    settingsManager.getAPIKey(providerPref.value).isNullOrBlank() &&
                    weatherModule.weatherManager.isKeyRequired(providerPref.value)
        }

        override fun onResume() {
            super.onResume()
            AnalyticsLogger.logEvent("SettingsFragment: onResume")

            // Register listener
            appLib.unregisterAppSharedPreferenceListener()
            appLib.registerAppSharedPreferenceListener(this)

            if (!BuildConfig.IS_NONGMS) {
                lifecycleScope.launch {
                    settingsViewModel.eventFlow.collect { (eventType, data) ->
                        when (eventType) {
                            ACTION_UPDATECONNECTIONSTATUS -> {
                                val status = WearConnectionStatus.valueOf(
                                    data.getInt(
                                        EXTRA_CONNECTIONSTATUS,
                                        0
                                    )
                                )
                                updateConnectionPref(status)
                            }

                            WearableListenerActions.ACTION_SHOWCONFIRMATION -> {
                                val jsonData =
                                    data.getString(WearableListenerActions.EXTRA_EVENTDATA)

                                JSONParser.deserializer<ConfirmationData>(
                                    jsonData,
                                    ConfirmationData::class.java
                                )?.let {
                                    confirmationViewModel.showConfirmation(it)
                                }
                            }
                        }
                    }
                }

                settingsViewModel.requestConnectionStatus()
            }

            updateBGLocationPrefState()
        }

        override fun onPause() {
            AnalyticsLogger.logEvent("SettingsFragment: onPause")

            if (isProviderAndKeyInvalid()) {
                // Fallback to supported weather provider
                val API = remoteConfigService.getDefaultWeatherProvider()
                providerPref.value = API
                providerPref.onPreferenceChangeListener?.onPreferenceChange(providerPref, API)
                settingsManager.setAPI(API)
                weatherModule.weatherManager.updateAPI()

                settingsManager.setPersonalKey(API, false)
                settingsManager.setKeyVerified(API, true)
            }

            // Unregister listener
            appLib.unregisterAppSharedPreferenceListener(this)
            appLib.registerAppSharedPreferenceListener()

            intentQueue.forEach { filter ->
                when (filter.intent.action) {
                    CommonActions.ACTION_SETTINGS_UPDATEAPI -> {
                        weatherModule.weatherManager.updateAPI()
                        localBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI)
                        )

                        val api = settingsManager.getAPI()

                        // Log event
                        val bundle = Bundle().apply {
                            putString("API", api)
                            putString(
                                "API_IsInternalKey",
                                (!settingsManager.usePersonalKey(api)).toString()
                            )
                        }
                        AnalyticsLogger.logEvent("Update_API", bundle)
                        AnalyticsLogger.setUserProperty(
                            AnalyticsProps.WEATHER_PROVIDER,
                            api
                        )
                        AnalyticsLogger.setUserProperty(
                            AnalyticsProps.USING_PERSONAL_KEY,
                            settingsManager.usePersonalKey(api)
                        )
                    }
                    CommonActions.ACTION_SETTINGS_UPDATEGPS -> {
                        localBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS)
                        )
                    }
                    CommonActions.ACTION_SETTINGS_UPDATEUNIT -> {
                        localBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT)
                        )
                    }
                    CommonActions.ACTION_SETTINGS_UPDATEDATASYNC -> {
                        localBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC)
                        )
                    }
                    else -> {
                        requireContext().startService(filter.intent)
                    }
                }
            }

            intentQueue.clear()

            super.onPause()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_general, rootKey)

            generalCategory = findPreference(CATEGORY_GENERAL)!!
            apiCategory = findPreference(CATEGORY_API)!!
            syncCategory = findPreference(CATEGORY_SYNC)!!

            findPreference<Preference>(KEY_ABOUTAPP)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    parentFragmentManager.beginTransaction()
                        .add(android.R.id.content, AboutAppFragment())
                        .addToBackStack(null)
                        .commit()

                    true
                }

            followGps = findPreference(SettingsManager.KEY_FOLLOWGPS)!!
            followGps.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    AnalyticsLogger.logEvent("Settings: followGps toggled")

                    if (newValue as Boolean) {
                        if (!preference.context.locationPermissionEnabled()) {
                            locationPermissionLauncher.requestLocationPermission()
                            return@OnPreferenceChangeListener false
                        } else {
                            activity?.let {
                                val locMan =
                                    it.getSystemService(LOCATION_SERVICE) as? LocationManager
                                if (locMan == null || !LocationManagerCompat.isLocationEnabled(
                                        locMan
                                    )
                                ) {
                                    showToast(
                                        sharedRes.string.error_enable_location_services,
                                        Toast.LENGTH_SHORT
                                    )
                                    settingsManager.setFollowGPS(false)
                                    return@OnPreferenceChangeListener false
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() &&
                                        !it.backgroundLocationPermissionEnabled()
                                    ) {
                                        AcceptDenyDialog.Builder(
                                            it
                                        ) { d: DialogInterface?, which: Int ->
                                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                                locationPermissionLauncher.requestBackgroundLocationPermission()
                                            }
                                        }
                                            .setMessage(it.getBackgroundLocationRationale())
                                            .show()

                                        settingsManager.setRequestBGAccess(true)
                                    }
                                }
                            }
                        }
                    }

                    true
                }
            intervalPref = findPreference(SettingsManager.KEY_REFRESHINTERVAL)!!
            if (enableAdditionalRefreshIntervals()) {
                intervalPref.setEntries(sharedRes.array.premium_refreshinterval_entries)
                intervalPref.setEntryValues(sharedRes.array.premium_refreshinterval_values)
            } else {
                intervalPref.setEntries(sharedRes.array.refreshinterval_entries)
                intervalPref.setEntryValues(sharedRes.array.refreshinterval_values)
            }

            bgLocationPref = findPreference(KEY_BGLOCATIONACCESS)!!
            bgLocationPref.setOnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    locationPermissionLauncher.requestBackgroundLocationPermission()
                }
                true
            }
            updateBGLocationPrefState()

            iconsPref = findPreference(KEY_ICONS)!!
            iconsPref.setOnPreferenceClickListener {
                parentFragmentManager.beginTransaction()
                    .add(android.R.id.content, IconsFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }

            unitsPref = findPreference(KEY_UNITS)!!
            unitsPref.setOnPreferenceClickListener {
                parentFragmentManager.beginTransaction()
                    .add(android.R.id.content, UnitsFragment())
                    .addToBackStack(null)
                    .commit()
                true
            }

            languagePref = findPreference(LocaleUtils.KEY_LANGUAGE)!!
            val langCodes = languagePref.entryValues
            val langEntries = arrayOfNulls<CharSequence>(langCodes.size)
            for (i in langCodes.indices) {
                val code = langCodes[i]
                if (TextUtils.isEmpty(code)) {
                    langEntries[i] = getString(sharedRes.string.summary_default)
                } else {
                    val localeCode = code.toString()
                    val locale = LocaleUtils.getLocaleForTag(localeCode)
                    langEntries[i] = locale.getDisplayName(locale)
                }
            }
            languagePref.entries = langEntries

            languagePref.setDefaultValue("")
            languagePref.value = LocaleUtils.getLocaleCode()
            languagePref.summary = localeSummaryFunc.apply(languagePref.value)
            languagePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    LocaleUtils.setLocaleCode(newValue.toString())
                    languagePref.summary = localeSummaryFunc.apply(newValue.toString())
                    true
                }

            keyEntry = findPreference(SettingsManager.KEY_APIKEY)!!
            personalKeyPref = findPreference(SettingsManager.KEY_USEPERSONALKEY)!!
            personalKeyPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    if (newValue as Boolean) {
                        if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null)
                            apiCategory.addPreference(keyEntry)
                        if (apiCategory.findPreference<Preference?>(KEY_APIREGISTER) == null)
                            apiCategory.addPreference(registerPref)
                        keyEntry.isEnabled = true
                    } else {
                        val selectedWProv =
                            weatherModule.weatherManager.getWeatherProvider(providerPref.value)

                    if (!selectedWProv.isKeyRequired() || !selectedWProv.getAPIKey().isNullOrBlank()) {
                        // We're using our own (verified) keys
                        settingsManager.setKeyVerified(providerPref.value, true)
                        settingsManager.setAPI(providerPref.value)
                    }

                    keyEntry.isEnabled = false
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                }

                true
            }

            val providers =
                WeatherAPI.APIs.filter { remoteConfigService.isProviderEnabled(it.value) }
            providerPref = findPreference(SettingsManager.KEY_API)!!
            providerPref.setDefaultValue(remoteConfigService.getDefaultWeatherProvider())

            val entries = arrayOfNulls<String>(providers.size)
            val entryValues = arrayOfNulls<String>(providers.size)

            providers.forEachIndexed { i, it ->
                entries[i] = it.display
                entryValues[i] = it.value
            }

            providerPref.entries = entries
            providerPref.entryValues = entryValues
            providerPref.isPersistent = false
            providerPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                val selectedProvider = newValue.toString()

                if (!isWeatherAPISupported(selectedProvider)) {
                    navigateToPremiumFragment()
                    return@OnPreferenceChangeListener false
                }

                val pref = preference as ListPreference
                val selectedWProv =
                    weatherModule.weatherManager.getWeatherProvider(selectedProvider)

                if (selectedWProv.isKeyRequired()) {
                    if (selectedWProv.getAPIKey().isNullOrBlank()) {
                        settingsManager.setPersonalKey(selectedProvider, true)
                        personalKeyPref.isChecked = true
                        personalKeyPref.isEnabled =
                            selectedProvider == WeatherAPI.OPENWEATHERMAP && !BuildConfig.IS_NONGMS
                        keyEntry.isEnabled = false
                        apiCategory.removePreference(keyEntry)
                        apiCategory.removePreference(registerPref)
                    } else {
                        personalKeyPref.isEnabled = true
                    }

                    if (!settingsManager.usePersonalKey(selectedProvider)) {
                        // We're using our own (verified) keys
                        settingsManager.setKeyVerified(selectedProvider, true)
                        keyEntry.isEnabled = false
                        apiCategory.removePreference(keyEntry)
                        apiCategory.removePreference(registerPref)
                    } else {
                        // User is using personal (unverified) keys
                        settingsManager.setKeyVerified(selectedProvider, false)

                        // Show dialog to set key
                        runWithView(Dispatchers.Main) {
                            onDisplayPreferenceDialog(keyEntry)
                        }

                        keyEntry.isEnabled = true

                        if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null)
                            apiCategory.addPreference(keyEntry)
                        if (apiCategory.findPreference<Preference?>(KEY_APIREGISTER) == null)
                            apiCategory.addPreference(registerPref)
                    }

                    if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_USEPERSONALKEY) == null)
                        apiCategory.addPreference(personalKeyPref)

                    // Reset to old value if not verified
                    if (!settingsManager.isKeyVerified(selectedProvider))
                        settingsManager.setAPI(pref.value)
                    else
                        settingsManager.setAPI(selectedProvider)

                    var providerEntry: ProviderEntry? = null
                    for (entry in providers) {
                        if (entry.value == selectedProvider) {
                            providerEntry = entry
                            break
                        }
                    }
                    updateKeySummary(providerEntry!!.display)
                    updateRegisterLink(providerEntry.value)
                } else {
                    settingsManager.setKeyVerified(selectedProvider, false)
                    keyEntry.isEnabled = false
                    personalKeyPref.isEnabled = false

                    settingsManager.setAPI(selectedProvider)
                    // Clear API KEY entry to avoid issues
                    settingsManager.setAPIKey(selectedProvider, "")

                    apiCategory.removePreference(personalKeyPref)
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                    updateKeySummary()
                    updateRegisterLink()
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    checkBackPressedCallback()
                }

                true
            }

            registerPref = findPreference(KEY_APIREGISTER)!!
            // Ignore click for non-gms
            if (!BuildConfig.IS_NONGMS) {
                registerPref.onPreferenceClickListener = registerPrefClickListener
            }

            // Set key as verified if API Key is req for API and its set
            if (weatherModule.weatherManager.isKeyRequired()) {
                keyEntry.isEnabled = true

                val provider = providerPref.value

                if (!settingsManager.getAPIKey().isNullOrBlank() &&
                    !settingsManager.isKeyVerified(provider)
                ) {
                    settingsManager.setKeyVerified(provider, true)
                }

                if (weatherModule.weatherManager.getAPIKey().isNullOrBlank()) {
                    settingsManager.setPersonalKey(provider, true)
                    personalKeyPref.isChecked = true
                    personalKeyPref.isEnabled = false
                    keyEntry.isEnabled = false
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                } else {
                    personalKeyPref.isEnabled = true
                }

                if (!settingsManager.usePersonalKey(provider)) {
                    // We're using our own (verified) keys
                    settingsManager.setKeyVerified(provider, true)
                    keyEntry.isEnabled = false
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                } else {
                    keyEntry.isEnabled = true

                    if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null)
                        apiCategory.addPreference(keyEntry)
                    if (apiCategory.findPreference<Preference?>(KEY_APIREGISTER) == null)
                        apiCategory.addPreference(registerPref)
                }
            } else {
                keyEntry.isEnabled = false
                personalKeyPref.isEnabled = false
                apiCategory.removePreference(personalKeyPref)
                apiCategory.removePreference(keyEntry)
                apiCategory.removePreference(registerPref)
                settingsManager.setKeyVerified(providerPref.value, false)
                // Clear API KEY entry to avoid issues
                settingsManager.setAPIKey("")
            }

            updateKeySummary()
            updateRegisterLink()

            syncCategory.isVisible = !BuildConfig.IS_NONGMS

            syncPreference = findPreference(SettingsManager.KEY_DATASYNC)!!
            syncPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    val newVal = newValue.toString().toInt()

                    val args = Bundle().apply {
                        putInt("mode", newVal)
                    }
                    AnalyticsLogger.logEvent("Settings: sync pref changed", args)

                    val pref = preference as ListPreference
                    pref.summary = pref.entries[newVal]

                    enableSyncedSettings(WearableDataSync.valueOf(newVal) == WearableDataSync.OFF)
                    true
                }
            syncPreference.summary = syncPreference.entries[syncPreference.value.toInt()]
            enableSyncedSettings(settingsManager.getDataSync() == WearableDataSync.OFF)

            connStatusPref = findPreference(KEY_CONNSTATUS)!!
            connStatusPref.isVisible = !BuildConfig.IS_NONGMS
        }

        private fun updateBGLocationPrefState() {
            bgLocationPref.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !requireContext().backgroundLocationPermissionEnabled() &&
                    settingsManager.useFollowGPS()
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference is WearEditTextPreference && (SettingsManager.KEY_APIKEY == preference.key)) {
                val TAG = KeyEntryPreferenceDialogFragment::class.java.name

                if (parentFragmentManager.findFragmentByTag(TAG) != null) {
                    return
                }

                val fragment = KeyEntryPreferenceDialogFragment.newInstance(
                    preference.getKey(),
                    providerPref.value
                )
                fragment.setPositiveButtonOnClickListener { dialog, _ ->
                    runWithView {
                        val provider = fragment.apiProvider
                        val key = fragment.key

                        try {
                            if (weatherModule.weatherManager.isKeyValid(key, provider)) {
                                settingsManager.setAPIKey(provider, key)
                                settingsManager.setAPI(provider)
                                settingsManager.setKeyVerified(provider, true)
                                settingsManager.setPersonalKey(provider, true)

                                updateKeySummary()

                                dialog.dismiss()
                            } else {
                                showToast(sharedRes.string.message_keyinvalid, Toast.LENGTH_SHORT)
                            }
                        } catch (e: WeatherException) {
                            Logger.writeLine(Log.ERROR, e)
                            showToast(e.message, Toast.LENGTH_SHORT)
                        }
                    }
                }

                runWithView {
                    runCatching {
                        fragment.setTargetFragment(this@SettingsFragment, 0)
                        fragment.show(
                            parentFragmentManager,
                            KeyEntryPreferenceDialogFragment::class.java.name
                        )
                    }
                }
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }

        private val localeSummaryFunc: Function<String, CharSequence> = Function { input ->
            if (input.isNullOrBlank()) {
                getString(sharedRes.string.summary_default)
            } else {
                LocaleUtils.getLocaleDisplayName()
            }
        }

        private fun enableSyncedSettings(enable: Boolean) {
            generalCategory.isEnabled = enable
            apiCategory.isEnabled = enable
        }

        private val connStatusPrefClickListener = Preference.OnPreferenceClickListener {
            lifecycleScope.launch {
                settingsViewModel.openPlayStore(showAnimation = true)
            }
            true
        }

        private val registerPrefClickListener = Preference.OnPreferenceClickListener { preference ->
            val intentAndroid = Intent(preference.intent)
                .addCategory(Intent.CATEGORY_BROWSABLE)

            lifecycleScope.launch {
                val success = runCatching {
                    settingsViewModel.startRemoteActivity(intentAndroid)
                }.getOrDefault(false)

                if (success) {
                    confirmationViewModel.showOpenOnPhone()
                } else {
                    confirmationViewModel.showFailure()
                }
            }

            true
        }

        private fun updateKeySummary(providerAPI: CharSequence? = providerPref.entry) {
            if (!settingsManager.getAPIKey(providerPref.value).isNullOrBlank()) {
                val keyVerified = settingsManager.isKeyVerified(providerPref.value)

                val colorSpan = ForegroundColorSpan(if (keyVerified) Color.GREEN else Color.RED)
                val summary: Spannable = SpannableString(
                    if (keyVerified) getString(sharedRes.string.message_keyverified) else getString(
                        sharedRes.string.message_keyinvalid
                    )
                )
                summary.setSpan(colorSpan, 0, summary.length, 0)
                keyEntry.summary = summary
            } else {
                keyEntry.summary =
                    getString(
                        sharedRes.string.pref_summary_apikey,
                        providerAPI ?: WeatherIcons.EM_DASH
                    )
            }
        }

        private fun updateRegisterLink(providerAPI: CharSequence? = providerPref.value) {
            var prov: ProviderEntry? = null
            for (provider in WeatherAPI.APIs) {
                if (provider.value == providerAPI.toString()) {
                    prov = provider
                    break
                }
            }

            if (prov != null) {
                registerPref.intent = Intent(Intent.ACTION_VIEW)
                    .setData(prov.apiRegisterURL.toUri())
            }
        }

        private fun enqueueIntent(intent: Intent?): Boolean {
            return intent?.let {
                intentQueue.add(FilterComparison(it))
            } ?: false
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            if (key.isNullOrBlank()) return

            when (key) {
                SettingsManager.KEY_API -> {
                    enqueueIntent(Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI))
                }
                SettingsManager.KEY_FOLLOWGPS -> {
                    enqueueIntent(Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS))
                }
                SettingsManager.KEY_DATASYNC -> {
                    enqueueIntent(Intent(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC))
                }
            }
        }

        private fun updateConnectionPref(status: WearConnectionStatus) {
            when (status) {
                WearConnectionStatus.DISCONNECTED -> {
                    connStatusPref.setSummary(R.string.status_disconnected)
                    connStatusPref.onPreferenceClickListener = null
                }
                WearConnectionStatus.CONNECTING -> {
                    connStatusPref.setSummary(R.string.status_connecting)
                    connStatusPref.onPreferenceClickListener = null
                }
                WearConnectionStatus.APPNOTINSTALLED -> {
                    connStatusPref.setSummary(R.string.status_notinstalled)
                    connStatusPref.onPreferenceClickListener = connStatusPrefClickListener
                }
                WearConnectionStatus.CONNECTED -> {
                    connStatusPref.setSummary(R.string.status_connected)
                    connStatusPref.onPreferenceClickListener = null
                }
            }
        }
    }

    class UnitsFragment : SwipeDismissPreferenceFragment() {
        companion object {
            private const val KEY_RESETUNITS = "key_resetunits"
        }

        private lateinit var tempUnitPref: ListPreference
        private lateinit var speedUnitPref: ListPreference
        private lateinit var distanceUnitPref: ListPreference
        private lateinit var precipationUnitPref: ListPreference
        private lateinit var pressureUnitPref: ListPreference

        override val titleResId: Int
            get() = sharedRes.string.pref_title_units

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_units, rootKey)

            preferenceScreen.setTitle(sharedRes.string.pref_title_units)

            tempUnitPref = findPreference(SettingsManager.KEY_TEMPUNIT)!!
            speedUnitPref = findPreference(SettingsManager.KEY_SPEEDUNIT)!!
            distanceUnitPref = findPreference(SettingsManager.KEY_DISTANCEUNIT)!!
            precipationUnitPref = findPreference(SettingsManager.KEY_PRECIPITATIONUNIT)!!
            pressureUnitPref = findPreference(SettingsManager.KEY_PRESSUREUNIT)!!

            findPreference<Preference>(KEY_RESETUNITS)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    activity?.let {
                        val params = WearDialogParams.Builder(it)
                            .setTitle(sharedRes.string.pref_title_units)
                            .setItems(sharedRes.array.default_units) { dialog, which ->
                                val isFahrenheit = which == 0
                                tempUnitPref.value =
                                    if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS
                                speedUnitPref.value =
                                    if (isFahrenheit) Units.MILES_PER_HOUR else Units.KILOMETERS_PER_HOUR
                                distanceUnitPref.value =
                                    if (isFahrenheit) Units.MILES else Units.KILOMETERS
                                precipationUnitPref.value =
                                    if (isFahrenheit) Units.INCHES else Units.MILLIMETERS
                                pressureUnitPref.value =
                                    if (isFahrenheit) Units.INHG else Units.MILLIBAR
                                dialog.dismiss()

                                localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT))
                            }
                            .hidePositiveButton()
                            .build()

                        WearDialogFragment.show(parentFragmentManager, params, null)
                    }
                    true
                }
        }
    }

    class IconsFragment : WearIconProviderPickerFragment() {
        override val titleResId: Int
            get() = sharedRes.string.pref_title_icons

        override fun getDefaultKey(): String {
            return settingsManager.getIconsProvider()
        }

        override fun setDefaultKey(key: String?): Boolean {
            if (TextUtils.isEmpty(key)) {
                return false
            }
            settingsManager.setIconsProvider(key)
            return true
        }

        override fun bindPreferenceExtra(
            pref: RadioButtonPreference?,
            key: String?,
            info: CandidateInfo?,
            defaultKey: String?,
            systemDefaultKey: String?
        ) {
            super.bindPreferenceExtra(pref, key, info, defaultKey, systemDefaultKey)
            pref?.isPersistent = false
        }

        override fun onSelectionPerformed(success: Boolean) {
            super.onSelectionPerformed(success)
            sharedDeps.weatherIconsManager.updateIconProvider()

            // Update tiles and complications
            WeatherComplicationHelper.requestComplicationUpdateAll(requireContext())
            WeatherTileHelper.requestTileUpdateAll(requireContext())
        }

        override fun onRadioButtonConfirmed(selectedKey: String?) {
            if (!isIconPackSupported(selectedKey)) {
                navigateUnsupportedIconPack()
                return
            }
            super.onRadioButtonConfirmed(selectedKey)
            AnalyticsLogger.logEvent("W_Icon_Selected", Bundle().apply {
                putString("iconProvider", selectedKey)
            })
            AnalyticsLogger.setUserProperty(AnalyticsProps.ICON_PROVIDER, selectedKey)
        }
    }

    class AboutAppFragment : SwipeDismissPreferenceFragment() {
        companion object {
            // Preference Keys
            private const val KEY_ABOUTCREDITS = "key_aboutcredits"
            private const val KEY_ABOUTOSLIBS = "key_aboutoslibs"
            private const val KEY_ABOUTVERSION = "key_aboutversion"
        }

        override val titleResId: Int
            get() = sharedRes.string.pref_title_about

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_aboutapp, rootKey)

            findPreference<Preference>(KEY_ABOUTCREDITS)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { // Display the fragment as the main content.
                    parentFragmentManager.beginTransaction()
                        .add(android.R.id.content, CreditsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

            findPreference<Preference>(KEY_ABOUTOSLIBS)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { // Display the fragment as the main content.
                    parentFragmentManager.beginTransaction()
                        .add(android.R.id.content, OSSCreditsFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

            runCatching {
                val packageInfo =
                    requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                findPreference<Preference>(KEY_ABOUTVERSION)!!.summary =
                    String.format("v%s", packageInfo.versionName)
            }
        }
    }

    class CreditsFragment : SwipeDismissPreferenceFragment() {
        private lateinit var remoteActivityHelper: RemoteActivityHelper
        private val confirmationViewModel: ConfirmationViewModel by viewModels()

        override val titleResId: Int
            get() = sharedRes.string.pref_title_credits

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            remoteActivityHelper = RemoteActivityHelper(requireContext())
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val root = super.onCreateView(inflater, container, savedInstanceState)

            if (root is ViewGroup) {
                root.addView(
                    ComposeView(root.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setContent {
                            val confirmationData by confirmationViewModel.confirmationEventsFlow.collectAsState()

                            WearAppTheme {
                                ConfirmationOverlay(
                                    confirmationData = confirmationData,
                                    onTimeout = { confirmationViewModel.clearFlow() },
                                )
                            }
                        }
                    }
                )
            }

            return root
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_credits, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.intent != null && !BuildConfig.IS_NONGMS) {
                runWithView {
                    val success = runCatching {
                        remoteActivityHelper.startRemoteActivity(
                            preference.intent!!
                                .setAction(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                        )
                        true
                    }.getOrDefault(false)

                    if (success) {
                        confirmationViewModel.showOpenOnPhone()
                    } else {
                        confirmationViewModel.showFailure()
                    }
                }

                return true
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    class OSSCreditsFragment : SwipeDismissPreferenceFragment() {
        override val titleResId: Int
            get() = sharedRes.string.pref_title_oslibs

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_oslibs, rootKey)
        }

        override fun onCreateLayoutManager(): RecyclerView.LayoutManager {
            return WearableLinearLayoutManager(context, null)
        }
    }
}