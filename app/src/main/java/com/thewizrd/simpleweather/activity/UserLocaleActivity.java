package com.thewizrd.simpleweather.activity;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.thewizrd.shared_resources.utils.LocaleUtils;

public abstract class UserLocaleActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.attachBaseContext(newBase));
        SplitCompat.installActivity(this);
    }
}
