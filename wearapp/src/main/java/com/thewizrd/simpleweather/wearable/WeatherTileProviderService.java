package com.thewizrd.simpleweather.wearable;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.clockwork.tiles.TileData;
import com.google.android.clockwork.tiles.TileProviderService;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.LaunchActivity;
import com.thewizrd.simpleweather.R;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WeatherTileProviderService extends TileProviderService {
    private static final String TAG = "WeatherTileProviderService";

    private Context mContext;
    private int id = -1;

    private static final int FORECAST_LENGTH = 4;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
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
        AsyncTask.await(new Callable<Void>() {
            @Override
            public Void call() {
                Weather weather = AsyncTask.await(new Callable<Weather>() {
                    @Override
                    public Weather call() {
                        WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());
                        WeatherRequest.Builder request = new WeatherRequest.Builder();
                        if (Settings.getDataSync() == WearableDataSync.OFF) {
                            request.forceRefresh(false);
                        } else {
                            request.forceLoadSavedData();
                        }
                        try {
                            return Tasks.await(wloader.loadWeatherData(request.build()));
                        } catch (ExecutionException | InterruptedException e) {
                            return null;
                        }
                    }
                });

                RemoteViews updateViews = buildUpdate(weather);

                if (updateViews != null) {
                    TileData tileData = new TileData.Builder()
                            .setRemoteViews(updateViews)
                            .build();

                    sendData(id, tileData);
                }

                return null;
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

        // Details
        DetailItemViewModel chanceModel = null;
        DetailItemViewModel windModel = null;

        for (DetailItemViewModel input : viewModel.getWeatherDetails()) {
            if (input != null && (input.getDetailsType() == WeatherDetailsType.POPCHANCE || input.getDetailsType() == WeatherDetailsType.POPCLOUDINESS)) {
                chanceModel = input;
            } else if (input != null && input.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                windModel = input;
            }

            if (chanceModel != null && windModel != null) {
                break;
            }
        }

        if (chanceModel != null) {
            updateViews.setImageViewBitmap(R.id.weather_popicon,
                    ImageUtils.weatherIconToBitmap(this, chanceModel.getIcon(), 72, false)
            );
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.getValue());
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE);
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE);
        }

        if (windModel != null) {
            if (windModel.getIconRotation() != 0) {
                updateViews.setImageViewBitmap(R.id.weather_windicon,
                        ImageUtils.rotateBitmap(ImageUtils.bitmapFromDrawable(this, R.drawable.direction_up), windModel.getIconRotation())
                );
            } else {
                updateViews.setImageViewResource(R.id.weather_windicon, R.drawable.direction_up);
            }
            String speed = TextUtils.isEmpty(windModel.getValue()) ? "" : windModel.getValue().toString();
            speed = speed.split(",")[0];
            updateViews.setTextViewText(R.id.weather_windspeed, speed);
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE);
        } else {
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE);
        }

        updateViews.setViewVisibility(R.id.extra_layout, chanceModel != null || windModel != null ? View.VISIBLE : View.GONE);

        // Build forecast
        updateViews.removeAllViews(R.id.forecast_layout);

        RemoteViews forecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.tile_forecast_layout_container);
        RemoteViews hrForecastPanel = null;

        List<ForecastItemViewModel> forecasts = getForecasts();
        List<HourlyForecastItemViewModel> hrforecasts = getHourlyForecasts();

        if (hrforecasts.size() > 0) {
            hrForecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.tile_forecast_layout_container);
        }

        for (int i = 0; i < Math.min(FORECAST_LENGTH, forecasts.size()); i++) {
            ForecastItemViewModel forecast = forecasts.get(i);
            addForecastItem(forecastPanel, forecast);

            if (hrForecastPanel != null && i < hrforecasts.size()) {
                addForecastItem(hrForecastPanel, hrforecasts.get(i));
            }
        }

        updateViews.setViewVisibility(R.id.forecast_layout, Math.min(forecasts.size(), hrforecasts.size()) <= 0 ? View.GONE : View.VISIBLE);

        updateViews.addView(R.id.forecast_layout, forecastPanel);
        if (hrForecastPanel != null) {
            updateViews.addView(R.id.forecast_layout, hrForecastPanel);
        }

        return updateViews;
    }

    private List<ForecastItemViewModel> getForecasts() {
        LocationData locationData = Settings.getHomeData();

        if (locationData != null && locationData.isValid()) {
            Forecasts forecasts = Settings.getWeatherForecastData(locationData.getQuery());

            if (forecasts != null && forecasts.getForecast() != null && !forecasts.getForecast().isEmpty()) {
                final int size = Math.min(FORECAST_LENGTH, forecasts.getForecast().size());
                List<ForecastItemViewModel> fcasts = new ArrayList<>(size);

                for (int i = 0; i < size; i++) {
                    fcasts.add(new ForecastItemViewModel(forecasts.getForecast().get(i)));
                }

                return fcasts;
            }
        }

        return Collections.emptyList();
    }

    private List<HourlyForecastItemViewModel> getHourlyForecasts() {
        LocationData locationData = Settings.getHomeData();

        if (locationData != null && locationData.isValid()) {
            ZonedDateTime now = ZonedDateTime.now(locationData.getTzOffset());
            List<HourlyForecast> forecasts = Settings.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(locationData.getQuery(), FORECAST_LENGTH, now);

            if (forecasts != null && !forecasts.isEmpty()) {
                List<HourlyForecastItemViewModel> fcasts = new ArrayList<>();

                for (HourlyForecast fcast : forecasts) {
                    fcasts.add(new HourlyForecastItemViewModel(fcast));
                }

                return fcasts;
            }
        }

        return Collections.emptyList();
    }

    private void addForecastItem(RemoteViews forecastPanel, BaseForecastItemViewModel forecast) {
        RemoteViews forecastItem = new RemoteViews(mContext.getPackageName(), R.layout.tile_forecast_panel);

        if (forecast instanceof ForecastItemViewModel) {
            forecastItem.setTextViewText(R.id.forecast_date, StringUtils.removeDigitChars(forecast.getShortDate()));
        } else {
            forecastItem.setTextViewText(R.id.forecast_date, forecast.getShortDate());
        }
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
}