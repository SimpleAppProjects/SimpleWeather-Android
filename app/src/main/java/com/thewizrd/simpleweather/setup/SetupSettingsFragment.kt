package com.thewizrd.simpleweather.setup

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.util.LocalePreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.PermissionLauncher
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.common.helpers.notificationPermissionEnabled
import com.thewizrd.common.helpers.openAppSettingsActivity
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.databinding.FragmentSetupSettingsBinding
import com.thewizrd.simpleweather.extras.enableAdditionalRefreshIntervals
import com.thewizrd.simpleweather.notifications.NotificationUtils.Companion.openAppNotificationSettingsActivity
import com.thewizrd.simpleweather.preferences.CustomPreferenceFragmentCompat
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager

class SetupSettingsFragment : CustomPreferenceFragmentCompat() {
    private lateinit var binding: FragmentSetupSettingsBinding
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher
    private lateinit var onGoingNotifPermissionLauncher: PermissionLauncher
    private lateinit var alertNotifPermissionLauncher: PermissionLauncher

    // Preferences
    private lateinit var unitPref: SwitchPreferenceCompat
    private lateinit var intervalPref: ListPreference
    private lateinit var notIconPref: ListPreference
    private lateinit var onGoingPref: SwitchPreferenceCompat
    private lateinit var alertsPref: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        locationPermissionLauncher = LocationPermissionLauncher(this)
        onGoingNotifPermissionLauncher = PermissionLauncher(this) { results ->
            val isChecked = results.isNotEmpty() && results.all { it.value }
            if (isChecked) {
                if (onGoingPref.callChangeListener(true)) {
                    onGoingPref.isChecked = true
                }
            } else {
                context?.let {
                    showSnackbar(
                        Snackbar.make(
                            it,
                            sharedRes.string.notification_perm_denied,
                            Snackbar.Duration.SHORT
                        ).apply {
                            setAction(sharedRes.string.action_settings) {
                                runCatching {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        it.context.openAppNotificationSettingsActivity()
                                    } else {
                                        it.context.openAppSettingsActivity()
                                    }
                                }
                            }
                        })
                }
            }
        }
        alertNotifPermissionLauncher = PermissionLauncher(this) { results ->
            val isChecked = results.isNotEmpty() && results.all { it.value }
            if (isChecked) {
                alertsPref.isChecked = true
            } else {
                context?.let {
                    showSnackbar(
                        Snackbar.make(
                            it,
                            sharedRes.string.notification_perm_denied,
                            Snackbar.Duration.SHORT
                        ).apply {
                            setAction(sharedRes.string.action_settings) {
                                runCatching {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        it.context.openAppNotificationSettingsActivity()
                                    } else {
                                        it.context.openAppSettingsActivity()
                                    }
                                }
                            }
                        })
                }
            }
        }

        // Set default units based on user locale
        PreferenceManager.getDefaultSharedPreferences(requireContext()).run {
            if (!contains(SettingsManager.KEY_USECELSIUS) && !contains(SettingsManager.KEY_TEMPUNIT)) {
                if (LocalePreferences.getTemperatureUnit() == LocalePreferences.TemperatureUnit.CELSIUS) {
                    settingsManager.setDefaultUnits(Units.CELSIUS)
                } else {
                    settingsManager.setDefaultUnits(Units.FAHRENHEIT)
                }
            }
        }
    }

    override fun createSnackManager(activity: Activity): SnackbarManager {
        val mStepperNavBar = activity.findViewById<View>(R.id.bottom_nav_bar)

        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
            setAnchorView(mStepperNavBar)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupSettingsBinding.inflate(inflater, container, false)
        val root = binding.root as ViewGroup
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        binding.fragmentContainer.addView(inflatedView)
        binding.fragmentContainer.setBackgroundColor(Colors.TRANSPARENT)

        return root
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        val adapter = super.onCreateAdapter(preferenceScreen)

        if (adapter is ConcatAdapter) {
            adapter.addAdapter(0, SpacerAdapter(preferenceScreen.context.dpToPx(16f).toInt()))
        }

        return adapter
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_setup, rootKey)

        unitPref = findPreference(SettingsManager.KEY_USECELSIUS)!!
        intervalPref = findPreference(SettingsManager.KEY_REFRESHINTERVAL)!!
        notIconPref = findPreference(SettingsManager.KEY_NOTIFICATIONICON)!!
        onGoingPref = findPreference(SettingsManager.KEY_ONGOINGNOTIFICATION)!!
        alertsPref = findPreference(SettingsManager.KEY_USEALERTS)!!

        unitPref.setOnPreferenceChangeListener { preference, newValue ->
            val value = newValue as Boolean

            if (value) {
                // Use Celsius
                settingsManager.setDefaultUnits(Units.CELSIUS)
            } else {
                settingsManager.setDefaultUnits(Units.FAHRENHEIT)
            }

            true
        }

        if (enableAdditionalRefreshIntervals()) {
            intervalPref.setEntries(sharedRes.array.premium_refreshinterval_entries)
            intervalPref.setEntryValues(sharedRes.array.premium_refreshinterval_values)
        } else {
            intervalPref.setEntries(sharedRes.array.refreshinterval_entries)
            intervalPref.setEntryValues(sharedRes.array.refreshinterval_values)
        }

        onGoingPref.setOnPreferenceChangeListener { preference, newValue ->
            val value = newValue as Boolean

            if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!preference.context.notificationPermissionEnabled()) {
                    onGoingNotifPermissionLauncher.requestPermission(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnPreferenceChangeListener false
                }
            }

            notIconPref.isVisible = value

            if (value && settingsManager.useFollowGPS() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() && requireActivity().backgroundLocationPermissionEnabled()) {
                context?.let {
                    val snackbar = Snackbar.make(
                        it,
                        it.getBackgroundLocationRationale(),
                        Snackbar.Duration.VERY_LONG
                    ).apply {
                        setAction(android.R.string.ok) {
                            locationPermissionLauncher.requestBackgroundLocationPermission()
                        }
                    }
                    showSnackbar(snackbar)
                    settingsManager.setRequestBGAccess(true)
                }
            }

            true
        }

        alertsPref.setOnPreferenceChangeListener { preference, newValue ->
            val value = newValue as Boolean

            if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!preference.context.notificationPermissionEnabled()) {
                    alertNotifPermissionLauncher.requestPermission(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnPreferenceChangeListener false
                }
            }

            true
        }

        notIconPref.isVisible = onGoingPref.isChecked
    }
}