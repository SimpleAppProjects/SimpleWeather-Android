package com.thewizrd.shared_resources.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.thewizrd.shared_resources.SimpleLibrary
import java.util.*

object LocaleUtils {
    const val KEY_LANGUAGE = "key_language"
    private var sLocale: Locale? = null

    @JvmStatic
    fun attachBaseContext(context: Context): Context {
        val oldConfig = context.resources.configuration
        val newConfig = Configuration(oldConfig)

        val locale = getLocale(context)

        newConfig.setLocale(locale)

        return context.createConfigurationContext(newConfig)
    }

    private fun updateAppContextLocale() {
        val context = SimpleLibrary.instance.appContext
        val oldConfig = context.resources.configuration
        val newConfig = Configuration(oldConfig)

        val locale = getLocale()

        newConfig.setLocale(locale)

        context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
    }

    @JvmStatic
    fun getLocaleCode(): String? {
        return SimpleLibrary.instance.app.preferences.getString(KEY_LANGUAGE, "")
    }

    @JvmStatic
    fun getLocaleCode(context: Context): String? {
        return getPreferences(context).getString(KEY_LANGUAGE, "")
    }

    @JvmStatic
    fun setLocaleCode(localeCode: String?) {
        SimpleLibrary.instance.app.preferences.edit {
            putString(KEY_LANGUAGE, localeCode)
        }
        updateLocale(localeCode)
        updateAppContextLocale()
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @JvmStatic
    fun getLocale(): Locale {
        if (sLocale == null) {
            updateLocale(getLocaleCode())
        }

        return sLocale!!
    }

    private fun getLocale(context: Context): Locale {
        if (sLocale == null) {
            updateLocale(getLocaleCode(context))
        }

        return sLocale!!
    }

    private fun updateLocale(localeCode: String?) {
        sLocale = getLocaleForCode(localeCode)
    }

    @JvmStatic
    fun getLocaleDisplayName(): String {
        return getLocale().let {
            it.getDisplayName(it)
        }
    }

    private fun getLocaleForCode(localeCode: String?): Locale {
        return if (!localeCode.isNullOrBlank()) {
            Locale(localeCode)
        } else {
            Locale.getDefault()
        }
    }

    @JvmStatic
    fun getLocaleForTag(localeCode: String?): Locale {
        return if (localeCode.isNullOrBlank()) {
            Locale.getDefault()
        } else if (localeCode.contains('-') || localeCode.contains('_')) {
            Locale.forLanguageTag(localeCode)
        } else {
            Locale(localeCode)
        }
    }
}