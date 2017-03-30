package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.thewizrd.simpleweather.utils.Settings;

import java.util.List;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        Intent intent = null;
        int homeIdx = 0;

        String ARG_QUERY = "query";
        String ARG_INDEX = "index";

        try {
            if (Settings.isWeatherLoaded()) {
                List<String> locations = Settings.getLocations_WU();
                String local = locations.get(homeIdx);

                intent = new Intent(this, MainActivity.class);
                intent.putExtra(ARG_QUERY, local);
                intent.putExtra(ARG_INDEX, homeIdx);
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
