package com.thewizrd.simpleweather.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.thewizrd.shared_resources.preferences.DevSettingsEnabler
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.R

class DevSettingsFragment : ToolbarPreferenceFragmentCompat() {

    override fun getTitle(): Int {
        return R.string.title_activity_settings
    }

    override fun onResume() {
        super.onResume()
        toolbar.title = "Dev Settings"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(context)

        val apiKeyCategory: PreferenceCategory

        preferenceScreen.addPreference(PreferenceCategory(context).apply {
            title = "API Keys"
        }.also { apiKeyCategory = it })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "Ambee Key (Pollen)"
            dialogTitle = "API Key"

            key = WeatherAPI.AMBEE
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                DevSettingsEnabler.getAPIKey(it.context, WeatherAPI.AMBEE) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                DevSettingsEnabler.setAPIKey(preference.context, preference.key, newValue?.toString())
                true
            }
        })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "AccuWeather Key"
            dialogTitle = "API Key"

            key = WeatherAPI.ACCUWEATHER
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                DevSettingsEnabler.getAPIKey(it.context, WeatherAPI.ACCUWEATHER) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                DevSettingsEnabler.setAPIKey(preference.context, preference.key, newValue?.toString())
                true
            }
        })
    }
}