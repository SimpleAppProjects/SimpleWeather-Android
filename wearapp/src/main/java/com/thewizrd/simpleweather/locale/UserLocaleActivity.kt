package com.thewizrd.simpleweather.locale

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.ActivityCompat
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.StringUtils.isNullOrWhitespace
import com.thewizrd.simpleweather.activities.AppCompatLiteActivity

abstract class UserLocaleActivity : AppCompatLiteActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.attachBaseContext(newBase))
    }

    protected open fun enableLocaleChangeListener(): Boolean {
        return true
    }

    override fun onStart() {
        super.onStart()
        if (enableLocaleChangeListener()) {
            appLib.registerAppSharedPreferenceListener(listener)
        }
    }

    override fun onStop() {
        if (enableLocaleChangeListener()) {
            appLib.unregisterAppSharedPreferenceListener(listener)
        }
        super.onStop()
    }

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String ->
            if (!key.isNullOrWhitespace()) {
                if (LocaleUtils.KEY_LANGUAGE == key) {
                    ActivityCompat.recreate(this@UserLocaleActivity)
                }
            }
        }
}