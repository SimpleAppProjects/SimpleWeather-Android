@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import android.content.Context
import androidx.preference.Preference
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.FileLoggingTree
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.preferences.SettingsFragment

fun initializeExtras(app: ApplicationLib) {
    if (!BuildConfig.DEBUG) {
        Logger.registerLogger(FileLoggingTree(app.appContext))
    }
}

fun App.attachToBaseContext(context: Context) {
    // no-op
}

fun UserLocaleActivity.attachToBaseContext() {
    // no-op
}

fun isIconPackSupported(packKey: String?): Boolean {
    return packKey != null && WeatherIconsManager.DEFAULT_ICONS.containsKey(packKey)
}

fun isWeatherAPISupported(api: String?): Boolean {
    return true
}

fun SettingsFragment.navigateToPremiumFragment() {
    // no-op
}

fun SettingsFragment.IconsFragment.navigateUnsupportedIconPack() {
    // no-op
}

fun enableAdditionalRefreshIntervals(): Boolean {
    return false
}

fun checkPremiumStatus() {
    // no-op
}

fun isPremiumSupported(): Boolean {
    return false
}

fun isRadarInteractionEnabled(): Boolean {
    return false
}

fun SettingsFragment.createPremiumPreference(): Preference? {
    return null
}

fun SettingsFragment.AboutAppFragment.setupReviewPreference(preference: Preference) {
    preference.isVisible = false
}