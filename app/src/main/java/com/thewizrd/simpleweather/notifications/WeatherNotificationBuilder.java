package com.thewizrd.simpleweather.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.icons.WeatherIconsProvider;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.main.MainActivity;

public class WeatherNotificationBuilder {
    private static final String TAG = "WeatherNotificationBuilder";

    static Notification updateNotification(String notChannelID, @NonNull final WeatherNowViewModel viewModel) {
        final Context context = App.getInstance().getAppContext();
        final SettingsManager settingsManager = App.getInstance().getSettingsManager();
        final WeatherIconsManager wim = WeatherIconsManager.getInstance();

        // Build update
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.weather_notification_layout);

        String condition = viewModel.getCurCondition();
        String hiTemp = StringUtils.removeNonDigitChars(viewModel.getHiTemp());
        String loTemp = StringUtils.removeNonDigitChars(viewModel.getLoTemp());
        String temp = viewModel.getCurTemp() != null ?
                StringUtils.removeNonDigitChars(viewModel.getCurTemp()) : WeatherIcons.PLACEHOLDER;

        // Weather icon
        updateViews.setImageViewResource(R.id.weather_icon, wim.getWeatherIconResource(viewModel.getWeatherIcon()));

        // Location Name
        updateViews.setTextViewText(R.id.location_name, viewModel.getLocation());

        // Condition text
        updateViews.setTextViewText(R.id.condition_weather,
                String.format("%s°%s - %s", !StringUtils.isNullOrWhitespace(temp) ? temp : WeatherIcons.PLACEHOLDER, viewModel.getTempUnit(), condition));

        // Details
        updateViews.setTextViewText(R.id.condition_hi, StringUtils.containsDigits(hiTemp) ? (hiTemp + "°") : WeatherIcons.PLACEHOLDER);
        updateViews.setTextViewText(R.id.condition_lo, StringUtils.containsDigits(loTemp) ? (loTemp + "°") : WeatherIcons.PLACEHOLDER);
        updateViews.setViewVisibility(R.id.condition_hilo_layout, viewModel.isShowHiLo() ? View.VISIBLE : View.GONE);

        // Get extras
        DetailItemViewModel chanceModel = null, windModel = null, feelsLikeModel = null, humidityModel = null, popRainModel = null, popSnowModel = null;
        for (DetailItemViewModel input : viewModel.getWeatherDetails()) {
            if (input.getDetailsType() == WeatherDetailsType.POPCHANCE) {
                chanceModel = input;
            } else if (chanceModel == null && input.getDetailsType() == WeatherDetailsType.POPCLOUDINESS) {
                chanceModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                windModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.FEELSLIKE) {
                feelsLikeModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.HUMIDITY) {
                humidityModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.POPRAIN) {
                popRainModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.POPSNOW) {
                popSnowModel = input;
            }

            if (chanceModel != null && windModel != null && feelsLikeModel != null && humidityModel != null && popRainModel != null && popSnowModel != null) {
                break;
            }
        }

        // Extras
        if (chanceModel != null) {
            updateViews.setImageViewResource(R.id.weather_popicon, chanceModel.getIcon());
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.getValue());
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE);
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE);
        }
        if (windModel != null) {
            final int windIconResId = wim.getWeatherIconResource(WeatherIcons.WIND_DIRECTION);
            if (windModel.getIconRotation() != 0) {
                updateViews.setImageViewBitmap(R.id.weather_windicon,
                        ImageUtils.rotateBitmap(ImageUtils.bitmapFromDrawable(context, windIconResId), windModel.getIconRotation())
                );
            } else {
                updateViews.setImageViewResource(R.id.weather_windicon, windIconResId);
            }
            String speed = TextUtils.isEmpty(windModel.getValue()) ? "" : windModel.getValue().toString();
            speed = speed.split(",")[0];
            updateViews.setTextViewText(R.id.weather_windspeed, speed);
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE);
        } else {
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE);
        }
        updateViews.setViewVisibility(R.id.extra_layout, chanceModel != null || windModel != null ? View.VISIBLE : View.GONE);

        // Extras 2
        RemoteViews bigUpdateViews;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bigUpdateViews = new RemoteViews(updateViews);
        } else {
            bigUpdateViews = updateViews.clone();
        }
        if (feelsLikeModel != null) {
            bigUpdateViews.setTextViewText(R.id.feelslike_label, context.getString(R.string.label_feelslike));
            bigUpdateViews.setTextViewText(R.id.feelslike_temp, feelsLikeModel.getValue() + viewModel.getTempUnit());
            bigUpdateViews.setViewVisibility(R.id.feelslike_layout, View.VISIBLE);
        }
        if (humidityModel != null) {
            bigUpdateViews.setImageViewResource(R.id.humidity_icon, humidityModel.getIcon());
            bigUpdateViews.setTextViewText(R.id.humidity, humidityModel.getValue());
            bigUpdateViews.setViewVisibility(R.id.humidity_layout, View.VISIBLE);
        }
        if (popRainModel != null) {
            bigUpdateViews.setImageViewResource(R.id.precip_rain_icon, popRainModel.getIcon());
            bigUpdateViews.setTextViewText(R.id.precip_rain, popRainModel.getValue());
            bigUpdateViews.setViewVisibility(R.id.precip_rain_layout, View.VISIBLE);
        }
        if (popSnowModel != null) {
            bigUpdateViews.setTextViewText(R.id.precip_snow, popSnowModel.getValue());
            bigUpdateViews.setViewVisibility(R.id.precip_snow_layout, View.VISIBLE);
        }
        bigUpdateViews.setViewVisibility(R.id.extra2_layout, feelsLikeModel != null || humidityModel != null || popRainModel != null || popSnowModel != null ? View.VISIBLE : View.GONE);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, notChannelID)
                        .setCustomContentView(updateViews)
                        .setCustomBigContentView(bigUpdateViews)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .setShowWhen(false);

        if (settingsManager.getNotificationIcon().equals(SettingsManager.TEMPERATURE_ICON)) {
            Integer tempLevel = NumberUtils.tryParseInt(temp.replace("°", ""));

            if (tempLevel == null) {
                mBuilder.setSmallIcon(R.drawable.notification_temp_unknown);
            } else {
                mBuilder.setSmallIcon(WeatherNotificationTemp.getTempDrawable(tempLevel));
            }
        } else if (settingsManager.getNotificationIcon().equals(SettingsManager.CONDITION_ICON)) {
            if (wim.isFontIcon()) {
                mBuilder.setSmallIcon(wim.getWeatherIconResource(viewModel.getWeatherIcon()));
            } else {
                // Use default icon pack here; animated icons are not supported here
                WeatherIconProvider wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY);
                mBuilder.setSmallIcon(wip.getWeatherIconResource(viewModel.getWeatherIcon()));
            }
        }

        Intent onClickIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0);
        mBuilder.setContentIntent(clickPendingIntent);

        // Builds the notification and issues it.
        Float temp_float = NumberUtils.tryParseFloat(temp);
        if (temp_float != null) {
            float temp_f = viewModel.getCurTemp().endsWith(Units.FAHRENHEIT) ?
                    temp_float : ConversionMethods.CtoF(temp_float);
            mBuilder.setColor(WeatherUtils.getColorFromTempF(temp_f));
        } else {
            mBuilder.setColor(Colors.SIMPLEBLUE);
        }
        return mBuilder.build();
    }
}
