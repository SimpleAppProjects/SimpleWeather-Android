package com.thewizrd.simpleweather.wearable;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.wearable.WearableSettings;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;

import org.threeten.bp.Clock;
import org.threeten.bp.Instant;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class WearableWorker extends Worker {
    private static String TAG = "WearableWorker";

    // Actions
    private static final String KEY_ACTION = "action";
    private static final String KEY_URGENTREQUEST = "urgent";
    public static final String ACTION_SENDUPDATE = "SimpleWeather.Droid.action.SEND_UPDATE";
    public static final String ACTION_SENDSETTINGSUPDATE = "SimpleWeather.Droid.action.SEND_SETTINGS_UPDATE";
    public static final String ACTION_SENDLOCATIONUPDATE = "SimpleWeather.Droid.action.SEND_LOCATION_UPDATE";
    public static final String ACTION_SENDWEATHERUPDATE = "SimpleWeather.Droid.action.SEND_WEATHER_UPDATE";

    private final Context mContext;

    public WearableWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction) {
        enqueueAction(context, intentAction, true);
    }

    public static void enqueueAction(@NonNull Context context, @NonNull String intentAction, boolean urgent) {
        context = context.getApplicationContext();

        switch (intentAction) {
            case ACTION_SENDUPDATE:
            case ACTION_SENDSETTINGSUPDATE:
            case ACTION_SENDLOCATIONUPDATE:
            case ACTION_SENDWEATHERUPDATE:
                startWork(context, intentAction, urgent);
                break;
        }
    }

    private static void startWork(@NonNull Context context, @NonNull String intentAction, boolean urgent) {
        context = context.getApplicationContext();

        Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG);

        OneTimeWorkRequest updateRequest = new OneTimeWorkRequest.Builder(WearableWorker.class)
                .setInputData(
                        new Data.Builder()
                                .putString(KEY_ACTION, intentAction)
                                .putBoolean(KEY_URGENTREQUEST, urgent)
                                .build()
                )
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(String.format(Locale.ROOT, "%s:%s_oneTime", TAG, intentAction), ExistingWorkPolicy.REPLACE, updateRequest);

        Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG);

        final String intentAction = getInputData().getString(KEY_ACTION);
        final boolean urgent = getInputData().getBoolean(KEY_URGENTREQUEST, true);

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction);

        // Don't send anything unless we're setup
        if (!Settings.isWeatherLoaded())
            return Result.success();

        Collection<Node> mWearNodesWithApp = findWearDevicesWithApp();
        if (!mWearNodesWithApp.isEmpty()) {
            if (ACTION_SENDUPDATE.equals(intentAction)) {
                createSettingsDataRequest(urgent);
                createLocationDataRequest(urgent);
                createWeatherDataRequest(urgent);
            } else if (ACTION_SENDSETTINGSUPDATE.equals(intentAction)) {
                createSettingsDataRequest(urgent);
            } else if (ACTION_SENDLOCATIONUPDATE.equals(intentAction)) {
                createLocationDataRequest(urgent);
            } else if (ACTION_SENDWEATHERUPDATE.equals(intentAction)) {
                createWeatherDataRequest(urgent);
            }
        }

        return Result.success();
    }

    /* Wearable Functions */
    private Collection<Node> findWearDevicesWithApp() {
        CapabilityInfo capabilityInfo = null;

        try {
            capabilityInfo = Tasks.await(Wearable.getCapabilityClient(mContext)
                    .getCapability(WearableHelper.CAPABILITY_WEAR_APP, CapabilityClient.FILTER_ALL));
        } catch (ExecutionException | InterruptedException e) {
            if (e.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) e.getCause();
                // Ignore this error
                if (apiException.getStatusCode() != WearableStatusCodes.API_NOT_CONNECTED) {
                    Logger.writeLine(Log.ERROR, e);
                }
            } else {
                Logger.writeLine(Log.ERROR, e);
            }
        }

        if (capabilityInfo != null) {
            return capabilityInfo.getNodes();
        }

        return Collections.emptySet();
    }

    private void createSettingsDataRequest(boolean urgent) {
        PutDataMapRequest mapRequest = PutDataMapRequest.create(WearableHelper.SettingsPath);
        mapRequest.getDataMap().putString(WearableSettings.KEY_API, Settings.getAPI());
        mapRequest.getDataMap().putString(WearableSettings.KEY_APIKEY, Settings.getAPIKEY());
        mapRequest.getDataMap().putBoolean(WearableSettings.KEY_APIKEY_VERIFIED, Settings.isKeyVerified());
        mapRequest.getDataMap().putBoolean(WearableSettings.KEY_FOLLOWGPS, Settings.useFollowGPS());
        mapRequest.getDataMap().putString(WearableSettings.KEY_TEMPUNIT, Settings.getTemperatureUnit());

        DataMap unitMap = new DataMap();
        unitMap.putString(WearableSettings.KEY_TEMPUNIT, Settings.getTemperatureUnit());
        unitMap.putString(WearableSettings.KEY_SPEEDUNIT, Settings.getSpeedUnit());
        unitMap.putString(WearableSettings.KEY_DISTANCEUNIT, Settings.getDistanceUnit());
        unitMap.putString(WearableSettings.KEY_PRESSUREUNIT, Settings.getPressureUnit());
        unitMap.putString(WearableSettings.KEY_PRECIPITATIONUNIT, Settings.getPrecipitationUnit());
        mapRequest.getDataMap().putDataMap(WearableSettings.KEY_UNITS, unitMap);

        mapRequest.getDataMap().putString(WearableSettings.KEY_LANGUAGE, LocaleUtils.getLocaleCode());
        mapRequest.getDataMap().putLong(WearableSettings.KEY_UPDATETIME, Instant.now(Clock.systemUTC()).toEpochMilli());
        PutDataRequest request = mapRequest.asPutDataRequest();
        if (urgent) request.setUrgent();
        try {
            DataClient client = Wearable.getDataClient(mContext);
            Tasks.await(client.deleteDataItems(mapRequest.getUri()));
            Tasks.await(client.putDataItem(request));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.INFO, "%s: createSettingsDataRequest(): urgent: %s", TAG, Boolean.toString(urgent));
    }

    private void createLocationDataRequest(boolean urgent) {
        PutDataMapRequest mapRequest = PutDataMapRequest.create(WearableHelper.LocationPath);
        LocationData homeData = Settings.getHomeData();
        mapRequest.getDataMap().putString(WearableSettings.KEY_LOCATIONDATA, JSONParser.serializer(homeData, LocationData.class));
        mapRequest.getDataMap().putLong(WearableSettings.KEY_UPDATETIME, Instant.now(Clock.systemUTC()).toEpochMilli());
        PutDataRequest request = mapRequest.asPutDataRequest();
        if (urgent) request.setUrgent();
        try {
            DataClient client = Wearable.getDataClient(mContext);
            Tasks.await(client.deleteDataItems(mapRequest.getUri()));
            Tasks.await(client.putDataItem(request));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.INFO, "%s: createLocationDataRequest(): urgent: %s", TAG, Boolean.toString(urgent));
    }

    private void createWeatherDataRequest(boolean urgent) {
        PutDataMapRequest mapRequest = PutDataMapRequest.create(WearableHelper.WeatherPath);
        LocationData homeData = Settings.getHomeData();
        Weather weatherData = Settings.getWeatherData(homeData.getQuery());
        Collection<WeatherAlert> alertData = Settings.getWeatherAlertData(homeData.getQuery());
        Forecasts forecasts = Settings.getWeatherForecastData(homeData.getQuery());
        List<HourlyForecast> hrForecasts = Settings.getHourlyWeatherForecastData(homeData.getQuery());

        if (weatherData != null) {
            weatherData.setForecast(forecasts.getForecast());
            weatherData.setHrForecast(hrForecasts);
            weatherData.setTxtForecast(forecasts.getTxtForecast());
            weatherData.setWeatherAlerts(alertData);
            mapRequest.getDataMap().putAsset(WearableSettings.KEY_WEATHERDATA, Asset.createFromBytes(JSONParser.serializer(weatherData, Weather.class).getBytes(Charset.forName("UTF-8"))));
        }

        mapRequest.getDataMap().putLong(WearableSettings.KEY_UPDATETIME, Instant.now(Clock.systemUTC()).toEpochMilli());

        PutDataRequest request = mapRequest.asPutDataRequest();
        if (urgent) request.setUrgent();
        try {
            DataClient client = Wearable.getDataClient(mContext);
            Tasks.await(client.deleteDataItems(mapRequest.getUri()));
            Tasks.await(client.putDataItem(request));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.INFO, "%s: createWeatherDataRequest(): urgent: %s", TAG, Boolean.toString(urgent));
    }
}
