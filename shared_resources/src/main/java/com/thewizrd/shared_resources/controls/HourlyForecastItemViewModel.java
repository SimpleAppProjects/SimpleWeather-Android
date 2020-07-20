package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.UV;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Locale;

public class HourlyForecastItemViewModel extends BaseForecastItemViewModel {

    private HourlyForecast forecast;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HourlyForecast getForecast() {
        return forecast;
    }

    public HourlyForecastItemViewModel(@NonNull HourlyForecast hrForecast) {
        this.forecast = hrForecast;

        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>();

        weatherIcon = hrForecast.getIcon();

        if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
            date = hrForecast.getDate().format(DateTimeFormatter.ofPattern("EEE HH:00"));
            shortDate = hrForecast.getDate().format(DateTimeFormatter.ofPattern("HH:00"));
        } else {
            date = hrForecast.getDate().format(DateTimeFormatter.ofPattern("EEE h a"));
            shortDate = hrForecast.getDate().format(DateTimeFormatter.ofPattern("h a"));
        }

        condition = hrForecast.getCondition();
        try {
            if (hrForecast.getHighF() != null && hrForecast.getHighC() != null) {
                int value = Settings.isFahrenheit() ? Math.round(hrForecast.getHighF()) : Math.round(hrForecast.getHighC());
                hiTemp = String.format(Locale.getDefault(), "%d°", value);
            } else {
                hiTemp = "--";
            }
        } catch (NumberFormatException nFe) {
            hiTemp = "--";
            Logger.writeLine(Log.ERROR, nFe);
        }

        pop = hrForecast.getPop() != null ? hrForecast.getPop() + "%" : null;

        if (hrForecast.getWindMph() != null && hrForecast.getWindMph() >= 0 &&
                hrForecast.getWindDegrees() != null && hrForecast.getWindDegrees() >= 0) {
            windDirection = NumberUtils.getValueOrDefault(hrForecast.getWindDegrees(), 0);

            windDir = WeatherUtils.getWindDirection(hrForecast.getWindDegrees());

            int speedVal = Settings.isFahrenheit() ? Math.round(hrForecast.getExtras().getWindMph()) : Math.round(hrForecast.getExtras().getWindKph());
            String speedUnit = Settings.isFahrenheit() ? "mph" : "kph";
            windSpeed = String.format(Locale.getDefault(), "%d %s", speedVal, speedUnit);
        }

        // Extras
        if (hrForecast.getExtras() != null) {
            if (hrForecast.getExtras().getFeelslikeF() != null && (!ObjectsCompat.equals(hrForecast.getExtras().getFeelslikeF(), hrForecast.getExtras().getFeelslikeC()))) {
                int value = Settings.isFahrenheit() ? Math.round(hrForecast.getExtras().getFeelslikeF()) : Math.round(hrForecast.getExtras().getFeelslikeC());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        String.format(Locale.getDefault(), "%d°", value)));
            }

            if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                if (hrForecast.getExtras().getQpfRainIn() != null && hrForecast.getExtras().getQpfRainIn() >= 0) {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", hrForecast.getExtras().getQpfRainIn()) :
                                    String.format(Locale.getDefault(), "%.2f mm", hrForecast.getExtras().getQpfRainMm())));
                }
                if (hrForecast.getExtras().getQpfSnowIn() != null && hrForecast.getExtras().getQpfSnowIn() >= 0) {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", hrForecast.getExtras().getQpfSnowIn()) :
                                    String.format(Locale.getDefault(), "%.2f cm", hrForecast.getExtras().getQpfSnowCm())));
                }
                if (hrForecast.getExtras().getPop() != null && hrForecast.getExtras().getPop() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, pop));
            } else {
                if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, pop));
                if (hrForecast.getExtras().getQpfRainIn() != null && hrForecast.getExtras().getQpfRainIn() >= 0) {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", forecast.getExtras().getQpfRainIn()) :
                                    String.format(Locale.getDefault(), "%.2f mm", forecast.getExtras().getQpfRainMm())));
                }
                if (hrForecast.getExtras().getQpfSnowIn() != null && hrForecast.getExtras().getQpfSnowIn() >= 0) {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                            Settings.isFahrenheit() ?
                                    String.format(Locale.getDefault(), "%.2f in", forecast.getExtras().getQpfSnowIn()) :
                                    String.format(Locale.getDefault(), "%.2f cm", forecast.getExtras().getQpfSnowCm())
                    ));
                }
            }

            if (hrForecast.getExtras().getHumidity() != null) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        String.format(Locale.getDefault(), "%d%%", hrForecast.getExtras().getHumidity())));
            }

            if (hrForecast.getExtras().getDewpointF() != null && (!ObjectsCompat.equals(hrForecast.getExtras().getDewpointF(), hrForecast.getExtras().getDewpointC()))) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        String.format(Locale.getDefault(), "%d°",
                                Settings.isFahrenheit() ?
                                        Math.round(hrForecast.getExtras().getDewpointF()) :
                                        Math.round(hrForecast.getExtras().getDewpointC())
                        )));
            }

            if (hrForecast.getExtras().getUvIndex() != null && hrForecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(hrForecast.getExtras().getUvIndex());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.getDefault(), "%.2f, %s", uv.getIndex(), uv.getDescription())));
            }

            if (hrForecast.getExtras().getPressureIn() != null) {
                float pressureVal = Settings.isFahrenheit() ? hrForecast.getExtras().getPressureIn() : hrForecast.getExtras().getPressureMb();
                String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                        String.format(Locale.getDefault(), "%.2f %s", pressureVal, pressureUnit)));
            }

            if (!StringUtils.isNullOrWhitespace(windSpeed)) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(Locale.getDefault(), "%s, %s", windSpeed, windDir), windDirection));
            }

            if (hrForecast.getExtras().getVisibilityMi() != null && hrForecast.getExtras().getVisibilityMi() >= 0) {
                float visibilityVal = Settings.isFahrenheit() ? hrForecast.getExtras().getVisibilityMi() : hrForecast.getExtras().getVisibilityKm();
                String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(Locale.getDefault(), "%.2f %s", visibilityVal, visibilityUnit)));
            }
        }
    }
}
