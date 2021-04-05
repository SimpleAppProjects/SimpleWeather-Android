package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.thewizrd.extras.ExtrasLibrary;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.simpleweather.activity.UserLocaleActivity;
import com.thewizrd.simpleweather.main.MainActivity;
import com.thewizrd.simpleweather.setup.SetupActivity;

public class LaunchActivity extends UserLocaleActivity {

    private static final String TAG = "LaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SettingsManager settingsMgr = App.getInstance().getSettingsManager();

        Intent intent = null;

        try {
            if (settingsMgr.isWeatherLoaded()) {
                intent = new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            } else {
                intent = new Intent(this, SetupActivity.class);
            }

            // Update configuration
            RemoteConfig.checkConfig();

            // Check premium status
            ExtrasLibrary.Companion.checkPremiumStatus();
        } catch (Exception e) {
            Logger.writeLine(Log.ERROR, e, "SimpleWeather: %s: error loading", TAG);
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
