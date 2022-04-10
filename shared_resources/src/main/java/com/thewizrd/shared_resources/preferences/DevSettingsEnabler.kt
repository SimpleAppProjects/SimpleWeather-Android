package com.thewizrd.shared_resources.preferences

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.shared_resources.utils.CommonActions

object DevSettingsEnabler {
    private const val KEY_DEVSETTINGSENABLED = "key_devsettingsenabled"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences("devsettings", Context.MODE_PRIVATE)
    }

    fun isDevSettingsEnabled(context: Context): Boolean {
        val preferences = getPreferences(context)
        return preferences.getBoolean(KEY_DEVSETTINGSENABLED, false)
    }

    fun setDevSettingsEnabled(context: Context, enable: Boolean) {
        val preferences = getPreferences(context)
        preferences.edit(true) {
            putBoolean(KEY_DEVSETTINGSENABLED, enable)
        }
        LocalBroadcastManager.getInstance(context.applicationContext)
            .sendBroadcast(Intent(CommonActions.ACTION_SETTINGS_SENDUPDATE))
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun getPreferenceMap(context: Context): Map<String, Any?> {
        val preferences = getPreferences(context)
        return preferences.all.minus(KEY_DEVSETTINGSENABLED)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun clearPreferences(context: Context, enable: Boolean? = null) {
        val preferences = getPreferences(context)
        val enabled = enable ?: isDevSettingsEnabled(context)
        preferences.edit(true) {
            clear()
            putBoolean(KEY_DEVSETTINGSENABLED, enabled)
        }
    }
}