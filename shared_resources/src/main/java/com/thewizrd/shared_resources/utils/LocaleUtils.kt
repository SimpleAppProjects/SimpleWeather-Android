package com.thewizrd.shared_resources.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.sharedDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val context = sharedDeps.context
        val oldConfig = context.resources.configuration
        val newConfig = Configuration(oldConfig)

        val locale = getLocale()

        newConfig.setLocale(locale)

        context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
    }

    @JvmStatic
    fun getLocaleCode(): String? {
        return appLib.preferences.getString(KEY_LANGUAGE, "")
    }

    private fun getLocaleCode(context: Context): String? {
        return getPreferences(context).getString(KEY_LANGUAGE, "")
    }

    @JvmStatic
    fun setLocaleCode(localeCode: String?) {
        appLib.preferences.edit {
            putString(KEY_LANGUAGE, localeCode)
        }
        updateLocale(localeCode)
        updateAppContextLocale()

        // TODO: NOTE: bug when app is restarted appContext locale is reset
        appLib.appScope.launch(Dispatchers.Main.immediate) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(getLocale()))
        }
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
        sLocale = getLocaleForTag(localeCode)
    }

    @JvmStatic
    fun getLocaleDisplayName(): String {
        return getLocale().let {
            it.getDisplayName(it)
        }
    }

    @JvmStatic
    fun getLocaleForTag(localeCode: String?): Locale {
        return if (localeCode.isNullOrBlank()) {
            getDefault()
        } else if (localeCode.contains('-') || localeCode.contains('_')) {
            Locale.forLanguageTag(localeCode)
        } else {
            Locale(localeCode)
        }
    }

    @JvmStatic
    fun getDefault(): Locale {
        return runCatching {
            LocaleManagerCompat.getSystemLocales(appLib.context).get(0) ?: Locale.getDefault()
        }.getOrDefault(Locale.getDefault())
    }
}