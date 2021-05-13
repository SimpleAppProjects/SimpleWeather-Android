@file:Suppress("DEPRECATION")

package com.thewizrd.simpleweather.preferences

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.Intent.FilterComparison
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.preference.*
import android.support.wearable.view.ConfirmationOverlay
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.arch.core.util.Function
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.wearable.intent.RemoteIntent
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.store.PlayStoreUtils
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.wearable.WearConnectionStatus
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.extras.isIconPackSupported
import com.thewizrd.simpleweather.extras.isWeatherAPISupported
import com.thewizrd.simpleweather.extras.navigateToPremiumFragment
import com.thewizrd.simpleweather.extras.navigateUnsupportedIconPack
import com.thewizrd.simpleweather.fragments.SwipeDismissPreferenceFragment
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver
import com.thewizrd.simpleweather.preferences.iconpreference.IconProviderPickerFragment
import com.thewizrd.simpleweather.preferences.radiopreference.CandidateInfo
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference
import com.thewizrd.simpleweather.wearable.WearableListenerActivity
import com.thewizrd.simpleweather.wearable.WeatherComplicationHelper
import com.thewizrd.simpleweather.wearable.WeatherTileHelper
import kotlinx.coroutines.launch
import java.util.*

class SettingsActivity : WearableListenerActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override lateinit var broadcastReceiver: BroadcastReceiver
    override lateinit var intentFilter: IntentFilter

    override fun attachBaseContext(newBase: Context) {
        // Use night mode resources (needed for external weather icons)
        super.attachBaseContext(ContextUtils.getThemeContextOverride(newBase, false))
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

        // Display the fragment as the main content.
        val fragment = fragmentManager.findFragmentById(android.R.id.content)

        // Check if fragment exists
        if (fragment == null) {
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, SettingsFragment())
                    .commit()
        }
    }

    override fun onBackPressed() {
        val current = fragmentManager.findFragmentById(android.R.id.content)
        var fragBackPressedListener: OnBackPressedFragmentListener? = null
        if (current is OnBackPressedFragmentListener) fragBackPressedListener = current

        // If fragment doesn't handle onBackPressed event fallback to this impl
        if (fragBackPressedListener == null || !fragBackPressedListener.onBackPressed()) {
            super.onBackPressed()
        }
    }

    class SettingsFragment : SwipeDismissPreferenceFragment(), OnSharedPreferenceChangeListener, OnBackPressedFragmentListener {
        companion object {
            private const val PERMISSION_LOCATION_REQUEST_CODE = 0

            // Preference Keys
            private const val KEY_ABOUTAPP = "key_aboutapp"
            private const val KEY_CONNSTATUS = "key_connectionstatus"
            private const val KEY_APIREGISTER = "key_apiregister"
            private const val KEY_UNITS = "key_units"
            private const val KEY_ICONS = "key_icons"
            private const val CATEGORY_GENERAL = "category_general"
            private const val CATEGORY_API = "category_api"
        }

        // Preferences
        private lateinit var followGps: SwitchPreference
        private lateinit var languagePref: ListPreference
        private lateinit var providerPref: ListPreference
        private lateinit var personalKeyPref: SwitchPreference
        private lateinit var keyEntry: KeyEntryPreference
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

        override fun onBackPressed(): Boolean {
            if (settingsManager.usePersonalKey() &&
                    settingsManager.getAPIKEY().isNullOrBlank() &&
                    WeatherManager.isKeyRequired(providerPref.value)) {
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
            val app = App.instance
            app.unregisterAppSharedPreferenceListener()
            app.registerAppSharedPreferenceListener(this)
            // Initialize queue
            intentQueue = HashSet()

            statusReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (ACTION_UPDATECONNECTIONSTATUS == intent.action) {
                        mConnectionStatus = WearConnectionStatus.valueOf(intent.getIntExtra(EXTRA_CONNECTIONSTATUS, 0))
                        updateConnectionPref()
                    }
                }
            }

            val mBroadcastMgr = LocalBroadcastManager.getInstance(parentActivity!!)
            mBroadcastMgr.registerReceiver(statusReceiver!!,
                    IntentFilter(ACTION_UPDATECONNECTIONSTATUS))
            mBroadcastMgr.sendBroadcast(
                    Intent(ACTION_SENDCONNECTIONSTATUS))
        }

        override fun onPause() {
            AnalyticsLogger.logEvent("SettingsFragment: onPause")

            if (settingsManager.usePersonalKey() && settingsManager.getAPIKEY().isNullOrBlank() && WeatherManager.isKeyRequired(providerPref.value)) {
                // Fallback to supported weather provider
                val API = RemoteConfig.getDefaultWeatherProvider()
                providerPref.value = API
                providerPref.onPreferenceChangeListener
                        .onPreferenceChange(providerPref, API)
                settingsManager.setAPI(API)
                WeatherManager.instance.updateAPI()

                settingsManager.setPersonalKey(false)
                settingsManager.setKeyVerified(true)
            }

            // Unregister listener
            val app = App.instance
            app.unregisterAppSharedPreferenceListener(this)
            app.registerAppSharedPreferenceListener()

            val mLocalBroadcastManager = LocalBroadcastManager.getInstance(parentActivity!!)
            mLocalBroadcastManager.unregisterReceiver(statusReceiver!!)

            for (filter in intentQueue!!) {
                if (CommonActions.ACTION_SETTINGS_UPDATEAPI == filter.intent.action) {
                    WeatherManager.instance.updateAPI()
                    mLocalBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI))

                    // Log event
                    val bundle = Bundle().apply {
                        putString("API", settingsManager.getAPI())
                        putString("API_IsInternalKey", (!settingsManager.usePersonalKey()).toString())
                    }
                    AnalyticsLogger.logEvent("Update_API", bundle)
                } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS == filter.intent.action) {
                    mLocalBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEGPS))
                } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT == filter.intent.action) {
                    mLocalBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT))
                } else if (CommonActions.ACTION_SETTINGS_UPDATEDATASYNC == filter.intent.action) {
                    mLocalBroadcastManager.sendBroadcast(
                            Intent(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC))
                } else {
                    parentActivity!!.startService(filter.intent)
                }
            }

            super.onPause()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?) {
            addPreferencesFromResource(R.xml.pref_general)

            generalCategory = findPreference(CATEGORY_GENERAL) as PreferenceCategory
            apiCategory = findPreference(CATEGORY_API) as PreferenceCategory

            findPreference(KEY_ABOUTAPP).onPreferenceClickListener = Preference.OnPreferenceClickListener { // Display the fragment as the main content.
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, AboutAppFragment())
                        .addToBackStack(null)
                        .commit()
                true
            }

            followGps = findPreference(SettingsManager.KEY_FOLLOWGPS) as SwitchPreference
            followGps.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                AnalyticsLogger.logEvent("Settings: followGps toggled")

                if (newValue as Boolean) {
                    if (ContextCompat.checkSelfPermission(parentActivity!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(parentActivity!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                                PERMISSION_LOCATION_REQUEST_CODE)
                        return@OnPreferenceChangeListener false
                    } else {
                        val locMan = parentActivity!!.getSystemService(LOCATION_SERVICE) as LocationManager?
                        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                            showToast(R.string.error_enable_location_services, Toast.LENGTH_SHORT)
                            settingsManager.setFollowGPS(false)
                            return@OnPreferenceChangeListener false
                        }
                    }
                }

                true
            }

            iconsPref = findPreference(KEY_ICONS)
            iconsPref.setOnPreferenceClickListener {
                // Display the fragment as the main content.
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, IconsFragment())
                        .addToBackStack(null)
                        .commit()
                true
            }

            unitsPref = findPreference(KEY_UNITS)
            unitsPref.setOnPreferenceClickListener {
                // Display the fragment as the main content.
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, UnitsFragment())
                        .addToBackStack(null)
                        .commit()
                true
            }

            languagePref = findPreference(LocaleUtils.KEY_LANGUAGE) as ListPreference
            val langCodes = languagePref.entryValues
            val langEntries = arrayOfNulls<CharSequence>(langCodes.size)
            for (i in langCodes.indices) {
                val code = langCodes[i]
                if (TextUtils.isEmpty(code)) {
                    langEntries[i] = getString(R.string.summary_default)
                } else {
                    val localeCode = code.toString()
                    val locale = Locale(localeCode)
                    langEntries[i] = locale.getDisplayName(locale)
                }
            }
            languagePref.entries = langEntries

            languagePref.setDefaultValue("")
            languagePref.value = LocaleUtils.getLocaleCode()
            languagePref.summary = localeSummaryFunc.apply(languagePref.value)
            languagePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                LocaleUtils.setLocaleCode(newValue.toString())
                languagePref.summary = localeSummaryFunc.apply(newValue.toString())
                true
            }

            keyEntry = findPreference(SettingsManager.KEY_APIKEY) as KeyEntryPreference
            keyEntry.setPositiveButtonOnClickListener { dialog, which ->
                runWithView {
                    val key = keyEntry.apiKey

                    val API = providerPref.value
                    try {
                        if (WeatherManager.isKeyValid(key, API)) {
                            settingsManager.setAPIKEY(key)
                            settingsManager.setAPI(API)

                            settingsManager.setKeyVerified(true)
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
            personalKeyPref = findPreference(SettingsManager.KEY_USEPERSONALKEY) as SwitchPreference
            personalKeyPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue as Boolean) {
                    if (apiCategory.findPreference(SettingsManager.KEY_APIKEY) == null)
                        apiCategory.addPreference(keyEntry)
                    if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                        apiCategory.addPreference(registerPref)
                    keyEntry.isEnabled = true
                } else {
                    val selectedWProv = WeatherManager.getProvider(providerPref.value)

                    if (!selectedWProv.isKeyRequired() || !selectedWProv.getAPIKey().isNullOrBlank()) {
                        // We're using our own (verified) keys
                        settingsManager.setKeyVerified(true)
                        settingsManager.setAPI(providerPref.value)
                    }

                    keyEntry.isEnabled = false
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                }

                true
            }

            val providers = WeatherAPI.APIs
            providerPref = findPreference(SettingsManager.KEY_API) as ListPreference

            val entries = arrayOfNulls<String>(providers.size)
            val entryValues = arrayOfNulls<String>(providers.size)

            for (i in providers.indices) {
                entries[i] = providers[i].display
                entryValues[i] = providers[i].value
            }

            providerPref.entries = entries
            providerPref.entryValues = entryValues
            providerPref.isPersistent = false
            providerPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                if (!isWeatherAPISupported(newValue.toString())) {
                    navigateToPremiumFragment()
                    return@OnPreferenceChangeListener false
                }

                val pref = preference as ListPreference
                val selectedWProv = WeatherManager.getProvider(newValue.toString())

                if (selectedWProv.isKeyRequired()) {
                    if (selectedWProv.getAPIKey().isNullOrBlank()) {
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
                        settingsManager.setKeyVerified(true)
                        keyEntry.isEnabled = false
                        apiCategory.removePreference(keyEntry)
                        apiCategory.removePreference(registerPref)
                    } else {
                        // User is using personal (unverified) keys
                        settingsManager.setKeyVerified(false)
                        // Clear API KEY entry to avoid issues
                        settingsManager.setAPIKEY("")

                        keyEntry.isEnabled = true

                        if (apiCategory.findPreference(SettingsManager.KEY_APIKEY) == null)
                            apiCategory.addPreference(keyEntry)
                        if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                            apiCategory.addPreference(registerPref)
                    }

                    if (apiCategory.findPreference(SettingsManager.KEY_USEPERSONALKEY) == null)
                        apiCategory.addPreference(personalKeyPref)

                    // Reset to old value if not verified
                    if (!settingsManager.isKeyVerified())
                        settingsManager.setAPI(pref.value)
                    else
                        settingsManager.setAPI(newValue.toString())

                    var providerEntry: ProviderEntry? = null
                    for (entry in providers) {
                        if (entry.value == newValue.toString()) {
                            providerEntry = entry
                            break
                        }
                    }
                    updateKeySummary(providerEntry!!.display)
                    updateRegisterLink(providerEntry.value)
                } else {
                    settingsManager.setKeyVerified(false)
                    keyEntry.isEnabled = false
                    personalKeyPref.isEnabled = false

                    settingsManager.setAPI(newValue.toString())
                    // Clear API KEY entry to avoid issues
                    settingsManager.setAPIKEY("")

                    apiCategory.removePreference(personalKeyPref)
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                    updateKeySummary()
                    updateRegisterLink()
                }

                true
            }

            registerPref = findPreference(KEY_APIREGISTER)
            registerPref.onPreferenceClickListener = registerPrefClickListener

            // Set key as verified if API Key is req for API and its set
            if (WeatherManager.instance.isKeyRequired()) {
                keyEntry.isEnabled = true

                if (!settingsManager.getAPIKEY().isNullOrBlank() && !settingsManager.isKeyVerified())
                    settingsManager.setKeyVerified(true)

                if (WeatherManager.instance.getAPIKey().isNullOrBlank()) {
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
                    settingsManager.setKeyVerified(true)
                    keyEntry.isEnabled = false
                    apiCategory.removePreference(keyEntry)
                    apiCategory.removePreference(registerPref)
                } else {
                    // User is using personal (unverified) keys
                    //getSettingsManager().setKeyVerified(false);
                    // Clear API KEY entry to avoid issues
                    //getSettingsManager().setAPIKEY("");

                    keyEntry.isEnabled = true

                    if (apiCategory.findPreference(SettingsManager.KEY_APIKEY) == null)
                        apiCategory.addPreference(keyEntry)
                    if (apiCategory.findPreference(KEY_APIREGISTER) == null)
                        apiCategory.addPreference(registerPref)
                }
            } else {
                keyEntry.isEnabled = false
                personalKeyPref.isEnabled = false
                apiCategory.removePreference(personalKeyPref)
                apiCategory.removePreference(keyEntry)
                apiCategory.removePreference(registerPref)
                settingsManager.setKeyVerified(false)
                // Clear API KEY entry to avoid issues
                settingsManager.setAPIKEY("")
            }

            updateKeySummary()
            updateRegisterLink()

            syncPreference = findPreference(SettingsManager.KEY_DATASYNC) as ListPreference
            syncPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
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

            connStatusPref = findPreference(KEY_CONNSTATUS)
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

            RemoteIntent.startRemoteActivity(parentActivity, intentAndroid,
                    ConfirmationResultReceiver(parentActivity))

            true
        }

        private val registerPrefClickListener = Preference.OnPreferenceClickListener { preference ->
            val intentAndroid = Intent(preference.intent)
                    .addCategory(Intent.CATEGORY_BROWSABLE)

            RemoteIntent.startRemoteActivity(parentActivity, intentAndroid,
                    ConfirmationResultReceiver(parentActivity))

            true
        }

        private fun updateKeySummary(providerAPI: CharSequence = providerPref.entry) {
            if (!settingsManager.getAPIKEY().isNullOrBlank()) {
                val keyVerified = settingsManager.isKeyVerified()

                val colorSpan = ForegroundColorSpan(if (keyVerified) Color.GREEN else Color.RED)
                val summary: Spannable = SpannableString(if (keyVerified) getString(R.string.message_keyverified) else getString(R.string.message_keyinvalid))
                summary.setSpan(colorSpan, 0, summary.length, 0)
                keyEntry.summary = summary
            } else {
                keyEntry.summary = getString(R.string.pref_summary_apikey, providerAPI)
            }
        }

        private fun updateRegisterLink(providerAPI: CharSequence = providerPref.value) {
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
                        // Reset home location data
                        //getSettingsManager().SaveLastGPSLocData(new WeatherData.LocationData());
                    } else {
                        // permission denied, boo! Disable the
                        // functionality that depends on this permission.
                        followGps.isChecked = false
                        settingsManager.setFollowGPS(false)
                        showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                    }
                    return
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
        private lateinit var localBroadcastMgr: LocalBroadcastManager

        override fun onCreatePreferences(savedInstanceState: Bundle?) {
            addPreferencesFromResource(R.xml.pref_units)

            preferenceScreen.setTitle(R.string.pref_title_units)

            localBroadcastMgr = LocalBroadcastManager.getInstance(parentActivity!!)

            tempUnitPref = findPreference(SettingsManager.KEY_TEMPUNIT) as ListPreference
            speedUnitPref = findPreference(SettingsManager.KEY_SPEEDUNIT) as ListPreference
            distanceUnitPref = findPreference(SettingsManager.KEY_DISTANCEUNIT) as ListPreference
            precipationUnitPref = findPreference(SettingsManager.KEY_PRECIPITATIONUNIT) as ListPreference
            pressureUnitPref = findPreference(SettingsManager.KEY_PRESSUREUNIT) as ListPreference

            findPreference(KEY_RESETUNITS).onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AlertDialog.Builder(parentActivity)
                        .setTitle(R.string.pref_title_units)
                        .setItems(R.array.default_units) { dialog, which ->
                            val isFahrenheit = which == 0
                            tempUnitPref.value = if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS
                            speedUnitPref.value = if (isFahrenheit) Units.MILES_PER_HOUR else Units.KILOMETERS_PER_HOUR
                            distanceUnitPref.value = if (isFahrenheit) Units.MILES else Units.KILOMETERS
                            precipationUnitPref.value = if (isFahrenheit) Units.INCHES else Units.MILLIMETERS
                            pressureUnitPref.value = if (isFahrenheit) Units.INHG else Units.MILLIBAR
                            dialog.dismiss()

                            localBroadcastMgr.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT))
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, which ->
                            dialog.cancel()
                        }
                        .setCancelable(true)
                        .show()
                true
            }
        }
    }

    class IconsFragment : IconProviderPickerFragment() {
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
            WeatherComplicationHelper.requestComplicationUpdateAll(context)
            WeatherTileHelper.requestTileUpdateAll(context)
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

        override fun onCreatePreferences(savedInstanceState: Bundle?) {
            addPreferencesFromResource(R.xml.pref_aboutapp)

            findPreference(KEY_ABOUTCREDITS).onPreferenceClickListener = Preference.OnPreferenceClickListener { // Display the fragment as the main content.
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, CreditsFragment())
                        .addToBackStack(null)
                        .commit()

                true
            }

            findPreference(KEY_ABOUTOSLIBS).onPreferenceClickListener = Preference.OnPreferenceClickListener { // Display the fragment as the main content.
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, OSSCreditsFragment())
                        .addToBackStack(null)
                        .commit()

                true
            }

            try {
                val packageInfo = parentActivity!!.packageManager.getPackageInfo(parentActivity!!.packageName, 0)
                findPreference(KEY_ABOUTVERSION).summary = String.format("v%s", packageInfo.versionName)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    class CreditsFragment : SwipeDismissPreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?) {
            addPreferencesFromResource(R.xml.pref_credits)
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
            if (preference.intent != null) {
                RemoteIntent.startRemoteActivity(parentActivity, preference.intent
                        .setAction(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE),
                        null)

                // Show open on phone animation
                ConfirmationOverlay().setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                        .setMessage(parentActivity!!.getString(R.string.message_openedonphone))
                        .showAbove(view)

                return true
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }
    }

    class OSSCreditsFragment : SwipeDismissPreferenceFragment() {
        override fun onCreatePreferences(savedInstanceState: Bundle?) {
            addPreferencesFromResource(R.xml.pref_oslibs)
        }
    }
}