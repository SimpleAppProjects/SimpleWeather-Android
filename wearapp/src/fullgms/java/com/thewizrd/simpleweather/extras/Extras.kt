@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import android.content.Intent
import android.widget.Toast
import com.thewizrd.extras.extrasModule
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.SettingsActivity
import com.thewizrd.simpleweather.wearable.WearableListenerActivity

fun initializeExtras() {
    extrasModule.initialize()
}

fun isIconPackSupported(packKey: String?): Boolean {
    return extrasModule.isIconPackSupported(packKey)
}

fun isWeatherAPISupported(api: String?): Boolean {
    return extrasModule.isWeatherAPISupported(api)
}

fun isPremiumWeatherAPI(api: String?): Boolean {
    return extrasModule.isPremiumWeatherAPI(api)
}

fun SettingsActivity.SettingsFragment.navigateToPremiumFragment() {
    // Navigate to premium page
    showToast(R.string.message_premium_required, Toast.LENGTH_SHORT);
    localBroadcastManager.sendBroadcast(
        Intent(WearableListenerActivity.ACTION_OPENONPHONE)
            .putExtra(WearableListenerActivity.EXTRA_SHOWANIMATION, true)
    )
    return
}

fun SettingsActivity.IconsFragment.navigateUnsupportedIconPack() {
    // Navigate to premium page
    showToast(R.string.message_premium_required, Toast.LENGTH_SHORT);
    localBroadcastManager.sendBroadcast(
        Intent(WearableListenerActivity.ACTION_OPENONPHONE)
            .putExtra(WearableListenerActivity.EXTRA_SHOWANIMATION, true)
    )
    return
}