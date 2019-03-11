package com.thewizrd.simpleweather.wearable;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.phone.PhoneDeviceType;
import android.util.Log;
import android.widget.Toast;

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
import com.thewizrd.shared_resources.helpers.WearConnectionStatus;
import com.thewizrd.shared_resources.helpers.WearWeatherJSON;
import com.thewizrd.shared_resources.helpers.WearableDataSync;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

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
    private Handler mMainHandler;

    public static void setAcceptDataUpdates(boolean value) {
        acceptDataUpdates = value;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMainHandler = new Handler(Looper.getMainLooper());

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

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.writeLine(Log.ERROR, e, "SimpleWeather: %s: UncaughtException", TAG);

                if (oldHandler != null) {
                    oldHandler.uncaughtException(t, e);
                } else {
                    System.exit(2);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        mLoaded = false;

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
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        updateSettings(dataMap);
                    } else if (item.getUri().getPath().compareTo(WearableHelper.LocationPath) == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        updateLocation(dataMap);
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
            byte[] data = messageEvent.getData();
            updateWeather(data);
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
        if (ACTION_OPENONPHONE.equals(intent.getAction())) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    openAppOnPhone();
                }
            });
            return START_NOT_STICKY;
        } else if (ACTION_UPDATECONNECTIONSTATUS.equals(intent.getAction())) {
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(ACTION_UPDATECONNECTIONSTATUS)
                            .putExtra(EXTRA_CONNECTIONSTATUS, mConnectionStatus.getValue()));
            return START_NOT_STICKY;
        } else if (ACTION_REQUESTSETTINGSUPDATE.equals(intent.getAction())) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    sendSettingsRequest();
                }
            });
            return START_NOT_STICKY;
        } else if (ACTION_REQUESTLOCATIONUPDATE.equals(intent.getAction())) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    sendLocationRequest();
                }
            });
            return START_NOT_STICKY;
        } else if (ACTION_REQUESTWEATHERUPDATE.equals(intent.getAction())) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    boolean forceUpdate = intent.getBooleanExtra(EXTRA_FORCEUPDATE, false);
                    if (!forceUpdate)
                        sendWeatherRequest();
                    else
                        sendWeatherUpdateRequest();
                }
            });
            return START_NOT_STICKY;
        } else if (ACTION_REQUESTSETUPSTATUS.equals(intent.getAction())) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    sendSetupStatusRequest();
                }
            });
            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId);
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
        Node bestNodeid = null;

        // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
        for (Node node : nodes) {
            bestNodeid = node;
        }

        return bestNodeid;
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

    private void openAppOnPhone() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                connect();

                if (mPhoneNodeWithApp == null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WearableDataListenerService.this, "Device is not connected or app is not installed on device...", Toast.LENGTH_SHORT).show();
                        }
                    });

                    int deviceType = PhoneDeviceType.getPhoneDeviceType(WearableDataListenerService.this);
                    switch (deviceType) {
                        case PhoneDeviceType.DEVICE_TYPE_ANDROID:
                            LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                                    new Intent(ACTION_SHOWSTORELISTING));
                            break;
                        case PhoneDeviceType.DEVICE_TYPE_IOS:
                        default:
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WearableDataListenerService.this, "Connected device is not supported", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                    }
                } else {
                    // Send message to device to start activity
                    int result = Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                            .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.StartActivityPath, new byte[0]));

                    LocalBroadcastManager.getInstance(WearableDataListenerService.this)
                            .sendBroadcast(new Intent(ACTION_OPENONPHONE)
                                    .putExtra(EXTRA_SUCCESS, result != -1));
                }
                return null;
            }
        });
    }

    private void sendSettingsRequest() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (!connect()) {
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                            new Intent(WearableHelper.ErrorPath));
                    return null;
                }

                DataItem dataItem = Tasks.await(Wearable.getDataClient(WearableDataListenerService.this)
                        .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.SettingsPath)));

                if (dataItem == null) {
                    // Send message to device to get settings
                    int result = Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                            .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.SettingsPath, new byte[0]));
                } else {
                    // Update with data
                    updateSettings(DataMapItem.fromDataItem(dataItem).getDataMap());
                }
                return null;
            }
        });
    }

    private void sendLocationRequest() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (!connect()) {
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                            new Intent(WearableHelper.ErrorPath));
                    return null;
                }

                DataItem dataItem = Tasks.await(Wearable.getDataClient(WearableDataListenerService.this)
                        .getDataItem(WearableHelper.getWearDataUri(mPhoneNodeWithApp.getId(), WearableHelper.LocationPath)));

                if (dataItem == null) {
                    // Send message to device to get settings
                    int result = Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                            .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.LocationPath, new byte[0]));
                } else {
                    // Update with data
                    updateLocation(DataMapItem.fromDataItem(dataItem).getDataMap());
                }
                return null;
            }
        });
    }

    private void sendWeatherRequest() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (!connect()) {
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                            new Intent(WearableHelper.ErrorPath));
                    return null;
                }

                // Send message to device to get weather
                Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                        .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.WeatherPath, new byte[0]));
                return null;
            }
        });
    }

    private void sendWeatherUpdateRequest() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (!connect()) {
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                            new Intent(WearableHelper.ErrorPath));
                    return null;
                }

                // Send message to device to get settings
                Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                        .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.WeatherPath, new byte[]{1}));
                return null;
            }
        });
    }

    private void sendSetupStatusRequest() {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (!connect()) {
                    LocalBroadcastManager.getInstance(WearableDataListenerService.this).sendBroadcast(
                            new Intent(WearableHelper.ErrorPath));
                    return null;
                }

                Tasks.await(Wearable.getMessageClient(WearableDataListenerService.this)
                        .sendMessage(mPhoneNodeWithApp.getId(), WearableHelper.IsSetupPath, new byte[0]));
                return null;
            }
        });
    }

    private void updateSettings(DataMap dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            String API = dataMap.getString("API", "");
            String API_KEY = dataMap.getString("API_KEY", "");
            boolean keyVerified = dataMap.getBoolean("KeyVerified", false);
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

            Settings.setFollowGPS(dataMap.getBoolean("FollowGPS", false));

            // Send callback to receiver
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    new Intent(WearableHelper.SettingsPath));
        }
    }

    private void updateLocation(DataMap dataMap) {
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
                        LocalBroadcastManager.getInstance(this).sendBroadcast(
                                new Intent(WearableHelper.LocationPath));
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    private void updateWeather(final byte[] data) {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String dataJson = new String(data, Charset.forName("UTF-8"));
                WearWeatherJSON weatherDataJSON = WearWeatherJSON.fromJson(new JsonReader(new StringReader(dataJson)));

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
                                return null;
                            }
                        }
                    }

                    String weatherJSON = weatherDataJSON.getWeatherData();
                    if (!StringUtils.isNullOrWhitespace(weatherJSON)) {
                        try (JsonReader weatherTextReader = new JsonReader(new StringReader(weatherJSON))) {
                            Weather weatherData = Weather.fromJson(weatherTextReader);
                            List<String> alerts = weatherDataJSON.getWeatherAlerts();

                            if (weatherData != null && weatherData.isValid()) {
                                if (alerts.size() > 0) {
                                    weatherData.setWeatherAlerts(new ArrayList<WeatherAlert>());
                                    for (String alertJSON : alerts) {
                                        try (JsonReader alertTextReader = new JsonReader(new StringReader(alertJSON))) {
                                            WeatherAlert alert = WeatherAlert.fromJson(alertTextReader);

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
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }
        });
    }
}
