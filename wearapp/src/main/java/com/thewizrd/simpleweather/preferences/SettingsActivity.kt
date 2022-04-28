package com.thewizrd.simpleweather.preferences

import android.Manifest
import android.content.*
import android.content.Intent.FilterComparison
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.arch.core.util.Function
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.thewizrd.common.helpers.*
import com.thewizrd.common.wearable.WearConnectionStatus
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.store.PlayStoreUtils
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.extras.isIconPackSupported
import com.thewizrd.simpleweather.extras.isWeatherAPISupported
import com.thewizrd.simpleweather.extras.navigateToPremiumFragment
import com.thewizrd.simpleweather.extras.navigateUnsupportedIconPack
import com.thewizrd.simpleweather.fragments.WearDialogFragment
import com.thewizrd.simpleweather.fragments.WearDialogParams
import com.thewizrd.simpleweather.helpers.AcceptDenyDialog
import com.thewizrd.simpleweather.helpers.showConfirmationOverlay
import com.thewizrd.simpleweather.preferences.iconpreference.IconProviderPickerFragment
import com.thewizrd.simpleweather.preferences.radiopreference.CandidateInfo
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference
import com.thewizrd.simpleweather.wearable.WearableListenerActivity
import com.thewizrd.simpleweather.wearable.WeatherComplicationHelper
import com.thewizrd.simpleweather.wearable.WeatherTileHelper
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class SettingsActivity : WearableListenerActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override lateinit var broadcastReceiver: BroadcastReceiver
    override lateinit var intentFilter: IntentFilter

    override fun attachBaseContext(newBase: Context) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(newBase.getThemeContextOverride(false))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("$TAG: onCreate")

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_SENDCONNECTIONSTATUS == intent.action) {
                    lifecycleScope.launch { updateConnectionStatus() }
                }
            }
        }

        intentFilter = IntentFilter(ACTION_SENDCONNECTIONSTATUS)

        remoteActivityHelper = RemoteActivityHelper(this)

        // Display the fragment as the main content.
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content)

        // Check if fragment exists
        if (fragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    override fun onBackPressed() {
        val current = supportFragmentManager.findFragmentById(android.R.id.content)

        var fragBackPressedListener: OnBackPressedFragmentListener? = null
        if (current is OnBackPressedFragmentListener)
            fragBackPressedListener = current

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                super.onBackPressed()
            }
        }
    }

    class SettingsFragment : SwipeDismissPreferenceFragment(), OnSharedPreferenceChangeListener,
        OnBackPressedFragmentListener {
        companion object {
            private const val PERMISSION_LOCATION_REQUEST_CODE = 0
            private const val PERMISSION_BGLOCATION_REQUEST_CODE = 1

            // Preference Keys
            private const val KEY_ABOUTAPP = "key_aboutapp"
            private const val KEY_BGLOCATIONACCESS = "key_bglocationaccess"
            private const val KEY_CONNSTATUS = "key_connectionstatus"
            private const val KEY_APIREGISTER = "key_apiregister"
            private const val KEY_UNITS = "key_units"
            private const val KEY_ICONS = "key_icons"
            private const val CATEGORY_GENERAL = "category_general"
            private const val CATEGORY_API = "category_api"
        }

        // Preferences
        private lateinit var followGps: SwitchPreference
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

        // Intent queue
        private var intentQueue: HashSet<FilterComparison>? = null

        // Wearable status
        private var mConnectionStatus = WearConnectionStatus.DISCONNECTED
        private var statusReceiver: BroadcastReceiver? = null

        protected lateinit var remoteActivityHelper: RemoteActivityHelper

        override val titleResId: Int
            get() = R.string.title_activity_settings

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            remoteActivityHelper = RemoteActivityHelper(requireContext())
        }

        override fun onBackPressed(): Boolean {
            if (settingsManager.usePersonalKey() &&
                settingsManager.getAPIKey(providerPref.value).isNullOrBlank() &&
                weatherModule.weatherManager.isKeyRequired(providerPref.value)
            ) {
                // Set keyentrypref color to red
                showToast(R.string.message_enter_apikey, Toast.LENGTH_SHORT)
                return true
            }

            return false
        }

        override fun onResume() {
            super.onResume()

            AnalyticsLogger.logEvent("SettingsFragment: onResume")

            // Register listener
            appLib.unregisterAppSharedPreferenceListener()
            appLib.registerAppSharedPreferenceListener(this)
            // Initialize queue
            intentQueue = HashSet()

            statusReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (ACTION_UPDATECONNECTIONSTATUS == intent.action) {
                        mConnectionStatus = WearConnectionStatus.valueOf(
                            intent.getIntExtra(
                                EXTRA_CONNECTIONSTATUS,
                                0
                            )
                        )
                        updateConnectionPref()
                    }
                }
            }

            localBroadcastManager.registerReceiver(
                statusReceiver!!,
                IntentFilter(ACTION_UPDATECONNECTIONSTATUS)
            )
            localBroadcastManager.sendBroadcast(
                Intent(ACTION_SENDCONNECTIONSTATUS)
            )
        }

        override fun onPause() {
            AnalyticsLogger.logEvent("SettingsFragment: onPause")

            if (settingsManager.usePersonalKey() &&
                settingsManager.getAPIKey(providerPref.value).isNullOrBlank() &&
                weatherModule.weatherManager.isKeyRequired(providerPref.value)
            ) {
                // Fallback to supported weather provider
                val API = remoteConfigService.getDefaultWeatherProvider()
                providerPref.value = API
                providerPref.onPreferenceChangeListener?.onPreferenceChange(providerPref, API)
                settingsManager.setAPI(API)
                weatherModule.weatherManager.updateAPI()

                settingsManager.setPersonalKey(false)
                settingsManager.setKeyVerified(API, true)
            }

            // Unregister listener
            appLib.unregisterAppSharedPreferenceListener(this)
            appLib.registerAppSharedPreferenceListener()

            localBroadcastManager.unregisterReceiver(statusReceiver!!)

            for (filter in intentQueue!!) {
                when (filter.intent.action) {
                    CommonActions.ACTION_SETTINGS_UPDATEAPI -> {
                        weatherModule.weatherManager.updateAPI()
                        localBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI)
                        )

                        // Log event
                        val bundle = Bundle().apply {
                            putString("API", settingsManager.getAPI())
                            putString(
                                "API_IsInternalKey",
                                (!settingsManager.usePersonalKey()).toString()
                            )
                        }
                        AnalyticsLogger.logEvent("Update_API", bundle)
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

            super.onPause()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_general, rootKey)

            generalCategory = findPreference(CATEGORY_GENERAL)!!
            apiCategory = findPreference(CATEGORY_API)!!

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
                            requestLocationPermission(PERMISSION_LOCATION_REQUEST_CODE)
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
                                        R.string.error_enable_location_services,
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
                                                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                                                    it.openAppSettingsActivity()
                                                } else {
                                                    requestBackgroundLocationPermission(
                                                        PERMISSION_BGLOCATION_REQUEST_CODE
                                                    )
                                                }
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

            bgLocationPref = findPreference(KEY_BGLOCATIONACCESS)!!
            bgLocationPref.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !requireContext().backgroundLocationPermissionEnabled() &&
                    settingsManager.useFollowGPS()
            bgLocationPref.setOnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        it.context.openAppSettingsActivity()
                    } else {
                        requestBackgroundLocationPermission(
                            PERMISSION_BGLOCATION_REQUEST_CODE
                        )
                    }
                }
                true
            }

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
                    langEntries[i] = getString(R.string.summary_default)
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
                        settingsManager.setPersonalKey(true)
                        personalKeyPref.isChecked = true
                        personalKeyPref.isEnabled =
                            selectedProvider == WeatherAPI.OPENWEATHERMAP && !BuildConfig.IS_NONGMS
                        keyEntry.isEnabled = false
                        apiCategory.removePreference(keyEntry)
                        apiCategory.removePreference(registerPref)
                    } else {
                        personalKeyPref.isEnabled = true
                    }

                    if (!settingsManager.usePersonalKey()) {
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

                true
            }

            registerPref = findPreference(KEY_APIREGISTER)!!
            registerPref.onPreferenceClickListener = registerPrefClickListener

            // Set key as verified if API Key is req for API and its set
            if (weatherModule.weatherManager.isKeyRequired()) {
                keyEntry.isEnabled = true

                if (!settingsManager.getAPIKey().isNullOrBlank() &&
                    !settingsManager.isKeyVerified(providerPref.value)
                ) {
                    settingsManager.setKeyVerified(providerPref.value, true)
                }

                if (weatherModule.weatherManager.getAPIKey().isNullOrBlank()) {
                    settingsManager.setPersonalKey(true)
                    personalKeyPref.isChecked = true
                    personalKeyPref.isEnabled = false
                    keyEntry.isEnabled = false
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                } else {
                    personalKeyPref.isEnabled = true
                }

                if (!settingsManager.usePersonalKey()) {
                    // We're using our own (verified) keys
                    settingsManager.setKeyVerified(providerPref.value, true)
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
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            if (preference is WearEditTextPreference && (SettingsManager.KEY_APIKEY == preference.getKey())) {
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

                                updateKeySummary()

                                dialog.dismiss()
                            } else {
                                showToast(R.string.message_keyinvalid, Toast.LENGTH_SHORT)
                            }
                        } catch (e: WeatherException) {
                            Logger.writeLine(Log.ERROR, e)
                            showToast(e.message, Toast.LENGTH_SHORT)
                        }
                    }
                }

                fragment.setTargetFragment(this, 0)
                fragment.show(
                    parentFragmentManager,
                    KeyEntryPreferenceDialogFragment::class.java.name
                )
            } else {
                super.onDisplayPreferenceDialog(preference)
            }
        }

        private val localeSummaryFunc: Function<String, CharSequence> = Function { input ->
            if (input.isNullOrBlank()) {
                getString(R.string.summary_default)
            } else {
                LocaleUtils.getLocaleDisplayName()
            }
        }

        private fun enableSyncedSettings(enable: Boolean) {
            generalCategory.isEnabled = enable
            apiCategory.isEnabled = enable
        }

        private val connStatusPrefClickListener = Preference.OnPreferenceClickListener {
            val intentAndroid = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(PlayStoreUtils.getPlayStoreURI())

            lifecycleScope.launch {
                runCatching {
                    remoteActivityHelper.startRemoteActivity(intentAndroid)
                        .await()

                    showConfirmationOverlay(true)
                }.onFailure {
                    if (it !is CancellationException) {
                        showConfirmationOverlay(false)
                    }
                }
            }

            true
        }

        private val registerPrefClickListener = Preference.OnPreferenceClickListener { preference ->
            val intentAndroid = Intent(preference.intent)
                .addCategory(Intent.CATEGORY_BROWSABLE)

            lifecycleScope.launch {
                runCatching {
                    remoteActivityHelper.startRemoteActivity(intentAndroid)
                        .await()

                    showConfirmationOverlay(true)
                }.onFailure {
                    if (it !is CancellationException) {
                        showConfirmationOverlay(false)
                    }
                }
            }

            true
        }

        private fun updateKeySummary(providerAPI: CharSequence? = providerPref.entry) {
            if (!settingsManager.getAPIKey(providerPref.value).isNullOrBlank()) {
                val keyVerified = settingsManager.isKeyVerified(providerPref.value)

                val colorSpan = ForegroundColorSpan(if (keyVerified) Color.GREEN else Color.RED)
                val summary: Spannable = SpannableString(
                    if (keyVerified) getString(R.string.message_keyverified) else getString(R.string.message_keyinvalid)
                )
                summary.setSpan(colorSpan, 0, summary.length, 0)
                keyEntry.summary = summary
            } else {
                keyEntry.summary =
                    getString(R.string.pref_summary_apikey, providerAPI ?: WeatherIcons.EM_DASH)
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
                        .setData(Uri.parse(prov.apiRegisterURL))
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            when (requestCode) {
                PERMISSION_LOCATION_REQUEST_CODE -> {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted, yay!
                        // Do the task you need to do.
                        followGps.isChecked = true
                        settingsManager.setFollowGPS(true)
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        followGps.isChecked = false
                        settingsManager.setFollowGPS(false)
                        showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                    }
                    return
                }
                PERMISSION_BGLOCATION_REQUEST_CODE -> {
                    // no-op
                }
            }
        }

        private fun enqueueIntent(intent: Intent?): Boolean {
            return if (intent == null) false else intentQueue!!.add(FilterComparison(intent))
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

        private fun updateConnectionPref() {
            when (mConnectionStatus) {
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
            get() = R.string.pref_title_units

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_units, rootKey)

            preferenceScreen.setTitle(R.string.pref_title_units)

            tempUnitPref = findPreference(SettingsManager.KEY_TEMPUNIT)!!
            speedUnitPref = findPreference(SettingsManager.KEY_SPEEDUNIT)!!
            distanceUnitPref = findPreference(SettingsManager.KEY_DISTANCEUNIT)!!
            precipationUnitPref = findPreference(SettingsManager.KEY_PRECIPITATIONUNIT)!!
            pressureUnitPref = findPreference(SettingsManager.KEY_PRESSUREUNIT)!!

            findPreference<Preference>(KEY_RESETUNITS)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    activity?.let {
                        val params = WearDialogParams.Builder(it)
                            .setTitle(R.string.pref_title_units)
                            .setItems(R.array.default_units) { dialog, which ->
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

    class IconsFragment : IconProviderPickerFragment() {
        override val titleResId: Int
            get() = R.string.pref_title_icons

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
            get() = R.string.pref_title_about

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

        override val titleResId: Int
            get() = R.string.pref_title_credits

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            remoteActivityHelper = RemoteActivityHelper(requireContext())
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_credits, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.intent != null) {
                runWithView {
                    runCatching {
                        remoteActivityHelper.startRemoteActivity(
                            preference.intent!!
                                .setAction(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                        )

                        // Show open on phone animation
                        ConfirmationOverlay()
                            .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                            .setMessage(preference.context.getString(R.string.message_openedonphone))
                            .showAbove(requireView())
                    }
                }

                return true
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    class OSSCreditsFragment : SwipeDismissPreferenceFragment() {
        override val titleResId: Int
            get() = R.string.pref_title_oslibs

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_oslibs, rootKey)
        }
    }
}