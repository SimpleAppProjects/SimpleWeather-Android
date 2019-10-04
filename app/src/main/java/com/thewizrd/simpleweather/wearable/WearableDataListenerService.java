package com.thewizrd.simpleweather.wearable;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.wearable.WearableSettings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.LaunchActivity;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;

import org.threeten.bp.Instant;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class WearableDataListenerService extends WearableListenerService {
    private static final String TAG = "WearableDataListenerService";

    // Actions
    public static final String ACTION_SENDSETTINGSUPDATE = "SimpleWeather.Droid.action.SEND_SETTINGS_UPDATE";
    public static final String ACTION_SENDLOCATIONUPDATE = "SimpleWeather.Droid.action.SEND_LOCATION_UPDATE";
    public static final String ACTION_SENDWEATHERUPDATE = "SimpleWeather.Droid.action.SEND_WEATHER_UPDATE";

    private Collection<Node> mWearNodesWithApp;
    private Collection<Node> mAllConnectedNodes;
    private boolean mLoaded = false;

    private static final int JOB_ID = 1002;
    private static final String NOT_CHANNEL_ID = "SimpleWeather.generalnotif";

    public static void enqueueWork(Context context, Intent work) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(work);
        } else {
            context.startService(work);
        }
    }

    private static void initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Gets an instance of the NotificationManager service
            Context context = App.getInstance().getAppContext();
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);

            if (mChannel == null) {
                String notchannel_name = context.getResources().getString(R.string.not_channel_name_general);

                mChannel = new NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW);
                // Configure the notification channel.
                mChannel.setShowBadge(false);
                mChannel.enableLights(false);
                mChannel.enableVibration(false);
                mNotifyMgr.createNotificationChannel(mChannel);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static Notification getForegroundNotification() {
        Context context = App.getInstance().getAppContext();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_watch_white_24dp)
                        .setContentTitle(context.getString(R.string.not_title_wearable_sync))
                        .setProgress(0, 0, true)
                        .setColor(Colors.SIMPLEBLUE)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationManager.IMPORTANCE_LOW);

        return mBuilder.build();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChannel();

            startForeground(JOB_ID, getForegroundNotification());
        }

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                mWearNodesWithApp = findWearDevicesWithApp();
                mAllConnectedNodes = findAllWearDevices();

                mLoaded = true;
            }
        });
    }

    @Override
    public void onDestroy() {
        mLoaded = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }

        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(WearableHelper.StartActivityPath)) {
            Intent startIntent = new Intent(this, LaunchActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(startIntent);
        } else if (messageEvent.getPath().equals(WearableHelper.SettingsPath)) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    createSettingsDataRequest(true);
                }
            });
        } else if (messageEvent.getPath().equals(WearableHelper.LocationPath)) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    createLocationDataRequest(true);
                }
            });
        } else if (messageEvent.getPath().equals(WearableHelper.WeatherPath)) {
            byte[] data = messageEvent.getData();
            boolean force = false;
            if (data != null && data.length > 0)
                force = !(data[0] == 0);

            if (!force) {
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        createWeatherDataRequest(true);
                    }
                });
            } else {
                // Refresh weather data
                WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            }
        } else if (messageEvent.getPath().equals(WearableHelper.IsSetupPath)) {
            sendSetupStatus(messageEvent.getSourceNodeId());
        }
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                mWearNodesWithApp = capabilityInfo.getNodes();
                mAllConnectedNodes = findAllWearDevices();
            }
        });
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(JOB_ID, getForegroundNotification());

        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (intent != null && ACTION_SENDSETTINGSUPDATE.equals(intent.getAction())) {
                    createSettingsDataRequest(true);
                } else if (intent != null && ACTION_SENDLOCATIONUPDATE.equals(intent.getAction())) {
                    createLocationDataRequest(true);
                } else if (intent != null && ACTION_SENDWEATHERUPDATE.equals(intent.getAction())) {
                    createWeatherDataRequest(true);
                } else if (intent != null) {
                    Logger.writeLine(Log.INFO, "%s: Unhandled action: %s", TAG, intent.getAction());
                }
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    stopForeground(true);
            }
        });

        return START_NOT_STICKY;
    }

    private Collection<Node> findWearDevicesWithApp() {
        CapabilityInfo capabilityInfo = null;

        try {
            capabilityInfo = Tasks.await(Wearable.getCapabilityClient(this)
                    .getCapability(WearableHelper.CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        if (capabilityInfo != null) {
            return capabilityInfo.getNodes();
        }

        return null;
    }

    private Collection<Node> findAllWearDevices() {
        List<Node> nodes = null;

        try {
            nodes = Tasks.await(Wearable.getNodeClient(this)
                    .getConnectedNodes());
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        return nodes;
    }

    private void createSettingsDataRequest(boolean urgent) {
        // Don't send anything unless we're setup
        if (!Settings.isWeatherLoaded())
            return;

        if (mWearNodesWithApp == null) {
            // Create requests if nodes exist with app support
            mWearNodesWithApp = findWearDevicesWithApp();

            if (mWearNodesWithApp == null || mWearNodesWithApp.size() == 0)
                return;
        }

        PutDataMapRequest mapRequest = PutDataMapRequest.create(WearableHelper.SettingsPath);
        mapRequest.getDataMap().putString(WearableSettings.KEY_API, Settings.getAPI());
        mapRequest.getDataMap().putString(WearableSettings.KEY_APIKEY, Settings.getAPIKEY());
        mapRequest.getDataMap().putBoolean(WearableSettings.KEY_APIKEY_VERIFIED, Settings.isKeyVerified());
        mapRequest.getDataMap().putBoolean(WearableSettings.KEY_FOLLOWGPS, Settings.useFollowGPS());
        mapRequest.getDataMap().putLong(WearableSettings.KEY_UPDATETIME, Instant.now().toEpochMilli());
        mapRequest.getDataMap().putString(WearableSettings.KEY_UNIT, Settings.getTempUnit());
        int interval = Settings.getRefreshInterval();
        mapRequest.getDataMap().putInt(WearableSettings.KEY_REFRESHINTERVAL, interval);
        PutDataRequest request = mapRequest.asPutDataRequest();
        if (urgent) request.setUrgent();
        try {
            Tasks.await(Wearable.getDataClient(this)
                    .putDataItem(request));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.ERROR, "%s: CreateSettingsDataRequest(): urgent: %s", TAG, Boolean.toString(urgent));
    }

    private void createLocationDataRequest(boolean urgent) {
        // Don't send anything unless we're setup
        if (!Settings.isWeatherLoaded())
            return;

        if (mWearNodesWithApp == null) {
            // Create requests if nodes exist with app support
            mWearNodesWithApp = findWearDevicesWithApp();

            if (mWearNodesWithApp == null || mWearNodesWithApp.size() == 0)
                return;
        }

        PutDataMapRequest mapRequest = PutDataMapRequest.create(WearableHelper.LocationPath);
        LocationData homeData = Settings.getHomeData();
        mapRequest.getDataMap().putString(WearableSettings.KEY_LOCATIONDATA, homeData == null ? null : homeData.toJson());
        mapRequest.getDataMap().putLong(WearableSettings.KEY_UPDATETIME, Instant.now().toEpochMilli());
        PutDataRequest request = mapRequest.asPutDataRequest();
        if (urgent) request.setUrgent();
        try {
            Tasks.await(Wearable.getDataClient(this)
                    .putDataItem(request));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.ERROR, "%s: CreateLocationDataRequest(): urgent: %s", TAG, Boolean.toString(urgent));
    }

    private void createWeatherDataRequest(boolean urgent) {
        // Don't send anything unless we're setup
        if (!Settings.isWeatherLoaded())
            return;

        if (mWearNodesWithApp == null) {
            // Create requests if nodes exist with app support
            mWearNodesWithApp = findWearDevicesWithApp();

            if (mWearNodesWithApp == null || mWearNodesWithApp.size() == 0)
                return;
        }

        PutDataMapRequest mapRequest = PutDataMapRequest.create(WearableHelper.WeatherPath);
        LocationData homeData = Settings.getHomeData();
        Weather weatherData = Settings.getWeatherData(homeData.getQuery());
        List<WeatherAlert> alertData = Settings.getWeatherAlertData(homeData.getQuery());

        if (weatherData != null) {
            weatherData.setWeatherAlerts(alertData);
            mapRequest.getDataMap().putAsset(WearableSettings.KEY_WEATHERDATA, Asset.createFromBytes(weatherData.toJson().getBytes(Charset.forName("UTF-8"))));
        }

        mapRequest.getDataMap().putLong(WearableSettings.KEY_UPDATETIME, Instant.now().toEpochMilli());

        PutDataRequest request = mapRequest.asPutDataRequest();
        if (urgent) request.setUrgent();
        try {
            Tasks.await(Wearable.getDataClient(this)
                    .putDataItem(request));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.ERROR, "%s: CreateWeatherDataRequest(): urgent: %s", TAG, Boolean.toString(urgent));
    }

    private void sendSetupStatus(String nodeID) {
        Wearable.getMessageClient(this)
                .sendMessage(nodeID, WearableHelper.IsSetupPath,
                        new byte[]{(byte) (Settings.isWeatherLoaded() ? 1 : 0)});
    }
}
