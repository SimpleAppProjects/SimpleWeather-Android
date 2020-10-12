package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.UV;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.ArrayList;

public class HourlyForecastItemViewModel extends BaseForecastItemViewModel {

    private HourlyForecast forecast;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HourlyForecast getForecast() {
        return forecast;
    }

    public HourlyForecastItemViewModel(@NonNull HourlyForecast hrForecast) {
        this.forecast = hrForecast;

        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>(WeatherDetailsType.values().length);

        weatherIcon = hrForecast.getIcon();

        if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
            date = hrForecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR)));
            shortDate = hrForecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)));
        } else {
            date = hrForecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_AMPM));
            shortDate = hrForecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_12HR_AMPM));
        }

        condition = hrForecast.getCondition();
        try {
            if (hrForecast.getHighF() != null && hrForecast.getHighC() != null) {
                int value = Settings.isFahrenheit() ? Math.round(hrForecast.getHighF()) : Math.round(hrForecast.getHighC());
                hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value);
            } else {
                hiTemp = "--";
            }
        } catch (NumberFormatException nFe) {
            hiTemp = "--";
            Logger.writeLine(Log.ERROR, nFe);
        }

        if (hrForecast.getWindMph() != null && hrForecast.getWindKph() != null && hrForecast.getWindMph() >= 0 &&
                hrForecast.getWindDegrees() != null && hrForecast.getWindDegrees() >= 0) {
            windDirection = NumberUtils.getValueOrDefault(hrForecast.getWindDegrees(), 0);

            windDir = WeatherUtils.getWindDirection(hrForecast.getWindDegrees());

            int speedVal = Settings.isFahrenheit() ? Math.round(hrForecast.getExtras().getWindMph()) : Math.round(hrForecast.getExtras().getWindKph());
            String speedUnit = WeatherUtils.getSpeedUnit();
            windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);
        }

        // Extras
        if (hrForecast.getExtras() != null) {
            if (hrForecast.getExtras().getFeelslikeF() != null && (!ObjectsCompat.equals(hrForecast.getExtras().getFeelslikeF(), hrForecast.getExtras().getFeelslikeC()))) {
                int value = Settings.isFahrenheit() ? Math.round(hrForecast.getExtras().getFeelslikeF()) : Math.round(hrForecast.getExtras().getFeelslikeC());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        String.format(LocaleUtils.getLocale(), "%d°", value)));
            }

            if (hrForecast.getExtras().getPop() != null && hrForecast.getExtras().getPop() >= 0)
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, hrForecast.getExtras().getPop() + "%"));
            if (hrForecast.getExtras().getQpfRainIn() != null && hrForecast.getExtras().getQpfRainIn() >= 0) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                        String.format(LocaleUtils.getLocale(), "%.2f %s",
                                Settings.isFahrenheit() ? forecast.getExtras().getQpfRainIn() : forecast.getExtras().getQpfRainMm(),
                                WeatherUtils.getPrecipitationUnit(false))
                        )
                );
            }
            if (hrForecast.getExtras().getQpfSnowIn() != null && hrForecast.getExtras().getQpfSnowIn() >= 0) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                        String.format(LocaleUtils.getLocale(), "%.2f %s",
                                Settings.isFahrenheit() ? forecast.getExtras().getQpfSnowIn() : forecast.getExtras().getQpfSnowCm(),
                                WeatherUtils.getPrecipitationUnit(true)
                        )
                ));
            }
            if (hrForecast.getExtras().getCloudiness() != null && hrForecast.getExtras().getCloudiness() >= 0)
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, hrForecast.getExtras().getCloudiness() + "%"));

            if (hrForecast.getExtras().getHumidity() != null) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        String.format(LocaleUtils.getLocale(), "%d%%", hrForecast.getExtras().getHumidity())));
            }

            if (hrForecast.getExtras().getDewpointF() != null && (!ObjectsCompat.equals(hrForecast.getExtras().getDewpointF(), hrForecast.getExtras().getDewpointC()))) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        String.format(LocaleUtils.getLocale(), "%d°",
                                Settings.isFahrenheit() ?
                                        Math.round(hrForecast.getExtras().getDewpointF()) :
                                        Math.round(hrForecast.getExtras().getDewpointC())
                        )));
            }

            if (hrForecast.getExtras().getUvIndex() != null && hrForecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(hrForecast.getExtras().getUvIndex());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(LocaleUtils.getLocale(), "%.2f, %s", uv.getIndex(), uv.getDescription())));
            }

            if (hrForecast.getExtras().getPressureIn() != null && hrForecast.getExtras().getPressureMb() != null) {
                float pressureVal = Settings.isFahrenheit() ? hrForecast.getExtras().getPressureIn() : hrForecast.getExtras().getPressureMb();
                String pressureUnit = WeatherUtils.getPressureUnit();

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                        String.format(LocaleUtils.getLocale(), "%.2f %s", pressureVal, pressureUnit)));
            }

            if (!StringUtils.isNullOrWhitespace(windSpeed)) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(LocaleUtils.getLocale(), "%s, %s", windSpeed, windDir), windDirection));
            }

            if (hrForecast.getExtras().getWindGustMph() != null && hrForecast.getExtras().getWindGustKph() != null && hrForecast.getExtras().getWindGustMph() >= 0) {
                int speedVal = Settings.isFahrenheit() ? Math.round(hrForecast.getExtras().getWindGustMph()) : Math.round(hrForecast.getExtras().getWindGustKph());
                String speedUnit = WeatherUtils.getSpeedUnit();

                String windGustSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDGUST, windGustSpeed));
            }

            if (hrForecast.getExtras().getVisibilityMi() != null && hrForecast.getExtras().getVisibilityMi() >= 0) {
                float visibilityVal = Settings.isFahrenheit() ? hrForecast.getExtras().getVisibilityMi() : hrForecast.getExtras().getVisibilityKm();
                String visibilityUnit = WeatherUtils.getDistanceUnit();

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(LocaleUtils.getLocale(), "%.2f %s", visibilityVal, visibilityUnit)));
            }
        }
    }
}
