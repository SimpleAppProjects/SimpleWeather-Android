package com.thewizrd.simpleweather.preferences

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.R

class DevSettingsFragment : ToolbarPreferenceFragmentCompat() {

    override val titleResId: Int
        get() = R.string.title_dev_settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        preferenceScreen = preferenceManager.createPreferenceScreen(context)

        val apiKeyCategory: PreferenceCategory

        preferenceScreen.addPreference(PreferenceCategory(context).apply {
            title = "API Keys"
        }.also { apiKeyCategory = it })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "AccuWeather Key"
            dialogTitle = "API Key"

            key = WeatherAPI.ACCUWEATHER
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                settingsManager.getAPIKey(WeatherAPI.ACCUWEATHER) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    settingsManager.setAPIKey(preference.key, newValue?.toString())
                    true
                }
        })

        apiKeyCategory.addPreference(EditTextPreference(context).apply {
            title = "Tomorrow.io Key"
            dialogTitle = "API Key"

            key = WeatherAPI.TOMORROWIO
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                settingsManager.getAPIKey(WeatherAPI.TOMORROWIO) ?: "null"
            }
            isPersistent = false
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    settingsManager.setAPIKey(preference.key, newValue?.toString())
                    true
                }
        })
    }
}