package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.thewizrd.simpleweather.utils.Settings;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        Intent intent = null;
        try {
            if (Settings.isWeatherLoaded()) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, SetupActivity.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (intent == null) {
                intent = new Intent(this, SetupActivity.class);
            }

            // Navigate
            startActivity(intent);
            finishAffinity();
        }
    }
}
