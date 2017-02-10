package com.thewizrd.simpleweather;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.thewizrd.simpleweather.utils.Settings;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = null;
        try {
            if (Settings.isWeatherLoaded()) {
                intent = new Intent(this, WeatherNow.class);
            } else {
                intent = new Intent(this, Setup.class);
            }
        } catch (IOException e) {
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
