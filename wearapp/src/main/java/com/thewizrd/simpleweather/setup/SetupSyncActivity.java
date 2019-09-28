package com.thewizrd.simpleweather.setup;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.view.ConfirmationOverlay;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.widget.CircularProgressLayout;

import com.google.android.wearable.intent.RemoteIntent;
import com.thewizrd.shared_resources.helpers.WearConnectionStatus;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

public class SetupSyncActivity extends Activity {
    private boolean settingsDataReceived = false;
    private boolean locationDataReceived = false;
    private boolean weatherDataReceived = false;

    private CircularProgressLayout mCircularProgress;
    private TextView mTextView;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter intentFilter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create your application here
        setContentView(R.layout.activity_setup_sync);

        mCircularProgress = findViewById(R.id.circular_progress);
        mCircularProgress.setIndeterminate(true);
        mCircularProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // User canceled, abort the action
                mCircularProgress.stopTimer();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        mCircularProgress.setOnTimerFinishedListener(new CircularProgressLayout.OnTimerFinishedListener() {
            @Override
            public void onTimerFinished(CircularProgressLayout layout) {
                // User didn't cancel, perform the action
                // All data received finish activity
                if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                    setResult(RESULT_OK);
                else
                    setResult(RESULT_CANCELED);
                finish();
            }
        });
        mTextView = findViewById(R.id.message);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WearableDataListenerService.ACTION_UPDATECONNECTIONSTATUS.equals(intent.getAction())) {
                    WearConnectionStatus connStatus = WearConnectionStatus.valueOf(intent.getIntExtra(WearableDataListenerService.EXTRA_CONNECTIONSTATUS, 0));
                    switch (connStatus) {
                        case DISCONNECTED:
                            mTextView.setText(R.string.status_disconnected);
                            errorProgress();
                            break;
                        case CONNECTING:
                            mTextView.setText(R.string.status_connecting);
                            resetTimer();
                            break;
                        case APPNOTINSTALLED:
                            mTextView.setText(R.string.error_notinstalled);
                            resetTimer();

                            // Open store on remote device
                            Intent intentAndroid = new Intent(Intent.ACTION_VIEW)
                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                                    .setData(WearableHelper.getPlayStoreURI());

                            RemoteIntent.startRemoteActivity(SetupSyncActivity.this, intentAndroid,
                                    new ConfirmationResultReceiver(SetupSyncActivity.this));

                            errorProgress();
                            break;
                        case CONNECTED:
                            mTextView.setText(R.string.status_connected);
                            resetTimer();
                            // Continue operation
                            startService(new Intent(SetupSyncActivity.this, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_REQUESTSETUPSTATUS));
                            break;
                    }
                } else if (WearableHelper.ErrorPath.equals(intent.getAction())) {
                    mTextView.setText(R.string.error_syncing);
                    errorProgress();
                } else if (WearableHelper.IsSetupPath.equals(intent.getAction())) {
                    boolean isDeviceSetup = intent.getBooleanExtra(WearableDataListenerService.EXTRA_DEVICESETUPSTATUS, false);

                    start(isDeviceSetup);
                } else if (WearableDataListenerService.ACTION_OPENONPHONE.equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra(WearableDataListenerService.EXTRA_SUCCESS, false);

                    new ConfirmationOverlay()
                            .setType(success ? ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION : ConfirmationOverlay.FAILURE_ANIMATION)
                            .showOn(SetupSyncActivity.this);

                    if (!success) {
                        mTextView.setText(R.string.error_syncing);
                        errorProgress();
                    }
                } else if (WearableHelper.SettingsPath.equals(intent.getAction())) {
                    mTextView.setText(R.string.message_settingsretrieved);
                    settingsDataReceived = true;

                    if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                        successProgress();
                } else if (WearableHelper.LocationPath.equals(intent.getAction())) {
                    mTextView.setText(R.string.message_locationretrieved);
                    locationDataReceived = true;

                    if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                        successProgress();
                } else if (WearableHelper.WeatherPath.equals(intent.getAction())) {
                    mTextView.setText(R.string.message_weatherretrieved);
                    weatherDataReceived = true;

                    if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                        successProgress();
                }
            }
        };

        mTextView.setText(R.string.message_gettingstatus);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WearableDataListenerService.ACTION_UPDATECONNECTIONSTATUS);
        intentFilter.addAction(WearableHelper.IsSetupPath);
        intentFilter.addAction(WearableHelper.LocationPath);
        intentFilter.addAction(WearableHelper.SettingsPath);
        intentFilter.addAction(WearableHelper.WeatherPath);
        intentFilter.addAction(WearableHelper.ErrorPath);

        startService(new Intent(this, WearableDataListenerService.class)
                .setAction(WearableDataListenerService.ACTION_UPDATECONNECTIONSTATUS));
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, intentFilter);
        // Allow service to parse OnDataChanged updates
        WearableDataListenerService.setAcceptDataUpdates(true);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mBroadcastReceiver);

        settingsDataReceived = false;
        locationDataReceived = false;
        weatherDataReceived = false;

        // Disallow service to parse OnDataChanged updates
        WearableDataListenerService.setAcceptDataUpdates(false);

        super.onPause();
    }

    private void errorProgress() {
        mCircularProgress.setIndeterminate(false);
        mCircularProgress.setTotalTime(5000);
        mCircularProgress.startTimer();

        settingsDataReceived = false;
        locationDataReceived = false;
        weatherDataReceived = false;
    }

    private void resetTimer() {
        mCircularProgress.stopTimer();
        mCircularProgress.setIndeterminate(true);
    }

    private void successProgress() {
        mTextView.setText(R.string.message_synccompleted);

        mCircularProgress.setIndeterminate(false);
        mCircularProgress.setTotalTime(1);
        mCircularProgress.startTimer();
    }

    private void start(boolean isDeviceSetup) {
        if (isDeviceSetup) {
            mTextView.setText(R.string.message_retrievingdata);

            startService(new Intent(this, WearableDataListenerService.class)
                    .setAction(WearableDataListenerService.ACTION_REQUESTSETTINGSUPDATE));
            startService(new Intent(this, WearableDataListenerService.class)
                    .setAction(WearableDataListenerService.ACTION_REQUESTLOCATIONUPDATE));
            startService(new Intent(this, WearableDataListenerService.class)
                    .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE));
        } else {
            mTextView.setText(R.string.message_continueondevice);

            startService(new Intent(this, WearableDataListenerService.class)
                    .setAction(WearableDataListenerService.ACTION_OPENONPHONE));
        }
    }
}
