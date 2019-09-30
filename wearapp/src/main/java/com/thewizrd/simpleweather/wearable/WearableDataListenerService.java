package com.thewizrd.simpleweather.wearable;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.wearable.phone.PhoneDeviceType;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.wearable.WearConnectionStatus;
import com.thewizrd.shared_resources.wearable.WearWeatherJSON;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.wearable.WearableSettings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class WearableDataListenerService extends WearableListenerService {
    private static final String TAG = "WearableDataListenerService";

    // Actions
    public static final String ACTION_OPENONPHONE = "SimpleWeather.Droid.Wear.action.OPEN_APP_ON_PHONE";
    public static final String ACTION_SHOWSTORELISTING = "SimpleWeather.Droid.Wear.action.SHOW_STORE_LISTING";
    public static final String ACTION_UPDATECONNECTIONSTATUS = "SimpleWeather.Droid.Wear.action.UPDATE_CONNECTION_STATUS";
    public static final String ACTION_REQUESTSETTINGSUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_SETTINGS_UPDATE";
    public static final String ACTION_REQUESTLOCATIONUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_LOCATION_UPDATE";
    public static final String ACTION_REQUESTWEATHERUPDATE = "SimpleWeather.Droid.Wear.action.REQUEST_WEATHER_UPDATE";
    public static final String ACTION_REQUESTSETUPSTATUS = "SimpleWeather.Droid.Wear.action.REQUEST_SETUP_STATUS";

    // Extras
    public static final String EXTRA_SUCCESS = "SimpleWeather.Droid.Wear.extra.SUCCESS";
    public static final String EXTRA_CONNECTIONSTATUS = "SimpleWeather.Droid.Wear.extra.CONNECTION_STATUS";
    public static final String EXTRA_DEVICESETUPSTATUS = "SimpleWeather.Droid.Wear.extra.DEVICE_SETUP_STATUS";
    public static final String EXTRA_FORCEUPDATE = "SimpleWeather.Droid.Wear.extra.FORCE_UPDATE";

    private Node mPhoneNodeWithApp;
    private WearConnectionStatus mConnectionStatus = WearConnectionStatus.DISCONNECTED;
    private boolean mLoaded = false;
    private static boolean acceptDataUpdates = false;

    public static void setAcceptDataUpdates(boolean value) {
        acceptDataUpdates = value;
    }

    private static final int JOB_ID = 1001;
    private static final String NOT_CHANNEL_ID = "SimpleWeather.generalnotif";

    public static void enqueueWork(Context context, Intent work) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(work);
        } else {
            context.startService(work);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void initChannel() {
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

    @TargetApi(Build.VERSION_CODES.O)
    private static Notification getForegroundNotification() {
        Context context = App.getInstance().getAppContext();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_phone_android_white_24dp)
                        .setContentTitle(context.getString(R.string.not_title_device_sync))
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
                mPhoneNodeWithApp = checkIfPhoneHasApp();

                if (mPhoneNodeWithApp == null)
                    mConnectionStatus = WearConnectionStatus.APPNOTINSTALLED;
                else
                    mConnectionStatus = WearConnectionStatus.CONNECTED;

                LocalBroadcastManager.getInstance(WearableDataListenerService.this)
                        .sendBroadcast(new Intent(ACTION_UPDATECONNECTIONSTATUS)
                                .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));

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
        // Only handle data changes if
        // DataSync is on,
        // App hasn't been setup yet,
        // Or if we are setup but want to change location and sync data (SetupSyncActivity)
        if (Settings.getDataSync() != WearableDataSync.OFF || acceptDataUpdates) {
            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo(WearableHelper.SettingsPath) == 0) {
                        final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                updateSettings(dataMap);
                            }
                        });
                    } else if (item.getUri().getPath().compareTo(WearableHelper.LocationPath) == 0) {
                        final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                updateLocation(dataMap);
                            }
                        });
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals(WearableHelper.ErrorPath)) {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(WearableHelper.ErrorPath));
        } else if (messageEvent.getPath().equals(WearableHelper.IsSetupPath)) {
            byte[] data = messageEvent.getData();
            boolean isDeviceSetup = !(data[0] == 0);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(WearableHelper.IsSetupPath)
                            .putExtra(EXTRA_DEVICESETUPSTATUS, isDeviceSetup)
                            .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));
        } else if (messageEvent.getPath().equals(WearableHelper.WeatherPath)) {
            final byte[] data = messageEvent.getData();
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    updateWeather(data);
                }
            });
        }
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        super.onCapabilityChanged(capabilityInfo);

        mPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());

        if (mPhoneNodeWithApp == null) {
            mConnectionStatus = WearConnectionStatus.APPNOTINSTALLED;
        } else {
            mConnectionStatus = WearConnectionStatus.CONNECTED;
        }

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ACTION_UPDATECONNECTIONSTATUS)
                        .putExtra(EXTRA_DEVICESETUPSTATUS, mConnectionStatus.getValue()));
    }

    @Override
    public int onStartCommand(@NonNull final Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(JOB_ID, getForegroundNotification());

        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (ACTION_OPENONPHONE.equals(intent.getAction())) {
                    openAppOnPhone();
                } else if (ACTION_UPDATECONNECTIONSTATUS.equals(intent.getAction())) {
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this)
                            .sendBroadcast(new Intent(ACTION_UPDATECONNECTIONSTATUS)
                                    .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));
                } else if (ACTION_REQUESTSETTINGSUPDATE.equals(intent.getAction())) {
                    sendSettingsRequest();
                } else if (ACTION_REQUESTLOCATIONUPDATE.equals(intent.getAction())) {
                    sendLocationRequest();
                } else if (ACTION_REQUESTWEATHERUPDATE.equals(intent.getAction())) {
                    boolean forceUpdate = intent.getBooleanExtra(EXTRA_FORCEUPDATE, false);
                    if (!forceUpdate)
                        sendWeatherRequest();
                    else
                        sendWeatherUpdateRequest();
                } else if (ACTION_REQUESTSETUPSTATUS.equals(intent.getAction())) {
                    sendSetupStatusRequest();
                } else {
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

    private Node checkIfPhoneHasApp() {
        Node node = null;

        try {
            CapabilityInfo capabilityInfo = Tasks.await(Wearable.getCapabilityClient(this)
                    .getCapability(WearableHelper.CAPABILITY_PHONE_APP,
                            CapabilityClient.FILTER_ALL));
            node = pickBestNodeId(capabilityInfo.getNodes());
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        return node;
    }

    /*
     * There should only ever be one phone in a node set (much less w/ the correct capability), so
     * I am just grabbing the first one (which should be the only one).
     */
    private static Node pickBestNodeId(Collection<Node> nodes) {
        Node bestNode = null;

        // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node;
            }
            bestNode = node;
        }
        return bestNode;
    }

    private boolean connect() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (!mLoaded && mPhoneNodeWithApp == null)
                    mPhoneNodeWithApp = checkIfPhoneHasApp();

                return mPhoneNodeWithApp != null;
            }
        });
    }

    @WorkerThread
    private void openAppOnPhone() {
        connect();

        if (mPhoneNodeWithApp == null) {
            // Device is not connected or app is not installed on device...
            int deviceType = PhoneDeviceType.getPhoneDeviceType(WearableDataListenerService.this);
            switch (deviceType) {
                case PhoneDeviceType.DEVICE_TYPE_ANDROID:
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                            new Intent(ACTION_SHOWSTORELISTING));
                    break;
                case PhoneDeviceType.DEVICE_TYPE_IOS:
                default:
                    // Connected device is not supported
                    break;
            }
        } else {
            // Send message to device to start activity
            int result = -1;
            try {
                result = Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                        .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.StartActivityPath, new byte[0]));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }

            LocalBroadcastManager.getInstance(WearableDataListenerService.this)
                    .sendBroadcast(new Intent(ACTION_OPENONPHONE)
                            .putExtra(EXTRA_SUCCESS, result != -1));
        }
    }

    @WorkerThread
    private void sendSettingsRequest() {
        if (!connect()) {
            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                    new Intent(WearableHelper.ErrorPath));
            return;
        }

        DataItem dataItem = null;
        try {
            dataItem = Tasks.await(Wearable.getDataClient(WearableDataListenerService.this)
                    .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.SettingsPath)));

            long update_time = DataMapItem.fromDataItem(dataItem).getDataMap().getLong("update_time");
            LocalDateTime upDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(update_time), ZoneOffset.UTC);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

            if (Duration.between(upDateTime, now).abs().toMinutes() >= Settings.getRefreshInterval()) {
                dataItem = null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        if (dataItem == null) {
            // Send message to device to get settings
            try {
                Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                        .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.SettingsPath, new byte[0]));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        } else {
            // Update with data
            updateSettings(DataMapItem.fromDataItem(dataItem).getDataMap());
        }
    }

    @WorkerThread
    private void sendLocationRequest() {
        if (!connect()) {
            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                    new Intent(WearableHelper.ErrorPath));
            return;
        }

        DataItem dataItem = null;
        try {
            dataItem = Tasks.await(Wearable.getDataClient(WearableDataListenerService.this)
                    .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.LocationPath)));

            long update_time = DataMapItem.fromDataItem(dataItem).getDataMap().getLong("update_time");
            LocalDateTime upDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(update_time), ZoneOffset.UTC);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

            if (Duration.between(upDateTime, now).abs().toMinutes() >= Settings.getRefreshInterval()) {
                dataItem = null;
            }
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        if (dataItem == null) {
            // Send message to device to get settings
            try {
                Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                        .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.LocationPath, new byte[0]));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        } else {
            // Update with data
            updateLocation(DataMapItem.fromDataItem(dataItem).getDataMap());
        }
    }

    @WorkerThread
    private void sendWeatherRequest() {
        if (!connect()) {
            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                    new Intent(WearableHelper.ErrorPath));
            return;
        }

        // Send message to device to get weather
        try {
            Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                    .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.WeatherPath, new byte[0]));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    @WorkerThread
    private void sendWeatherUpdateRequest() {
        if (!connect()) {
            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                    new Intent(WearableHelper.ErrorPath));
            return;
        }

        // Send message to device to get settings
        try {
            Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                    .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.WeatherPath, new byte[]{1}));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    @WorkerThread
    private void sendSetupStatusRequest() {
        if (!connect()) {
            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                    new Intent(WearableHelper.ErrorPath));
            return;
        }

        try {
            Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                    .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.IsSetupPath, new byte[0]));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    private void updateSettings(final DataMap dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            String API = dataMap.getString(WearableSettings.KEY_API, "");
            String API_KEY = dataMap.getString(WearableSettings.KEY_APIKEY, "");
            boolean keyVerified = dataMap.getBoolean(WearableSettings.KEY_APIKEY_VERIFIED, false);
            if (!StringUtils.isNullOrWhitespace(API)) {
                Settings.setAPI(API);
                if (WeatherManager.isKeyRequired(API)) {
                    Settings.setAPIKEY(API_KEY);
                    Settings.setKeyVerified(false);
                } else {
                    Settings.setAPIKEY("");
                    Settings.setKeyVerified(false);
                }
            }

            Settings.setFollowGPS(dataMap.getBoolean(WearableSettings.KEY_FOLLOWGPS, false));

            Settings.setTempUnit(dataMap.getString(WearableSettings.KEY_UNIT, Settings.FAHRENHEIT));

            int newInterval = dataMap.getInt(WearableSettings.KEY_REFRESHINTERVAL, Settings.DEFAULTINTERVAL);
            Settings.setRefreshInterval(Math.max(Settings.DEFAULTINTERVAL, newInterval));

            // Send callback to receiver
            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                    new Intent(WearableHelper.SettingsPath));
        }
    }

    @WorkerThread
    private void updateLocation(final DataMap dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            String locationJSON = dataMap.getString("locationData", "");
            if (!StringUtils.isNullOrWhitespace(locationJSON)) {
                try (JsonReader reader = new JsonReader(new StringReader(locationJSON))) {
                    LocationData locationData = LocationData.fromJson(reader);

                    if (locationData != null) {
                        if (!locationData.equals(Settings.getHomeData())) {
                            Settings.saveHomeData(locationData);
                        }

                        // Send callback to receiver
                        LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                                new Intent(WearableHelper.LocationPath));
                    }
                } catch (IOException e) {
                    Logger.writeLine(Log.ERROR, e);
                }
            }
        }
    }

    @WorkerThread
    private void updateWeather(final byte[] data) {
        final String dataJson = new String(data, Charset.forName("UTF-8"));
        WearWeatherJSON weatherDataJSON = new AsyncTask<WearWeatherJSON>().await(new Callable<WearWeatherJSON>() {
            @Override
            public WearWeatherJSON call() throws Exception {
                return WearWeatherJSON.fromJson(new JsonReader(new StringReader(dataJson)));
            }
        });

        if (weatherDataJSON != null && weatherDataJSON.isValid()) {
            long update_time = weatherDataJSON.getUpdateTime();
            if (update_time != 0) {
                if (Settings.getHomeData() != null) {
                    LocalDateTime upDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(update_time), ZoneOffset.UTC);
                            /*
                                DateTime < 0 - This instance is earlier than value.
                                DateTime == 0 - This instance is the same as value.
                                DateTime > 0 - This instance is later than value.
                            */
                    LocalDateTime settingsUpdateTime = Settings.getUpdateTime();
                    if (settingsUpdateTime.compareTo(upDateTime) >= 0) {
                        // Send callback to receiver
                        LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                                new Intent(WearableHelper.WeatherPath));
                        return;
                    }
                }
            }

            final String weatherJSON = weatherDataJSON.getWeatherData();
            if (!StringUtils.isNullOrWhitespace(weatherJSON)) {
                try (JsonReader weatherTextReader = new JsonReader(new StringReader(weatherJSON))) {
                    Weather weatherData = new AsyncTask<Weather>().await(new Callable<Weather>() {
                        @Override
                        public Weather call() throws Exception {
                            return Weather.fromJson(weatherTextReader);
                        }
                    });
                    List<String> alerts = weatherDataJSON.getWeatherAlerts();

                    if (weatherData != null && weatherData.isValid()) {
                        if (alerts.size() > 0) {
                            weatherData.setWeatherAlerts(new ArrayList<WeatherAlert>());
                            for (String alertJSON : alerts) {
                                try (JsonReader alertTextReader = new JsonReader(new StringReader(alertJSON))) {
                                    WeatherAlert alert = new AsyncTask<WeatherAlert>().await(new Callable<WeatherAlert>() {
                                        @Override
                                        public WeatherAlert call() throws Exception {
                                            return WeatherAlert.fromJson(alertTextReader);
                                        }
                                    });

                                    if (alert != null)
                                        weatherData.getWeatherAlerts().add(alert);
                                }
                            }
                        }

                        Settings.saveWeatherAlerts(Settings.getHomeData(), weatherData.getWeatherAlerts());
                        Settings.saveWeatherData(weatherData);
                        Settings.setUpdateTime(weatherData.getUpdateTime()
                                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());

                        // Send callback to receiver
                        LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                                new Intent(WearableHelper.WeatherPath));

                        // Update complications
                        WeatherComplicationIntentService.enqueueWork(WearableDataListenerService.this,
                                new Intent(WearableDataListenerService.this, WeatherComplicationIntentService.class)
                                        .setAction(WeatherComplicationIntentService.ACTION_UPDATECOMPLICATIONS)
                                        .putExtra(WeatherComplicationIntentService.EXTRA_FORCEUPDATE, true));
                    }
                } catch (IOException e) {
                    Logger.writeLine(Log.ERROR, e);
                }
            }
        }
    }
}
