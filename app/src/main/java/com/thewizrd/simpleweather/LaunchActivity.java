package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.main.MainActivity;
import com.thewizrd.simpleweather.setup.SetupActivity;

public class LaunchActivity extends AppCompatActivity {

    private static final String TAG = "LaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = null;

        try {
            if (Settings.isWeatherLoaded() && Settings.isOnBoardingComplete()) {
                intent = new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Constants.KEY_DATA, JSONParser.serializer(Settings.getHomeData(), LocationData.class));
            } else {
                intent = new Intent(this, SetupActivity.class);
            }
        } catch (Exception e) {
            Logger.writeLine(Log.ERROR, e);
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
