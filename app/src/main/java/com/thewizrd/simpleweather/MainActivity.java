package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.thewizrd.simpleweather.utils.Settings;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = null;
        try {
            if (Settings.isWeatherLoaded()) {
                java.util.List<String> locations = Settings.getLocations_WU();
                String local = locations.get(0);

                intent = new Intent(this, WeatherNow.class);
                intent.putExtra("query", local);
            } else {
                intent = new Intent(this, Setup.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (intent == null) {
                intent = new Intent(this, Setup.class);
            }

            // Navigate
            startActivity(intent);
            finishAffinity();
        }
    }
}
