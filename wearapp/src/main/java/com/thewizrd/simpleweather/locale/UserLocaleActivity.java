package com.thewizrd.simpleweather.locale;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;

import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.activities.AppCompatLiteActivity;

public abstract class UserLocaleActivity extends AppCompatLiteActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.attachBaseContext(newBase));
    }

    protected boolean enableLocaleChangeListener() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (enableLocaleChangeListener()) {
            App.getInstance().registerAppSharedPreferenceListener(listener);
        }
    }

    @Override
    protected void onStop() {
        if (enableLocaleChangeListener()) {
            App.getInstance().unregisterAppSharedPreferenceListener(listener);
        }
        super.onStop();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        if (!StringUtils.isNullOrWhitespace(key)) {
            if (LocaleUtils.KEY_LANGUAGE.equals(key)) {
                ActivityCompat.recreate(UserLocaleActivity.this);
            }
        }
    };
}