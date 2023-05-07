@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import android.content.Context
import androidx.preference.Preference
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.preferences.SettingsFragment
import com.thewizrd.simpleweather.preferences.BaseSettingsFragment

fun initializeExtras() {
    // no-op
}

fun initializeFirebase(context: Context) {
    // no-op
}

fun App.attachToBaseContext(context: Context) {
    // no-op
}

fun UserLocaleActivity.attachToBaseContext() {
    // no-op
}

fun isIconPackSupported(packKey: String?): Boolean {
    return packKey != null && sharedDeps.weatherIconsManager.defaultIconProviders.containsKey(
        packKey
    )
}

fun isWeatherAPISupported(api: String?): Boolean {
    return true
}

fun isPremiumWeatherAPI(api: String?): Boolean {
    return false
}

fun BaseSettingsFragment.navigateToPremiumFragment() {
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

fun areNotificationExtrasEnabled(): Boolean {
    return false
}

fun SettingsFragment.createPremiumPreference(): Preference? {
    return null
}

fun SettingsFragment.AboutAppFragment.setupReviewPreference(preference: Preference) {
    preference.isVisible = false
}