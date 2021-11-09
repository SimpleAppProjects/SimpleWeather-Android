package com.thewizrd.simpleweather.preferences

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FilterComparison
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.location.LocationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.preference.*
import androidx.preference.Preference.SummaryProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.helpers.*
import com.thewizrd.shared_resources.preferences.KeyEntryPreferenceDialogFragment
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.UserThemeMode.OnThemeChangeListener
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.extras.*
import com.thewizrd.simpleweather.locale.InstallRequest
import com.thewizrd.simpleweather.locale.LocaleInstaller
import com.thewizrd.simpleweather.notifications.WeatherNotificationWorker
import com.thewizrd.simpleweather.preferences.iconpreference.IconProviderPickerFragment
import com.thewizrd.simpleweather.preferences.radiopreference.CandidateInfo
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference
import com.thewizrd.simpleweather.preferences.timepickerpreference.TimePickerPreference
import com.thewizrd.simpleweather.radar.RadarProvider
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver
import com.thewizrd.simpleweather.services.UpdaterUtils
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetWorker
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.wearable.WearableWorkerActions
import java.util.*

class SettingsFragment : ToolbarPreferenceFragmentCompat(),
        OnSharedPreferenceChangeListener,
        OnThemeChangeListener {
    // Preferences
    private lateinit var followGps: SwitchPreferenceCompat
    private lateinit var intervalPref: ListPreference
    private lateinit var providerPref: ListPreference
    private lateinit var radarProviderPref: ListPreference
    private lateinit var personalKeyPref: SwitchPreferenceCompat
    private lateinit var keyEntry: EditTextPreference
    private lateinit var onGoingNotification: SwitchPreferenceCompat
    private lateinit var notificationIcon: ListPreference
    private lateinit var alertNotification: SwitchPreferenceCompat
    private lateinit var registerPref: Preference
    private lateinit var themePref: ListPreference
    private lateinit var languagePref: ListPreference

    private var premiumPref: Preference? = null
    private lateinit var dailyNotifPref: SwitchPreferenceCompat
    private lateinit var dailyNotifTimePref: TimePickerPreference
    private lateinit var popChanceNotifPref: SwitchPreferenceCompat

    // Background ops
    private lateinit var foregroundPref: SwitchPreferenceCompat
    private lateinit var batteryOptsPref: Preference
    private lateinit var notCategory: PreferenceCategory
    private lateinit var apiCategory: PreferenceCategory
    private lateinit var aboutCategory: PreferenceCategory

    // Intent queue
    private lateinit var intentQueue: HashSet<FilterComparison>
    private var mThemeChangeListeners: MutableList<OnThemeChangeListener>? = null
    private var splitInstallRequest: InstallRequest? = null

    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 0
        private const val PERMISSION_BGLOCATION_REQUEST_CODE = 1

        // Preference Keys
        private const val KEY_UNITS = "key_units"
        private const val KEY_ICONS = "key_icons"
        private const val KEY_FEATURES = "key_features"
        private const val KEY_PREMIUM = "key_premium"
        private const val KEY_ABOUTAPP = "key_aboutapp"
        private const val KEY_APIREGISTER = "key_apiregister"
        private const val CATEGORY_NOTIFICATION = "category_notification"
        private const val CATEGORY_API = "category_api"

        private fun tintIcons(preference: Preference, @ColorInt color: Int) {
            if (preference is PreferenceGroup) {
                for (i in 0 until preference.preferenceCount) {
                    tintIcons(preference.getPreference(i), color)
                }
            } else {
                val icon: Drawable? = preference.icon
                icon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    override fun getTitle(): Int {
        return R.string.title_activity_settings
    }

    /**
     * Registers a listener.
     */
    private fun registerOnThemeChangeListener(listener: OnThemeChangeListener) {
        synchronized(this) {
            if (mThemeChangeListeners == null) {
                mThemeChangeListeners = ArrayList()
            }
            if (!mThemeChangeListeners!!.contains(listener)) {
                mThemeChangeListeners!!.add(listener)
            }
        }
    }

    /**
     * Unregisters a listener.
     */
    private fun unregisterOnThemeChangeListener(listener: OnThemeChangeListener) {
        synchronized(this) {
            mThemeChangeListeners?.remove(listener)
        }
    }

    private fun dispatchThemeChanged(mode: UserThemeMode) {
        var list: List<OnThemeChangeListener>

        synchronized(this) {
            if (mThemeChangeListeners == null) return
            list = ArrayList(mThemeChangeListeners!!)
        }

        val N = list.size
        for (i in 0 until N) {
            list[i].onThemeChanged(mode)
        }
    }

    override fun onThemeChanged(mode: UserThemeMode) {
        updateWindowColors(mode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SettingsFragment: onCreate")
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SettingsFragment: onResume")

        // Register listener
        val app = App.instance
        app.unregisterAppSharedPreferenceListener()
        app.registerAppSharedPreferenceListener(this)
        registerOnThemeChangeListener(this)
        registerOnThemeChangeListener(appCompatActivity as OnThemeChangeListener)

        // Initialize queue
        intentQueue = HashSet()

        batteryOptsPref.isVisible = !(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || PowerUtils.isBackgroundOptimizationDisabled(requireContext()))
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SettingsFragment: onPause")
        if (settingsManager.usePersonalKey() && settingsManager.getAPIKEY().isNullOrBlank() && WeatherManager.isKeyRequired(providerPref.value)) {
            // Fallback to supported weather provider
            val API = RemoteConfig.getDefaultWeatherProvider()
            providerPref.value = API
            providerPref.callChangeListener(API)
            settingsManager.setAPI(API)
            WeatherManager.instance.updateAPI()

            settingsManager.setPersonalKey(false)
            settingsManager.setKeyVerified(true)
        }

        // Unregister listener
        val app = App.instance
        app.unregisterAppSharedPreferenceListener(this)
        app.registerAppSharedPreferenceListener()
        unregisterOnThemeChangeListener(appCompatActivity as OnThemeChangeListener)
        unregisterOnThemeChangeListener(this)

        for (filter: FilterComparison in intentQueue) {
            if ((CommonActions.ACTION_SETTINGS_UPDATEAPI == filter.intent.action)) {
                WeatherManager.instance.updateAPI()
                // Log event
                val bundle = Bundle()
                bundle.putString("API", settingsManager.getAPI())
                bundle.putString(
                    "API_IsInternalKey",
                    (!settingsManager.usePersonalKey()).toString()
                )
                AnalyticsLogger.logEvent("Update_API", bundle)

                WeatherUpdaterWorker.enqueueAction(
                    appCompatActivity,
                    WeatherUpdaterWorker.ACTION_UPDATEWEATHER
                )
            } else if ((WidgetWorker::class.java.name == filter.intent.component!!.className)) {
                when (filter.intent.action) {
                    WidgetWorker.ACTION_REFRESHGPSWIDGETS -> WidgetWorker.enqueueRefreshGPSWidgets(
                        appCompatActivity
                    )
                    WidgetWorker.ACTION_RESETGPSWIDGETS -> WidgetWorker.enqueueResetGPSWidgets(
                        appCompatActivity
                    )
                }
            } else if ((WeatherUpdaterWorker::class.java.name == filter.intent.component!!.className)) {
                when (filter.intent.action) {
                    WeatherUpdaterWorker.ACTION_REQUEUEWORK -> {
                        UpdaterUtils.updateAlarm(appCompatActivity)
                    }
                    WeatherUpdaterWorker.ACTION_ENQUEUEWORK -> {
                        UpdaterUtils.startAlarm(appCompatActivity)
                    }
                    WeatherUpdaterWorker.ACTION_CANCELWORK -> {
                        UpdaterUtils.cancelAlarm(appCompatActivity)
                    }
                    else -> {
                        WeatherUpdaterWorker.enqueueAction(
                            appCompatActivity,
                            (filter.intent.action)!!
                        )
                    }
                }
            } else if ((WearableWorker::class.java.name == filter.intent.component!!.className)) {
                WearableWorker.enqueueAction(appCompatActivity, (filter.intent.action)!!)
            } else if ((CommonActionsBroadcastReceiver::class.java.name == filter.intent.component!!.className)) {
                LocalBroadcastManager.getInstance(appCompatActivity).sendBroadcast(filter.intent)
            } else {
                appCompatActivity.startService(filter.intent)
            }
        }

        super.onPause()
    }

    override fun onBackPressed(): Boolean {
        if ((settingsManager.usePersonalKey() &&
             settingsManager.getAPIKEY().isNullOrBlank() &&
             WeatherManager.isKeyRequired(providerPref.value))) {
            // Set keyentrypref color to red
            showSnackbar(Snackbar.make(R.string.message_enter_apikey, Snackbar.Duration.LONG), null)
            return true
        }

        return false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, null)

        notCategory = findPreference(CATEGORY_NOTIFICATION)!!
        apiCategory = findPreference(CATEGORY_API)!!

        findPreference<Preference>(KEY_ABOUTAPP)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Display the fragment as the main content.
            appCompatActivity.findNavController(R.id.fragment_container)
                .safeNavigate(SettingsFragmentDirections.actionSettingsFragmentToAboutAppFragment())
            true
        }

        findPreference<Preference>(KEY_UNITS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { // Display the fragment as the main content.
            appCompatActivity.findNavController(R.id.fragment_container)
                .safeNavigate(SettingsFragmentDirections.actionSettingsFragmentToUnitsFragment())
            true
        }

        followGps = findPreference(SettingsManager.KEY_FOLLOWGPS)!!
        followGps.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
                AnalyticsLogger.logEvent("Settings: followGps toggled")
                if (newValue as Boolean) {
                    if (!appCompatActivity.locationPermissionEnabled()) {
                        this@SettingsFragment.requestLocationPermission(
                            PERMISSION_LOCATION_REQUEST_CODE
                        )
                        return false
                    } else {
                        val locMan =
                            appCompatActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                        if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                            showSnackbar(
                                Snackbar.make(
                                    R.string.error_enable_location_services,
                                    Snackbar.Duration.SHORT
                                ), null
                            )

                            settingsManager.setFollowGPS(false)
                            return false
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() &&
                                !appCompatActivity.backgroundLocationPermissionEnabled()
                            ) {
                                val snackbar = Snackbar.make(
                                    appCompatActivity.getBackgroundLocationRationale(),
                                    Snackbar.Duration.VERY_LONG
                                )
                                snackbar.setAction(android.R.string.ok) {
                                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                                        appCompatActivity.openAppSettingsActivity()
                                    } else {
                                        requestBackgroundLocationPermission(
                                            PERMISSION_BGLOCATION_REQUEST_CODE
                                        )
                                    }
                                }
                                showSnackbar(snackbar, null)
                                settingsManager.setRequestBGAccess(true)
                            }
                        }
                    }
                }
                return true
            }
        }
        intervalPref = findPreference(SettingsManager.KEY_REFRESHINTERVAL)!!
        if (enableAdditionalRefreshIntervals()) {
            intervalPref.setEntries(R.array.premium_refreshinterval_entries)
            intervalPref.setEntryValues(R.array.premium_refreshinterval_values)
        } else {
            intervalPref.setEntries(R.array.refreshinterval_entries)
            intervalPref.setEntryValues(R.array.refreshinterval_values)
        }

        themePref = findPreference(SettingsManager.KEY_USERTHEME)!!
        themePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val args = Bundle()
            args.putString("mode", newValue.toString())
            AnalyticsLogger.logEvent("Settings: theme changed", args)

            val mode: UserThemeMode
            when (newValue.toString()) {
                "0" -> {
                    mode = UserThemeMode.FOLLOW_SYSTEM
                    if (Build.VERSION.SDK_INT >= 29)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    else
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
                "1" -> {
                    mode = UserThemeMode.DARK
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                "2" -> {
                    mode = UserThemeMode.AMOLED_DARK
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                "3" -> {
                    mode = UserThemeMode.LIGHT
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                else -> {
                    mode = UserThemeMode.FOLLOW_SYSTEM
                    if (Build.VERSION.SDK_INT >= 29)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    else
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
            dispatchThemeChanged(mode)
            true
        }

        keyEntry = findPreference(SettingsManager.KEY_APIKEY)!!
        personalKeyPref = findPreference(SettingsManager.KEY_USEPERSONALKEY)!!
        personalKeyPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue as Boolean) {
                if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null)
                    apiCategory.addPreference(keyEntry)
                if (apiCategory.findPreference<Preference?>(KEY_APIREGISTER) == null)
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
        providerPref = findPreference(SettingsManager.KEY_API)!!

        var entries = arrayOfNulls<String>(providers.size)
        var entryValues = arrayOfNulls<String>(providers.size)

        providers.forEachIndexed { i, it ->
            entries[i] = it.display
            entryValues[i] = it.value
        }

        providerPref.entries = entries
        providerPref.entryValues = entryValues
        providerPref.isPersistent = false
        providerPref.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
            override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
                if (!isWeatherAPISupported(newValue.toString())) {
                    runWithView {
                        navigateToPremiumFragment()
                    }
                    return false
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
                        if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_APIKEY) == null) apiCategory.addPreference(keyEntry)
                        if (apiCategory.findPreference<Preference?>(KEY_APIREGISTER) == null) apiCategory.addPreference(registerPref)
                    }

                    if (apiCategory.findPreference<Preference?>(SettingsManager.KEY_USEPERSONALKEY) == null)
                        apiCategory.addPreference(personalKeyPref)

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

                updateAlertPreference(WeatherManager.instance.supportsAlerts())

                return true
            }
        }

        registerPref = findPreference(KEY_APIREGISTER)!!

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
            settingsManager.setKeyVerified(false)
            // Clear API KEY entry to avoid issues
            settingsManager.setAPIKEY("")
        }

        updateKeySummary()
        updateRegisterLink()

        radarProviderPref = findPreference(RadarProvider.KEY_RADARPROVIDER)!!
        val radarProviders = RadarProvider.getRadarProviders()
        entries = arrayOfNulls(radarProviders.size)
        entryValues = arrayOfNulls(radarProviders.size)

        for (i in radarProviders.indices) {
            entries[i] = radarProviders[i].display
            entryValues[i] = radarProviders[i].value
        }

        radarProviderPref.entries = entries
        radarProviderPref.entryValues = entryValues
        radarProviderPref.value = RadarProvider.getRadarProvider()

        onGoingNotification = findPreference(SettingsManager.KEY_ONGOINGNOTIFICATION)!!
        onGoingNotification.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val context = preference.context.applicationContext

            // On-going notification
            if (newValue as Boolean) {
                WeatherNotificationWorker.requestRefreshNotification(context)

                if (notCategory.findPreference<Preference?>(SettingsManager.KEY_NOTIFICATIONICON) == null)
                    notCategory.addPreference(notificationIcon)

                checkBackgroundLocationAccess()

                enqueueIntent(
                    Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
                )
            } else {
                WeatherNotificationWorker.removeNotification(context)

                notCategory.removePreference(notificationIcon)

                enqueueIntent(Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_CANCELWORK))
            }

            true
        }

        notificationIcon = findPreference(SettingsManager.KEY_NOTIFICATIONICON)!!
        notificationIcon.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            WeatherNotificationWorker.requestRefreshNotification(preference.context.applicationContext)
            true
        }

        // Remove preferences
        if (!onGoingNotification.isChecked) {
            notCategory.removePreference(notificationIcon)
        }

        alertNotification = findPreference(SettingsManager.KEY_USEALERTS)!!
        alertNotification.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val context = preference.context.applicationContext

            // Alert notification
            if (newValue as Boolean) {
                enqueueIntent(Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_ENQUEUEWORK))
            } else {
                enqueueIntent(Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_CANCELWORK))
            }
            true
        }
        updateAlertPreference(WeatherManager.instance.supportsAlerts())

        findPreference<Preference>(KEY_ICONS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Display the fragment as the main content.
            appCompatActivity.findNavController(R.id.fragment_container)
                .safeNavigate(SettingsFragmentDirections.actionSettingsFragmentToIconsFragment())
            true
        }

        findPreference<Preference>(KEY_FEATURES)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Display the fragment as the main content.
            appCompatActivity.findNavController(R.id.fragment_container)
                .safeNavigate(SettingsFragmentDirections.actionSettingsFragmentToFeaturesFragment2())
            true
        }

        languagePref = findPreference(LocaleUtils.KEY_LANGUAGE)!!
        val langCodes = languagePref.entryValues
        val langEntries = arrayOfNulls<CharSequence>(langCodes.size)
        for (i in langCodes.indices) {
            val code = langCodes[i]

            if (TextUtils.isEmpty(code)) {
                langEntries[i] = requireContext().getString(R.string.summary_default)
            } else {
                val localeCode = code.toString()
                val locale = Locale(localeCode)
                langEntries[i] = locale.getDisplayName(locale)
            }
        }
        languagePref.entries = langEntries

        languagePref.summaryProvider = SummaryProvider<ListPreference> { preference ->
            if (preference.value.isNullOrBlank()) {
                preference.context.getString(R.string.summary_default)
            } else {
                LocaleUtils.getLocaleDisplayName()
            }
        }

        languagePref.setDefaultValue("")
        languagePref.value = LocaleUtils.getLocaleCode()
        languagePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val requestedLang = newValue.toString()
            splitInstallRequest = LocaleInstaller.installLocale(requireActivity(), requestedLang)
            false
        }

        if (isPremiumSupported()) {
            premiumPref = createPremiumPreference()
        }

        foregroundPref = findPreference(PowerUtils.KEY_USE_FOREGROUNDSERVICE)!!
        batteryOptsPref = findPreference(PowerUtils.KEY_REQUESTIGNOREBATOPTS)!!

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            !PowerUtils.checkBackgroundOptimizationPermission(requireContext()) ||
            !PowerUtils.canStartIgnoreBatteryOptActivity(requireContext()) ||
            PowerUtils.isBackgroundOptimizationDisabled(requireContext())
        ) {
            batteryOptsPref.isVisible = false
        } else {
            batteryOptsPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    PowerUtils.startIgnoreBatteryOptActivity(requireContext())
                    true
                }
        }

        foregroundPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                UpdaterUtils.enableForegroundService(requireContext(), newValue as Boolean)
                if (newValue) {
                    checkBackgroundLocationAccess()
                }
                true
            }

        dailyNotifPref = findPreference(SettingsManager.KEY_DAILYNOTIFICATION)!!
        dailyNotifPref.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue == true && !areNotificationExtrasEnabled()) {
                    runWithView {
                        navigateToPremiumFragment()
                    }
                    return@OnPreferenceChangeListener false
                }
                UpdaterUtils.enableDailyNotificationService(preference.context, newValue as Boolean)
            if (newValue) {
                checkBackgroundLocationAccess()
            }
            true
        }
        dailyNotifTimePref = findPreference(SettingsManager.KEY_DAILYNOTIFICATIONTIME)!!
        dailyNotifTimePref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            settingsManager.setDailyNotificationTime(newValue.toString())
            UpdaterUtils.rescheduleDailyNotificationService(preference.context)
            true
        }
        popChanceNotifPref = findPreference(SettingsManager.KEY_POPCHANCENOTIFICATION)!!
        popChanceNotifPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (newValue == true && !areNotificationExtrasEnabled()) {
                runWithView {
                    navigateToPremiumFragment()
                }
                return@OnPreferenceChangeListener false
            }

            val context = preference.context.applicationContext

            // Alert notification
            if (newValue as Boolean) {
                enqueueIntent(
                    Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
                )

                checkBackgroundLocationAccess()
            } else {
                enqueueIntent(Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_CANCELWORK))
            }
            true
        }

        val aboutPref = findPreference<Preference>(KEY_ABOUTAPP)!!
        aboutCategory = aboutPref.parent as PreferenceCategory

        // Remove if subs are not supported
        if (!isPremiumSupported()) {
            premiumPref?.let {
                aboutCategory.removePreference(it)
            }
            dailyNotifPref.isVisible = false
            dailyNotifTimePref.isVisible = false
            popChanceNotifPref.isVisible = false
        } else if (premiumPref != null && aboutCategory.findPreference<Preference>(KEY_PREMIUM) == null) {
            aboutCategory.addPreference(premiumPref)
            dailyNotifPref.isVisible = true
            dailyNotifTimePref.isVisible = true
            popChanceNotifPref.isVisible = true
        }

        tintIcons(preferenceScreen, appCompatActivity.getAttrColor(R.attr.colorPrimary))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LocaleInstaller.CONFIRMATION_REQUEST_CODE) {
            // Handle the user's decision. For example, if the user selects "Cancel",
            // you may want to disable certain functionality that depends on the module.
            if (resultCode == Activity.RESULT_CANCELED) {
                splitInstallRequest?.cancelRequest()
                splitInstallRequest = null
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("Range")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is EditTextPreference && (SettingsManager.KEY_APIKEY == preference.getKey())) {
            val TAG = KeyEntryPreferenceDialogFragment::class.java.name

            if (parentFragmentManager.findFragmentByTag(TAG) != null) {
                return
            }

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
                            updateAlertPreference(WeatherManager.instance.supportsAlerts())

                            fragment.dialog?.dismiss()
                        } else {
                            Toast.makeText(appCompatActivity, R.string.message_keyinvalid, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: WeatherException) {
                        Logger.writeLine(Log.ERROR, e)
                    }
                }
            }

            fragment.setTargetFragment(this, 0)
            fragment.show(parentFragmentManager, KeyEntryPreferenceDialogFragment::class.java.name)
        } else if (preference is TimePickerPreference) {
            val TAG = MaterialTimePicker::class.java.name

            if (parentFragmentManager.findFragmentByTag(TAG) != null) {
                return
            }

            val is24hour = DateFormat.is24HourFormat(preference.getContext())

            val f = MaterialTimePicker.Builder()
                    .setTimeFormat(if (is24hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                    .setHour(preference.hourOfDay)
                    .setMinute(preference.minute)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                    .build()
            f.addOnPositiveButtonClickListener {
                if (preference.callChangeListener(String.format(Locale.ROOT, "%02d:%02d", f.hour, f.minute))) {
                    preference.setTime(f.hour, f.minute)
                }
            }

            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun updateKeySummary(providerAPI: CharSequence = providerPref.entry) {
        if (!settingsManager.getAPIKEY().isNullOrBlank()) {
            val keyVerified = settingsManager.isKeyVerified()
            val colorSpan = ForegroundColorSpan(if (keyVerified) Color.GREEN else Color.RED)
            val summary = SpannableString(if (keyVerified) getString(R.string.message_keyverified) else getString(R.string.message_keyinvalid))
            summary.setSpan(colorSpan, 0, summary.length, 0)
            keyEntry.summary = summary
        } else {
            keyEntry.summary = getString(R.string.pref_summary_apikey, providerAPI)
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

    private fun updateAlertPreference(enable: Boolean) {
        alertNotification.isEnabled = enable
        alertNotification.summary = if (enable) getString(R.string.pref_summary_alerts) else getString(R.string.pref_summary_alerts_disabled)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray
    ) {
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
                    showSnackbar(
                        Snackbar.make(
                            R.string.error_location_denied,
                            Snackbar.Duration.SHORT
                        ), null
                    )
                }
                return
            }
            PERMISSION_BGLOCATION_REQUEST_CODE -> {
                // no-op
            }
        }
    }

    private fun enqueueIntent(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        } else {
            if (WeatherUpdaterWorker.ACTION_REQUEUEWORK == intent.action || (WeatherUpdaterWorker.ACTION_ENQUEUEWORK == intent.action)) {
                for (filter: FilterComparison in intentQueue) {
                    if (WeatherUpdaterWorker.ACTION_CANCELWORK == filter.intent.action) {
                        intentQueue.remove(filter)
                        break
                    }
                }
            } else if (WeatherUpdaterWorker.ACTION_CANCELWORK == intent.action) {
                for (filter: FilterComparison in intentQueue) {
                    if (WeatherUpdaterWorker.ACTION_REQUEUEWORK == filter.intent.action || WeatherUpdaterWorker.ACTION_ENQUEUEWORK == intent.action) {
                        intentQueue.remove(filter)
                        break
                    }
                }
            }

            return intentQueue.add(FilterComparison(intent))
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key.isNullOrBlank()) return

        val context: Context = appCompatActivity

        when (key) {
            SettingsManager.KEY_API -> {
                enqueueIntent(Intent(CommonActions.ACTION_SETTINGS_UPDATEAPI))
                enqueueIntent(Intent(context, WearableWorker::class.java)
                        .setAction(WearableWorkerActions.ACTION_SENDSETTINGSUPDATE))
            }
            SettingsManager.KEY_FOLLOWGPS -> {
                val value = sharedPreferences.getBoolean(key, false)
                enqueueIntent(
                    Intent(context, WearableWorker::class.java)
                        .setAction(WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
                )
                enqueueIntent(
                    Intent(context, WearableWorker::class.java)
                        .setAction(WearableWorkerActions.ACTION_SENDLOCATIONUPDATE)
                )
                enqueueIntent(
                    Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
                )
                enqueueIntent(
                    Intent(context, WearableWorker::class.java)
                        .setAction(WearableWorkerActions.ACTION_SENDWEATHERUPDATE)
                )
                enqueueIntent(
                    Intent(context, WidgetWorker::class.java)
                        .setAction(if (value) WidgetWorker.ACTION_REFRESHGPSWIDGETS else WidgetWorker.ACTION_RESETGPSWIDGETS)
                )
            }
            SettingsManager.KEY_REFRESHINTERVAL -> {
                enqueueIntent(
                    Intent(context, WeatherUpdaterWorker::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_REQUEUEWORK)
                )
            }
            LocaleUtils.KEY_LANGUAGE -> {
                enqueueIntent(
                    Intent(context, WearableWorker::class.java)
                        .setAction(WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
                )
            }
        }
    }

    private fun checkBackgroundLocationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() &&
            !appCompatActivity.backgroundLocationPermissionEnabled()
        ) {
            val snackbar = Snackbar.make(
                appCompatActivity.getBackgroundLocationRationale(),
                Snackbar.Duration.VERY_LONG
            )
            snackbar.setAction(android.R.string.ok) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    appCompatActivity.openAppSettingsActivity()
                } else {
                    requestBackgroundLocationPermission(
                        PERMISSION_BGLOCATION_REQUEST_CODE
                    )
                }
            }
            showSnackbar(snackbar, null)
            settingsManager.setRequestBGAccess(true)
        }
    }

    class UnitsFragment : ToolbarPreferenceFragmentCompat() {
        companion object {
            private val KEY_RESETUNITS = "key_resetunits"
        }

        private lateinit var tempUnitPref: ListPreference
        private lateinit var speedUnitPref: ListPreference
        private lateinit var distanceUnitPref: ListPreference
        private lateinit var precipationUnitPref: ListPreference
        private lateinit var pressureUnitPref: ListPreference

        private var unitsChanged = false
        private lateinit var localBroadcastMgr: LocalBroadcastManager

        override fun getTitle(): Int {
            return R.string.pref_title_units
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_units, null)

            tempUnitPref = findPreference(SettingsManager.KEY_TEMPUNIT)!!
            speedUnitPref = findPreference(SettingsManager.KEY_SPEEDUNIT)!!
            distanceUnitPref = findPreference(SettingsManager.KEY_DISTANCEUNIT)!!
            precipationUnitPref = findPreference(SettingsManager.KEY_PRECIPITATIONUNIT)!!
            pressureUnitPref = findPreference(SettingsManager.KEY_PRESSUREUNIT)!!

            tempUnitPref.onPreferenceChangeListener = onUnitChangeListener
            speedUnitPref.onPreferenceChangeListener = onUnitChangeListener
            distanceUnitPref.onPreferenceChangeListener = onUnitChangeListener
            precipationUnitPref.onPreferenceChangeListener = onUnitChangeListener
            pressureUnitPref.onPreferenceChangeListener = onUnitChangeListener

            findPreference<Preference>(KEY_RESETUNITS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                MaterialAlertDialogBuilder(appCompatActivity)
                        .setTitle(R.string.pref_title_units)
                        .setItems(R.array.default_units) { dialog, which ->
                            val isFahrenheit: Boolean = which == 0
                            tempUnitPref.value = if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS
                            speedUnitPref.value = if (isFahrenheit) Units.MILES_PER_HOUR else Units.KILOMETERS_PER_HOUR
                            distanceUnitPref.value = if (isFahrenheit) Units.MILES else Units.KILOMETERS
                            precipationUnitPref.value = if (isFahrenheit) Units.INCHES else Units.MILLIMETERS
                            pressureUnitPref.value = if (isFahrenheit) Units.INHG else Units.MILLIBAR
                            dialog.dismiss()

                            localBroadcastMgr.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT))
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }
                        .setCancelable(true)
                        .show()
                true
            }
        }

        private val onUnitChangeListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
            unitsChanged = true
            true
        }

        override fun onResume() {
            super.onResume()
            localBroadcastMgr = LocalBroadcastManager.getInstance(appCompatActivity)
        }

        override fun onPause() {
            if (unitsChanged) {
                localBroadcastMgr.sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_UPDATEUNIT))
                unitsChanged = false
            }
            super.onPause()
        }
    }

    class IconsFragment : IconProviderPickerFragment() {
        override fun getTitle(): Int {
            return R.string.pref_title_icons
        }

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

        override fun bindPreferenceExtra(pref: RadioButtonPreference, key: String,
                                         info: CandidateInfo, defaultKey: String?,
                                         systemDefaultKey: String?
        ) {
            super.bindPreferenceExtra(pref, key, info, defaultKey, systemDefaultKey)
            pref.isPersistent = false
        }

        override fun onSelectionPerformed(success: Boolean) {
            super.onSelectionPerformed(success)
            val context = prefContext.applicationContext
            WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
        }

        override fun onRadioButtonConfirmed(selectedKey: String?) {
            if (!isIconPackSupported(selectedKey)) {
                navigateUnsupportedIconPack()
                return
            }
            super.onRadioButtonConfirmed(selectedKey)
        }
    }

    class FeaturesFragment : ToolbarPreferenceFragmentCompat() {
        override fun getTitle(): Int {
            return R.string.pref_title_features
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_features, null)

            findPreference<Preference>(FeatureSettings.KEY_FEATURE_BGIMAGE)?.isVisible = !BuildConfig.IS_NONGMS
        }
    }

    class AboutAppFragment : ToolbarPreferenceFragmentCompat() {
        companion object {
            // Preference Keys
            private const val KEY_ABOUTCREDITS = "key_aboutcredits"
            private const val KEY_ABOUTOSLIBS = "key_aboutoslibs"
            private const val KEY_FEEDBACK = "key_feedback"
            private const val KEY_RATEREVIEW = "key_ratereview"
            private const val KEY_TRANSLATE = "key_translate"
            private const val KEY_ABOUTVERSION = "key_aboutversion"
        }

        private lateinit var devSettingsController: DevSettingsController

        override fun getTitle(): Int {
            return R.string.pref_title_about
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            devSettingsController = DevSettingsController(this, KEY_ABOUTVERSION)
            super.onCreate(savedInstanceState)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_aboutapp, null)

            findPreference<Preference>(KEY_ABOUTCREDITS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // Display the fragment as the main content.
                appCompatActivity.findNavController(R.id.fragment_container)
                    .safeNavigate(`SettingsFragment$AboutAppFragmentDirections`.actionAboutAppFragmentToCreditsFragment())
                true
            }

            findPreference<Preference>(KEY_ABOUTOSLIBS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // Display the fragment as the main content.
                appCompatActivity.findNavController(R.id.fragment_container)
                    .safeNavigate(`SettingsFragment$AboutAppFragmentDirections`.actionAboutAppFragmentToOSSCreditsFragment())
                true
            }

            findPreference<Preference>(KEY_FEEDBACK)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val sendTo = Intent(Intent.ACTION_SENDTO)
                sendTo.data = Uri.parse("mailto:thewizrd.dev+SimpleWeatherAndroid@gmail.com")
                startActivity(Intent.createChooser(sendTo, null))
                true
            }

            setupReviewPreference(findPreference(KEY_RATEREVIEW)!!)

            findPreference<Preference>(KEY_TRANSLATE)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                if (preference.intent != null) {
                    try {
                        startActivity(preference.intent)
                    } catch (e: ActivityNotFoundException) {
                        val i: Intent = preference.intent

                        if (i.resolveActivity(appCompatActivity.packageManager) != null) {
                            startActivity(i)
                        }
                    }
                }
                true
            }

            try {
                val packageInfo = appCompatActivity.packageManager.getPackageInfo(appCompatActivity.packageName, 0)
                findPreference<Preference>(KEY_ABOUTVERSION)!!.summary = String.format("v%s", packageInfo.versionName)
            } catch (e: PackageManager.NameNotFoundException) {
                // should never happen
            }

            devSettingsController.onCreatePreferences(preferenceScreen)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            if (devSettingsController.onPreferenceTreeClick(preference)) {
                return true
            }

            return super.onPreferenceTreeClick(preference)
        }
    }

    class CreditsFragment : ToolbarPreferenceFragmentCompat() {
        private val CATEGORY_ICONS = "key_caticons"

        override fun getTitle(): Int {
            return R.string.pref_title_credits
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_credits, null)

            val iconsCategory = findPreference<PreferenceCategory>(CATEGORY_ICONS)
            iconsCategory!!.removeAll()

            val providers = SimpleLibrary.instance.iconProviders
            providers.forEach { (s, wiProvider) ->
                val pref = Preference(requireContext())
                pref.title = wiProvider.displayName
                pref.summary = wiProvider.authorName
                if (wiProvider.attributionLink != null) {
                    pref.intent = Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(Uri.parse(wiProvider.attributionLink))
                }

                iconsCategory.addPreference(pref)
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.intent != null) {
                try {
                    startActivity(preference.intent)
                } catch (e: ActivityNotFoundException) {
                    val i = preference.intent

                    if (i.resolveActivity(appCompatActivity.packageManager) != null) {
                        startActivity(i)
                    }
                }

                return true
            }

            return false
        }
    }

    class OSSCreditsFragment : ToolbarPreferenceFragmentCompat() {
        override fun getTitle(): Int {
            return R.string.pref_title_oslibs
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_oslibs, null)
        }
    }
}