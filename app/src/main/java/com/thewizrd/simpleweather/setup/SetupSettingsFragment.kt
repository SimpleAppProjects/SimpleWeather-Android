package com.thewizrd.simpleweather.setup

import android.Manifest
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
import com.thewizrd.shared_resources.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.shared_resources.helpers.getBackgroundLocationRationale
import com.thewizrd.shared_resources.helpers.openAppSettingsActivity
import com.thewizrd.shared_resources.helpers.requestBackgroundLocationPermission
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    @NonNull
    override fun createSnackManager(): SnackbarManager {
        val mStepperNavBar = appCompatActivity.findViewById<View>(R.id.bottom_nav_bar)

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
                    val snackbar = Snackbar.make(
                        requireActivity().getBackgroundLocationRationale(),
                        Snackbar.Duration.VERY_LONG
                    ).apply {
                        setAction(android.R.string.ok) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                                requireActivity().openAppSettingsActivity()
                            } else {
                                requestBackgroundLocationPermission(0)
                            }
                        }
                    }
                    showSnackbar(snackbar, null)
                    settingsManager.setRequestBGAccess(true)
                }
            }

            true
        }

        notIconPref.isVisible = onGoingPref.isChecked
    }
}