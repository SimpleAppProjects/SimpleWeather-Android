@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.simpleweather.preferences.SettingsActivity

fun initializeExtras() {
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

fun SettingsActivity.SettingsFragment.navigateToPremiumFragment() {
    // no-op
}

fun SettingsActivity.IconsFragment.navigateUnsupportedIconPack() {
    // no-op
}