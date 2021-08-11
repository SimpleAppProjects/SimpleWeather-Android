package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.model.UV;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class HourlyForecastItemViewModel extends BaseForecastItemViewModel {

    private final HourlyForecast forecast;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HourlyForecast getForecast() {
        return forecast;
    }

    public HourlyForecastItemViewModel(@NonNull HourlyForecast forecast) {
        this.forecast = forecast;

        final Context context = SimpleLibrary.getInstance().getAppContext();
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(settingsMgr.getTemperatureUnit());
        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        detailExtras = new ArrayList<>(WeatherDetailsType.values().length);

        weatherIcon = forecast.getIcon();

        if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
            date = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR)));
            shortDate = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_24HR)));
        } else {
            date = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_AMPM));
            shortDate = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_12HR_AMPM));
        }

        condition = wm.supportsWeatherLocale() ? forecast.getCondition() : wm.getWeatherCondition(forecast.getIcon());
        try {
            if (forecast.getHighF() != null && forecast.getHighC() != null) {
                int value = isFahrenheit ? Math.round(forecast.getHighF()) : Math.round(forecast.getHighC());
                hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value);
            } else {
                hiTemp = WeatherIcons.PLACEHOLDER;
            }
        } catch (NumberFormatException nFe) {
            hiTemp = WeatherIcons.PLACEHOLDER;
            Logger.writeLine(Log.ERROR, nFe);
        }

        if (forecast.getWindMph() != null && forecast.getWindKph() != null && forecast.getWindMph() >= 0 &&
                forecast.getWindDegrees() != null && forecast.getWindDegrees() >= 0) {
            windDirection = NumberUtils.getValueOrDefault(forecast.getWindDegrees(), 0);
            windDir = WeatherUtils.getWindDirection(forecast.getWindDegrees());

            final String unit = settingsMgr.getSpeedUnit();
            int speedVal;
            String speedUnit;

            switch (unit) {
                case Units.MILES_PER_HOUR:
                default:
                    speedVal = Math.round(forecast.getExtras().getWindMph());
                    speedUnit = context.getString(R.string.unit_mph);
                    break;
                case Units.KILOMETERS_PER_HOUR:
                    speedVal = Math.round(forecast.getExtras().getWindKph());
                    speedUnit = context.getString(R.string.unit_kph);
                    break;
                case Units.METERS_PER_SECOND:
                    speedVal = Math.round(ConversionMethods.kphToMsec(forecast.getExtras().getWindKph()));
                    speedUnit = context.getString(R.string.unit_msec);
                    break;
            }

            windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);
        }

        // Extras
        if (forecast.getExtras() != null) {
            if (forecast.getExtras().getFeelslikeF() != null && (!ObjectsCompat.equals(forecast.getExtras().getFeelslikeF(), forecast.getExtras().getFeelslikeC()))) {
                int value = isFahrenheit ? Math.round(forecast.getExtras().getFeelslikeF()) : Math.round(forecast.getExtras().getFeelslikeC());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        String.format(LocaleUtils.getLocale(), "%d°", value)));
            }

            if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0)
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, forecast.getExtras().getPop() + "%"));
            if (forecast.getExtras().getQpfRainIn() != null && forecast.getExtras().getQpfRainIn() >= 0) {
                final String unit = settingsMgr.getPrecipitationUnit();
                float precipValue;
                String precipUnit;

                switch (unit) {
                    case Units.INCHES:
                    default:
                        precipValue = forecast.getExtras().getQpfRainIn();
                        precipUnit = context.getString(R.string.unit_in);
                        break;
                    case Units.MILLIMETERS:
                        precipValue = forecast.getExtras().getQpfRainMm();
                        precipUnit = context.getString(R.string.unit_mm);
                        break;
                }

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                                String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)
                        )
                );
            }
            if (forecast.getExtras().getQpfSnowIn() != null && forecast.getExtras().getQpfSnowIn() >= 0) {
                final String unit = settingsMgr.getPrecipitationUnit();
                float precipValue;
                String precipUnit;

                switch (unit) {
                    case Units.INCHES:
                    default:
                        precipValue = this.forecast.getExtras().getQpfSnowIn();
                        precipUnit = context.getString(R.string.unit_in);
                        break;
                    case Units.MILLIMETERS:
                        precipValue = this.forecast.getExtras().getQpfSnowCm() * 10;
                        precipUnit = context.getString(R.string.unit_mm);
                        break;
                }

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                        String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)
                ));
            }
            if (forecast.getExtras().getCloudiness() != null && forecast.getExtras().getCloudiness() >= 0)
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, forecast.getExtras().getCloudiness() + "%"));

            if (forecast.getExtras().getHumidity() != null) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        String.format(LocaleUtils.getLocale(), "%d%%", forecast.getExtras().getHumidity())));
            }

            if (forecast.getExtras().getDewpointF() != null && (!ObjectsCompat.equals(forecast.getExtras().getDewpointF(), forecast.getExtras().getDewpointC()))) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        String.format(LocaleUtils.getLocale(), "%d°",
                                isFahrenheit ?
                                        Math.round(forecast.getExtras().getDewpointF()) :
                                        Math.round(forecast.getExtras().getDewpointC())
                        )));
            }

            if (forecast.getExtras().getUvIndex() != null && forecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(forecast.getExtras().getUvIndex());

                detailExtras.add(new DetailItemViewModel(uv));
            }

            if (forecast.getExtras().getPressureIn() != null && forecast.getExtras().getPressureMb() != null) {
                final String unit = settingsMgr.getPressureUnit();
                float pressureVal;
                String pressureUnit;

                switch (unit) {
                    case Units.INHG:
                    default:
                        pressureVal = forecast.getExtras().getPressureIn();
                        pressureUnit = context.getString(R.string.unit_inHg);
                        break;
                    case Units.MILLIBAR:
                        pressureVal = forecast.getExtras().getPressureMb();
                        pressureUnit = context.getString(R.string.unit_mBar);
                        break;
                }

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                        String.format(LocaleUtils.getLocale(), "%s %s", df.format(pressureVal), pressureUnit)));
            }

            if (!StringUtils.isNullOrWhitespace(windSpeed)) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(LocaleUtils.getLocale(), "%s, %s", windSpeed, windDir), windDirection + 180));
            }

            if (forecast.getExtras().getWindGustMph() != null && forecast.getExtras().getWindGustKph() != null && forecast.getExtras().getWindGustMph() >= 0) {
                final String unit = settingsMgr.getSpeedUnit();
                int speedVal;
                String speedUnit;

                switch (unit) {
                    case Units.MILES_PER_HOUR:
                    default:
                        speedVal = Math.round(forecast.getExtras().getWindMph());
                        speedUnit = context.getString(R.string.unit_mph);
                        break;
                    case Units.KILOMETERS_PER_HOUR:
                        speedVal = Math.round(forecast.getExtras().getWindKph());
                        speedUnit = context.getString(R.string.unit_kph);
                        break;
                    case Units.METERS_PER_SECOND:
                        speedVal = Math.round(ConversionMethods.kphToMsec(forecast.getExtras().getWindKph()));
                        speedUnit = context.getString(R.string.unit_msec);
                        break;
                }

                String windGustSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDGUST, windGustSpeed));
            }

            if (forecast.getExtras().getVisibilityMi() != null && forecast.getExtras().getVisibilityMi() >= 0) {
                final String unit = settingsMgr.getDistanceUnit();
                int visibilityVal;
                String visibilityUnit;

                switch (unit) {
                    case Units.MILES:
                    default:
                        visibilityVal = Math.round(forecast.getExtras().getVisibilityMi());
                        visibilityUnit = context.getString(R.string.unit_miles);
                        break;
                    case Units.KILOMETERS:
                        visibilityVal = Math.round(forecast.getExtras().getVisibilityKm());
                        visibilityUnit = context.getString(R.string.unit_kilometers);
                        break;
                }

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(LocaleUtils.getLocale(), "%d %s", visibilityVal, visibilityUnit)));
            }
        }
    }
}
