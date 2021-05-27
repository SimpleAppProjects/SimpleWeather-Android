package com.thewizrd.shared_resources.preferences

import android.content.Context
import androidx.core.content.edit

object DevSettingsEnabler {
    private const val KEY_DEVSETTINGSENABLED = "key_devsettingsenabled"

    fun isDevSettingsEnabled(context: Context): Boolean {
        val preferences = context.applicationContext.getSharedPreferences("devsettings", Context.MODE_PRIVATE)
        return preferences.getBoolean(KEY_DEVSETTINGSENABLED, false)
    }

    fun setDevSettingsEnabled(context: Context, enable: Boolean) {
        val preferences = context.applicationContext.getSharedPreferences("devsettings", Context.MODE_PRIVATE)
        preferences.edit(true) {
            putBoolean(KEY_DEVSETTINGSENABLED, enable)
        }
    }

    fun getAPIKey(context: Context, key: String): String? {
        val preferences = context.applicationContext.getSharedPreferences("devsettings", Context.MODE_PRIVATE)
        return preferences.getString(key, null)
    }

    fun setAPIKey(context: Context, key: String, value: String?) {
        val preferences = context.applicationContext.getSharedPreferences("devsettings", Context.MODE_PRIVATE)
        preferences.edit(true) {
            putString(key, value)
        }
    }
}