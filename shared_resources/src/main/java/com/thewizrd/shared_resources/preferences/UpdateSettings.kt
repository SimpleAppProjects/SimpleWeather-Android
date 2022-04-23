package com.thewizrd.shared_resources.preferences

import androidx.core.content.edit
import com.thewizrd.shared_resources.appLib

object UpdateSettings {
    private val preferences = appLib.preferences

    private const val KEY_UPDATEAVAILABLE = "key_updateavailable"

    @JvmStatic
    var isUpdateAvailable: Boolean
        get() {
            return preferences.getBoolean(KEY_UPDATEAVAILABLE, false)
        }
        set(value) {
            preferences.edit {
                putBoolean(KEY_UPDATEAVAILABLE, value)
            }
        }
}