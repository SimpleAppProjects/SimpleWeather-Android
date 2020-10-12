package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.activity.UserLocaleActivity;
import com.thewizrd.simpleweather.main.MainActivity;
import com.thewizrd.simpleweather.setup.SetupActivity;
import com.thewizrd.simpleweather.updates.InAppUpdateManager;

public class LaunchActivity extends UserLocaleActivity {

    private static final String TAG = "LaunchActivity";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private InAppUpdateManager appUpdateManager;
    private static final int INSTALL_REQUESTCODE = 168;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FeatureSettings.isUpdateAvailable()) {
            // Update is available; double check if mandatory
            appUpdateManager = InAppUpdateManager.create(getApplicationContext());

            appUpdateManager.shouldStartImmediateUpdateFlow()
                    .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            if (task.isSuccessful() && task.getResult()) {
                                appUpdateManager.startImmediateUpdateFlow(LaunchActivity.this, INSTALL_REQUESTCODE);
                            } else {
                                startMainActivity();
                            }
                        }
                    });
            return;
        }

        startMainActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Checks that the update is not stalled during 'onResume()'.
        // However, you should execute this check at all entry points into the app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FeatureSettings.isUpdateAvailable()) {
            appUpdateManager.resumeUpdateIfStarted(this, INSTALL_REQUESTCODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == INSTALL_REQUESTCODE) {
            if (resultCode != RESULT_OK) {
                // Update flow failed; exit
                finishAffinity();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startMainActivity() {
        Intent intent = null;

        try {
            if (Settings.isWeatherLoaded() && Settings.isOnBoardingComplete()) {
                intent = new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(Constants.KEY_DATA, JSONParser.serializer(Settings.getHomeData(), LocationData.class))
                        .putExtra(Constants.FRAGTAG_HOME, true);
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
