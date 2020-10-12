package com.thewizrd.simpleweather.wearable;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import androidx.core.util.ObjectsCompat;

import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.wearable.WearableDataSync;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.LaunchActivity;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WeatherComplicationService extends ComplicationProviderService {
    private static final String TAG = "WeatherComplicationService";

    private Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    @Override
    public void onComplicationActivated(int complicationId, int type, ComplicationManager manager) {
        super.onComplicationActivated(complicationId, type, manager);

        // Request complication update
        WeatherComplicationWorker.enqueueAction(mContext,
                new Intent(WeatherComplicationWorker.ACTION_UPDATECOMPLICATION)
                        .putExtra(WeatherComplicationWorker.EXTRA_COMPLICATIONID, complicationId));
    }

    @Override
    public void onComplicationDeactivated(int complicationId) {
        super.onComplicationDeactivated(complicationId);
    }

    @Override
    public void onComplicationUpdate(final int complicationId, final int type, final ComplicationManager manager) {
        ComplicationData complicationData = null;

        if (Settings.isWeatherLoaded()) {
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

            complicationData = buildUpdate(type, weather);
        }

        if (complicationData != null) {
            manager.updateComplicationData(complicationId, complicationData);
            Logger.writeLine(Log.DEBUG, "%s: Complication %d updated", TAG, complicationId);
        } else {
            // If no data is sent, we still need to inform the ComplicationManager, so
            // the update job can finish and the wake lock isn't held any longer.
            manager.noUpdateRequired(complicationId);
            Logger.writeLine(Log.DEBUG, "%s: Complication %d no update required", TAG, complicationId);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean result = super.onUnbind(intent);
        Logger.writeLine(Log.DEBUG, "%s: Service unbound", TAG);
        return result;
    }

    private PendingIntent getTapIntent(Context context) {
        Intent onClickIntent = new Intent(context.getApplicationContext(), LaunchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, onClickIntent, 0);
    }

    private ComplicationData buildUpdate(int dataType, Weather weather) {
        if ((weather == null || !weather.isValid()) || (dataType != ComplicationData.TYPE_SHORT_TEXT && dataType != ComplicationData.TYPE_LONG_TEXT)) {
            return null;
        } else {
            // Temperature
            String currTemp;
            if (weather.getCondition().getTempF() != null && !ObjectsCompat.equals(weather.getCondition().getTempF(), weather.getCondition().getTempC())) {
                int temp = Settings.isFahrenheit() ? Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC());
                currTemp = String.format(LocaleUtils.getLocale(), "%d", temp);
            } else {
                currTemp = "--";
            }

            String tempUnit = Settings.getTempUnit();
            String temp = String.format(LocaleUtils.getLocale(), "%s°%s", currTemp, tempUnit);
            // Weather Icon
            int weatherIcon = WeatherUtils.getWeatherIconResource(weather.getCondition().getIcon());
            // Condition text
            String condition = weather.getCondition().getWeather();

            ComplicationData.Builder builder = new ComplicationData.Builder(dataType);
            if (dataType == ComplicationData.TYPE_SHORT_TEXT) {
                builder.setShortText(ComplicationText.plainText(temp));
            } else if (dataType == ComplicationData.TYPE_LONG_TEXT) {
                builder.setLongText(ComplicationText.plainText(String.format("%s - %s", condition, temp)));
            }

            builder.setIcon(Icon.createWithResource(this, weatherIcon))
                    .setTapAction(getTapIntent(this));

            return builder.build();
        }
    }
}
