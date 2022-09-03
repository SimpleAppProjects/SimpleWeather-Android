package com.thewizrd.shared_resources

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.thewizrd.shared_resources.preferences.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

public lateinit var appLib: ApplicationLib

abstract class ApplicationLib {
    abstract val context: Context
    abstract val preferences: SharedPreferences
    abstract fun registerAppSharedPreferenceListener()
    abstract fun unregisterAppSharedPreferenceListener()
    abstract fun registerAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    abstract fun unregisterAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    abstract val appState: AppState
    abstract val isPhone: Boolean
    abstract val properties: Bundle
    abstract val settingsManager: SettingsManager
    open val appScope: CoroutineScope = MainScope()
}