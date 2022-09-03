package com.thewizrd.shared_resources.designer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.thewizrd.shared_resources.*
import com.thewizrd.shared_resources.preferences.SettingsManager

fun Context.initializeDependencies() {
    val appContext = this.applicationContext

    sharedDeps = object : SharedModule() {
        override val context: Context
            get() = appContext
    }

    appLib = object : ApplicationLib() {
        override val context: Context
            get() = appContext
        override val preferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(appContext)

        override fun registerAppSharedPreferenceListener() {
        }

        override fun registerAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        }

        override fun unregisterAppSharedPreferenceListener() {
        }

        override fun unregisterAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        }

        override val appState: AppState
            get() = AppState.FOREGROUND
        override val isPhone: Boolean
            get() = true
        override val properties: Bundle
            get() = Bundle.EMPTY
        override val settingsManager: SettingsManager
            get() = SettingsManager(appContext)

    }
}