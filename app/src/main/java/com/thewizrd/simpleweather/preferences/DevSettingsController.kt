package com.thewizrd.simpleweather.preferences

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.thewizrd.shared_resources.preferences.DevSettingsEnabler
import com.thewizrd.simpleweather.R

class DevSettingsController(private val prefFragment: PreferenceFragmentCompat, private val prefKey: String) {
    private val context = prefFragment.requireContext()

    private val TAPS_TO_BE_A_DEVELOPER = 7

    private var mDevHitCountdown: Int = 0
    private var mDevHitToast: Toast? = null

    private lateinit var devSettingPref: Preference

    init {
        prefFragment.lifecycleScope.launchWhenStarted {
            onStart()
        }
    }

    fun onCreatePreferences(screen: PreferenceScreen) {
        screen.addPreference(Preference(context).apply {
            title = "Developer settings"
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                prefFragment.listView.findNavController().navigate(R.id.action_aboutAppFragment_to_devSettingsFragment)
                true
            }
            isVisible = DevSettingsEnabler.isDevSettingsEnabled(context)
        }.also { devSettingPref = it })
    }

    fun onStart() {
        mDevHitCountdown = if (DevSettingsEnabler.isDevSettingsEnabled(context)) {
            -1
        } else {
            TAPS_TO_BE_A_DEVELOPER
        }
        mDevHitToast = null
    }

    fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == prefKey) {
            if (mDevHitCountdown > 0) {
                mDevHitCountdown--
                if (mDevHitCountdown == 0) {
                    mDevHitToast?.cancel()
                    mDevHitToast = Toast.makeText(preference.context, "Dev settings enabled.", Toast.LENGTH_LONG)
                    mDevHitToast!!.show()
                    enableDevSettings()
                } else if (mDevHitCountdown > 0 && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER - 2)) {
                    mDevHitToast?.cancel()
                    mDevHitToast = Toast.makeText(preference.context, "You are now $mDevHitCountdown steps away from being a developer.", Toast.LENGTH_SHORT)
                    mDevHitToast!!.show()
                }
            } else if (mDevHitCountdown < 0) {
                mDevHitToast?.cancel()
                mDevHitToast = Toast.makeText(preference.context, "Dev settings already enabled.", Toast.LENGTH_LONG)
                mDevHitToast!!.show()
            }
            return true
        } else {
            return false
        }
    }

    private fun enableDevSettings() {
        mDevHitCountdown = 0
        DevSettingsEnabler.setDevSettingsEnabled(context, true)
        // Refresh prefs
        prefFragment.activity?.recreate()
    }
}