package com.thewizrd.simpleweather.setup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.wear.widget.CircularProgressLayout;

import com.google.android.wearable.intent.RemoteIntent;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.wearable.WearConnectionStatus;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.ActivitySetupSyncBinding;
import com.thewizrd.simpleweather.helpers.ConfirmationResultReceiver;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.wearable.WearableListenerActivity;
import com.thewizrd.simpleweather.wearable.WearableWorker;

public class SetupSyncActivity extends WearableListenerActivity {
    private boolean settingsDataReceived = false;
    private boolean locationDataReceived = false;
    private boolean weatherDataReceived = false;

    private ActivitySetupSyncBinding binding;

    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter intentFilter;

    @Override
    protected BroadcastReceiver getBroadcastReceiver() {
        return mBroadcastReceiver;
    }

    @Override
    protected IntentFilter getIntentFilter() {
        return intentFilter;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsLogger.logEvent("SetupSyncActivity: onCreate");

        binding = ActivitySetupSyncBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.circularProgress.setIndeterminate(true);
        binding.circularProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // User canceled, abort the action
                binding.circularProgress.stopTimer();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        binding.circularProgress.setOnTimerFinishedListener(new CircularProgressLayout.OnTimerFinishedListener() {
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
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_UPDATECONNECTIONSTATUS.equals(intent.getAction())) {
                    WearConnectionStatus connStatus = WearConnectionStatus.valueOf(intent.getIntExtra(EXTRA_CONNECTIONSTATUS, 0));
                    switch (connStatus) {
                        case DISCONNECTED:
                            binding.message.setText(R.string.status_disconnected);
                            errorProgress();
                            break;
                        case CONNECTING:
                            binding.message.setText(R.string.status_connecting);
                            resetTimer();
                            break;
                        case APPNOTINSTALLED:
                            binding.message.setText(R.string.error_notinstalled);
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
                            binding.message.setText(R.string.status_connected);
                            resetTimer();
                            // Continue operation
                            AsyncTask.run(new Runnable() {
                                @Override
                                public void run() {
                                    sendSetupStatusRequest();
                                }
                            });
                            break;
                    }
                } else if (WearableHelper.ErrorPath.equals(intent.getAction())) {
                    binding.message.setText(R.string.error_syncing);
                    errorProgress();
                } else if (WearableHelper.IsSetupPath.equals(intent.getAction())) {
                    boolean isDeviceSetup = intent.getBooleanExtra(EXTRA_DEVICESETUPSTATUS, false);
                    start(isDeviceSetup);
                } else if (WearableHelper.SettingsPath.equals(intent.getAction())) {
                    binding.message.setText(R.string.message_settingsretrieved);
                    settingsDataReceived = true;

                    if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                        successProgress();
                } else if (WearableHelper.LocationPath.equals(intent.getAction())) {
                    binding.message.setText(R.string.message_locationretrieved);
                    locationDataReceived = true;

                    if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                        successProgress();
                } else if (WearableHelper.WeatherPath.equals(intent.getAction())) {
                    binding.message.setText(R.string.message_weatherretrieved);
                    weatherDataReceived = true;

                    if (settingsDataReceived && locationDataReceived && weatherDataReceived)
                        successProgress();
                }
            }
        };

        binding.message.setText(R.string.message_gettingstatus);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WearableHelper.IsSetupPath);
        intentFilter.addAction(WearableHelper.LocationPath);
        intentFilter.addAction(WearableHelper.SettingsPath);
        intentFilter.addAction(WearableHelper.WeatherPath);
        intentFilter.addAction(WearableHelper.ErrorPath);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AnalyticsLogger.logEvent("SetupSyncActivity: onResume");

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, intentFilter);
        // Allow service to parse OnDataChanged updates
        this.setAcceptDataUpdates(true);
        WearableDataListenerService.setAcceptDataUpdates(true);

        sendSetupStatusRequest();
    }

    @Override
    protected void onPause() {
        AnalyticsLogger.logEvent("SetupSyncActivity: onPause");

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mBroadcastReceiver);

        settingsDataReceived = false;
        locationDataReceived = false;
        weatherDataReceived = false;

        // Disallow service to parse OnDataChanged updates
        WearableDataListenerService.setAcceptDataUpdates(false);
        this.setAcceptDataUpdates(false);

        super.onPause();
    }

    private void errorProgress() {
        binding.circularProgress.setIndeterminate(false);
        binding.circularProgress.setTotalTime(5000);
        binding.circularProgress.startTimer();

        settingsDataReceived = false;
        locationDataReceived = false;
        weatherDataReceived = false;
    }

    private void resetTimer() {
        binding.circularProgress.stopTimer();
        binding.circularProgress.setIndeterminate(true);
    }

    private void successProgress() {
        binding.message.setText(R.string.message_synccompleted);

        binding.circularProgress.setIndeterminate(false);
        binding.circularProgress.setTotalTime(1);
        binding.circularProgress.startTimer();
    }

    private void start(boolean isDeviceSetup) {
        if (isDeviceSetup) {
            binding.message.setText(R.string.message_retrievingdata);
            WearableWorker.enqueueAction(this, WearableWorker.ACTION_REQUESTUPDATE);
        } else {
            binding.message.setText(R.string.message_continueondevice);
            if (!openAppOnPhone(true)) {
                binding.message.setText(R.string.error_syncing);
                errorProgress();
            }
        }
    }
}
