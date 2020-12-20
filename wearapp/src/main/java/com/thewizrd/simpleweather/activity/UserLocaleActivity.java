package com.thewizrd.simpleweather.activity;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.App;

public abstract class UserLocaleActivity extends FragmentActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.attachBaseContext(newBase));
        SplitCompat.installActivity(this);
    }

    protected boolean enableLocaleChangeListener() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (enableLocaleChangeListener()) {
            App.getInstance().getPreferences().registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @Override
    protected void onStop() {
        if (enableLocaleChangeListener()) {
            App.getInstance().getPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        }
        super.onStop();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (!StringUtils.isNullOrWhitespace(key)) {
                if (LocaleUtils.KEY_LANGUAGE.equals(key)) {
                    ActivityCompat.recreate(UserLocaleActivity.this);
                }
            }
        }
    };
}