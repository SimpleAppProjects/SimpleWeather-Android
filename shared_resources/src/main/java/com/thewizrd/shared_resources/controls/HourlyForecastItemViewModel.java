package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Logger;
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

    public HourlyForecastItemViewModel(HourlyForecast hrForecast) {
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
            hiTemp = (Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%d", Math.round(Double.valueOf(hrForecast.getHighF()))) : String.format(Locale.getDefault(), "%d", Math.round(Double.valueOf(hrForecast.getHighC())))) + "º";
        } catch (NumberFormatException nFe) {
            hiTemp = "--º ";
            Logger.writeLine(Log.ERROR, nFe);
        }
        pop = hrForecast.getPop() + "%";
        windDirection = hrForecast.getWindDegrees();
        windDir = WeatherUtils.getWindDirection(hrForecast.getWindDegrees());
        try {
            windSpeed = (Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(),
                            "%d mph", Math.round(Double.valueOf(hrForecast.getWindMph()))) :
                    String.format(Locale.getDefault(),
                            "%d kph", Math.round(Double.valueOf(hrForecast.getWindKph()))));
        } catch (NumberFormatException nFe) {
            windSpeed = "--";
            Logger.writeLine(Log.ERROR, nFe);
        }

        // Extras
        if (hrForecast.getExtras() != null) {
            if (hrForecast.getExtras().getFeelslikeF() != 0) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%dº", Math.round(hrForecast.getExtras().getFeelslikeF())) :
                                String.format(Locale.getDefault(), "%dº", Math.round(hrForecast.getExtras().getFeelslikeC()))));
            }

            String qpfRain = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", hrForecast.getExtras().getQpfRainIn()) :
                    String.format(Locale.getDefault(), "%.2f mm", hrForecast.getExtras().getQpfRainMm());
            String qpfSnow = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", hrForecast.getExtras().getQpfSnowIn()) :
                    String.format(Locale.getDefault(), "%.2f cm", hrForecast.getExtras().getQpfSnowCm());

            if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                if (hrForecast.getExtras().getQpfRainIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                if (hrForecast.getExtras().getQpfSnowIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                if (!StringUtils.isNullOrWhitespace(hrForecast.getPop()))
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, pop));
            } else {
                if (!StringUtils.isNullOrWhitespace(hrForecast.getPop()))
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, pop));
                if (hrForecast.getExtras().getQpfRainIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                if (hrForecast.getExtras().getQpfSnowIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
            }

            if (!StringUtils.isNullOrWhitespace(hrForecast.getExtras().getHumidity())) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        hrForecast.getExtras().getHumidity().endsWith("%") ?
                                hrForecast.getExtras().getHumidity() :
                                hrForecast.getExtras().getHumidity() + "%"));
            }

            if (!StringUtils.isNullOrWhitespace(hrForecast.getExtras().getDewpointF())) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.valueOf(hrForecast.getExtras().getDewpointF()))) :
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.valueOf(hrForecast.getExtras().getDewpointC())))));
            }

            if (hrForecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(hrForecast.getExtras().getUvIndex());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.ROOT, "%s, %s", uv.getIndex(), uv.getDescription())));
            }

            if (!StringUtils.isNullOrWhitespace(hrForecast.getExtras().getPressureIn())) {
                String pressureVal = Settings.isFahrenheit() ?
                        hrForecast.getExtras().getPressureIn() :
                        hrForecast.getExtras().getPressureMb();

                String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

                try {
                    float pressure = Float.parseFloat(pressureVal);
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                            String.format(Locale.getDefault(), "%s %s", Float.toString(pressure), pressureUnit)));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }

            if (!StringUtils.isNullOrWhitespace(windSpeed)) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(Locale.ROOT, "%s, %s", windSpeed, windDir), windDirection));
            }

            if (!StringUtils.isNullOrWhitespace(hrForecast.getExtras().getVisibilityMi())) {
                String visibilityVal = Settings.isFahrenheit() ?
                        hrForecast.getExtras().getVisibilityMi() :
                        hrForecast.getExtras().getVisibilityKm();

                String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

                try {
                    float visibility = Float.parseFloat(visibilityVal);
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                            String.format(Locale.getDefault(), "%s %s", Float.toString(visibility), visibilityUnit)));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }
        }
    }
}
