package com.thewizrd.simpleweather.wearable;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.clockwork.tiles.TileData;
import com.google.android.clockwork.tiles.TileProviderService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.wearable.WearableHelper;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.LaunchActivity;
import com.thewizrd.simpleweather.R;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class WeatherTileProviderService extends TileProviderService {
    private static final String TAG = "WeatherTileProviderService";

    private Context mContext;
    private WeatherManager wm;
    private FusedLocationProviderClient mFusedLocationClient;
    private int id = -1;

    private static LocalDateTime updateTime = DateTimeUtils.getLocalDateTimeMIN();

    public static LocalDateTime getUpdateTime() {
        return updateTime;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        wm = WeatherManager.getInstance();

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(this);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroying service...");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean result = super.onUnbind(intent);
        Log.d(TAG, "Service unbound");
        return result;
    }

    @Override
    public void onTileUpdate(int tileId) {
        Log.d(TAG, "onTileUpdate called with: tileId = " + tileId);

        if (!isIdForDummyData(tileId)) {
            id = tileId;
            sendRemoteViews();
        }
    }

    @Override
    public void onTileFocus(int tileId) {
        super.onTileFocus(tileId);

        Log.d(TAG, "onTileFocus called with: tileId = " + tileId);

        if (!isIdForDummyData(tileId)) {
            id = tileId;
        }
    }

    @Override
    public void onTileBlur(int tileId) {
        super.onTileBlur(tileId);

        Log.d(TAG, "onTileBlur called with: tileId = " + tileId);

        if (!isIdForDummyData(tileId)) {
            id = tileId;
        }
    }

    private void sendRemoteViews() {
        Log.d(TAG, "sendRemoteViews");
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                RemoteViews updateViews = buildUpdate(getWeather());

                if (updateViews != null) {
                    TileData tileData = new TileData.Builder()
                            .setRemoteViews(updateViews)
                            .build();

                    updateTime = LocalDateTime.now(ZoneOffset.UTC);
                    sendData(id, tileData);
                    // Reset alarm
                    WeatherTileIntentService.enqueueWork(mContext, new Intent(mContext, WeatherTileIntentService.class)
                            .setAction(WeatherTileIntentService.ACTION_UPDATEALARM));
                }
            }
        });
    }

    private RemoteViews buildUpdate(final Weather weather) {
        if (weather == null || !weather.isValid())
            return null;

        RemoteViews updateViews = new RemoteViews(mContext.getPackageName(), R.layout.tile_layout_weather);
        WeatherNowViewModel viewModel = new WeatherNowViewModel(weather);

        updateViews.setOnClickPendingIntent(R.id.tile, getTapIntent(mContext));

        updateViews.setImageViewBitmap(R.id.condition_temp,
                ImageUtils.weatherIconToBitmap(mContext, viewModel.getCurTemp(), 72, Colors.WHITE));
        updateViews.setImageViewBitmap(R.id.weather_icon,
                ImageUtils.weatherIconToBitmap(mContext, viewModel.getWeatherIcon(), 72, Colors.WHITE));
        updateViews.setTextViewText(R.id.weather_condition, viewModel.getCurCondition());

        // Build forecast
        final int FORECAST_LENGTH = 3;
        updateViews.removeAllViews(R.id.forecast_layout);

        RemoteViews forecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.tile_forecast_layout_container);
        RemoteViews hrForecastPanel = null;

        if (viewModel.getHourlyForecasts().size() > 0) {
            hrForecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.tile_forecast_layout_container);
        }

        for (int i = 0; i < FORECAST_LENGTH; i++) {
            ForecastItemViewModel forecast = viewModel.getForecasts().get(i);
            addForecastItem(forecastPanel, forecast);

            if (hrForecastPanel != null) {
                addForecastItem(hrForecastPanel, viewModel.getHourlyForecasts().get(i));
            }
        }

        updateViews.addView(R.id.forecast_layout, forecastPanel);
        if (hrForecastPanel != null) {
            updateViews.addView(R.id.forecast_layout, hrForecastPanel);
        }

        return updateViews;
    }

    private void addForecastItem(RemoteViews forecastPanel, BaseForecastItemViewModel forecast) {
        RemoteViews forecastItem = new RemoteViews(mContext.getPackageName(), R.layout.tile_forecast_panel);

        forecastItem.setTextViewText(R.id.forecast_date, forecast.getShortDate());
        forecastItem.setTextViewText(R.id.forecast_hi, forecast.getHiTemp());
        if (forecast instanceof ForecastItemViewModel) {
            forecastItem.setTextViewText(R.id.forecast_lo, ((ForecastItemViewModel) forecast).getLoTemp());
        }

        forecastItem.setImageViewBitmap(R.id.forecast_icon,
                ImageUtils.weatherIconToBitmap(this, forecast.getWeatherIcon(), 72, Colors.WHITE));

        if (forecast instanceof HourlyForecastItemViewModel) {
            forecastItem.setViewVisibility(R.id.forecast_lo, View.GONE);
        }

        forecastPanel.addView(R.id.forecast_container, forecastItem);
    }

    private PendingIntent getTapIntent(Context context) {
        Intent onClickIntent = new Intent(context.getApplicationContext(), LaunchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, onClickIntent, 0);
    }

    private Weather getWeather() {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() {
                Weather weather = null;

                try {
                    if (Settings.getDataSync() == WearableDataSync.OFF && Settings.useFollowGPS())
                        updateLocation();

                    WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());

                    WeatherRequest.Builder request = new WeatherRequest.Builder().loadForecasts();
                    if (Settings.getDataSync() == WearableDataSync.OFF) {
                        request.forceRefresh(false);
                    } else {
                        request.forceLoadSavedData();
                    }

                    weather = Tasks.await(wloader.loadWeatherData(request.build()));

                    if (weather != null && Settings.getDataSync() != WearableDataSync.OFF) {
                        int ttl = Settings.DEFAULTINTERVAL;
                        try {
                            ttl = Integer.parseInt(weather.getTtl());
                        } catch (NumberFormatException ex) {
                            Logger.writeLine(Log.ERROR, ex);
                        } finally {
                            ttl = Math.max(ttl, Settings.getRefreshInterval());
                        }

                        // Check file age
                        ZonedDateTime updateTime = Settings.getUpdateTime().atZone(ZoneOffset.UTC);

                        Duration span = Duration.between(ZonedDateTime.now(), updateTime).abs();
                        if (span.toMinutes() > ttl) {
                            WearableDataListenerService.enqueueWork(mContext, new Intent(mContext, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_REQUESTWEATHERUPDATE));
                        }
                    }
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex, "%s: GetWeather error", TAG);
                    return null;
                }

                return weather;
            }
        });
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                boolean locationChanged = false;

                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }

                    Location location = null;

                    LocationManager locMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

                    if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                        boolean disable = true;

                        PowerManager pwrMan = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                        // Some devices (ex. Pixel) disable location services if device is in Battery Saver mode
                        // and the screen is off; don't disable this feature for this case
                        boolean lowPwrMode = pwrMan != null && (pwrMan.isPowerSaveMode() && !pwrMan.isInteractive());

                        // Disable if we're unable to access location
                        // and we're not in Low Power (Battery Saver) mode
                        disable = !lowPwrMode;

                        if (disable) {
                            // Disable GPS feature if location is not enabled
                            Settings.setFollowGPS(false);
                            Logger.writeLine(Log.INFO, "%s: Disabled location feature", TAG);
                        }

                        return false;
                    }

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 10, TimeUnit.SECONDS);
                                } catch (Exception e) {
                                    Logger.writeLine(Log.ERROR, e);
                                }
                                return result;
                            }
                        });
                    } else {
                        boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                        boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                        if (isGPSEnabled || isNetEnabled) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);
                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);
                        }
                    }

                    if (location != null) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
                        }

                        LocationQueryViewModel query_vm = null;

                        // TODO: task it
                        try {
                            query_vm = wm.getLocation(location);
                        } catch (WeatherException e) {
                            // Stop since there is no valid query
                            Logger.writeLine(Log.ERROR, e);
                            return false;
                        }

                        if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                            query_vm = new LocationQueryViewModel();
                        // END TASK IT

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        locationChanged = true;
                    }
                }

                return locationChanged;
            }
        });
    }
}