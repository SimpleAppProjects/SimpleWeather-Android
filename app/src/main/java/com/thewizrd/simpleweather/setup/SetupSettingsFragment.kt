package com.thewizrd.simpleweather.setup

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupSettingsBinding
import com.thewizrd.simpleweather.extras.enableAdditionalRefreshIntervals
import com.thewizrd.simpleweather.preferences.CustomPreferenceFragmentCompat
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager

class SetupSettingsFragment : CustomPreferenceFragmentCompat() {
    private lateinit var binding: FragmentSetupSettingsBinding
    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        locationPermissionLauncher = LocationPermissionLauncher(requireActivity())
    }

    @NonNull
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

        setDivider(ColorDrawable(root.context.getAttrColor(R.attr.colorOnSurface)))
        setDividerHeight(root.context.dpToPx(1f).toInt())

        return root
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_setup, rootKey)

        val intervalPref = findPreference<ListPreference>(SettingsManager.KEY_REFRESHINTERVAL)!!
        val notIconPref = findPreference<ListPreference>(SettingsManager.KEY_NOTIFICATIONICON)!!
        val onGoingPref =
            findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_ONGOINGNOTIFICATION)!!

        if (enableAdditionalRefreshIntervals()) {
            intervalPref.setEntries(R.array.premium_refreshinterval_entries)
            intervalPref.setEntryValues(R.array.premium_refreshinterval_values)
        } else {
            intervalPref.setEntries(R.array.refreshinterval_entries)
            intervalPref.setEntryValues(R.array.refreshinterval_values)
        }

        onGoingPref.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as Boolean
            notIconPref.isVisible = value

            if (value && settingsManager.useFollowGPS() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() && requireActivity().backgroundLocationPermissionEnabled()) {
                runWithView {
                    val ctx = requireActivity()
                    val snackbar = Snackbar.make(
                        ctx,
                        ctx.getBackgroundLocationRationale(),
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

        notIconPref.isVisible = onGoingPref.isChecked
    }
}