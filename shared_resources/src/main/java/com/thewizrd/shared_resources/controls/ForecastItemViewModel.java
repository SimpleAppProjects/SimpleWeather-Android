package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.Units;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.model.Forecast;
import com.thewizrd.shared_resources.weatherdata.model.TextForecast;
import com.thewizrd.shared_resources.weatherdata.model.UV;

import java.text.DecimalFormat;

public class ForecastItemViewModel extends BaseForecastItemViewModel {
    private String loTemp;
    private String conditionLongDesc;

    public ForecastItemViewModel(@NonNull Forecast forecast, TextForecast... txtForecasts) {
        final Context context = SimpleLibrary.getInstance().getAppContext();
        final boolean isFahrenheit = Units.FAHRENHEIT.equals(settingsMgr.getTemperatureUnit());
        final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LocaleUtils.getLocale());
        df.applyPattern("0.##");

        weatherIcon = forecast.getIcon();
        date = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(context.getString(R.string.forecast_date_format)));
        shortDate = date;
        longDate = forecast.getDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.DAY_OF_THE_WEEK));
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
        try {
            if (forecast.getLowF() != null && forecast.getLowC() != null) {
                int value = isFahrenheit ? Math.round(forecast.getLowF()) : Math.round(forecast.getLowC());
                loTemp = String.format(LocaleUtils.getLocale(), "%d°", value);
            } else {
                loTemp = WeatherIcons.PLACEHOLDER;
            }
        } catch (NumberFormatException nFe) {
            loTemp = WeatherIcons.PLACEHOLDER;
            Logger.writeLine(Log.ERROR, nFe);
        }

        // Extras
        if (forecast.getExtras() != null) {
            if (forecast.getExtras().getFeelslikeF() != null && (!ObjectsCompat.equals(forecast.getExtras().getFeelslikeF(), forecast.getExtras().getFeelslikeC()))) {
                int value = isFahrenheit ? Math.round(forecast.getExtras().getFeelslikeF()) : Math.round(forecast.getExtras().getFeelslikeC());
                detailExtras.put(WeatherDetailsType.FEELSLIKE, new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        String.format(LocaleUtils.getLocale(), "%d°", value)));
            }

            if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0)
                detailExtras.put(WeatherDetailsType.POPCHANCE, new DetailItemViewModel(WeatherDetailsType.POPCHANCE, forecast.getExtras().getPop() + "%"));
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

                detailExtras.put(WeatherDetailsType.POPRAIN, new DetailItemViewModel(WeatherDetailsType.POPRAIN,
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
                        precipValue = forecast.getExtras().getQpfSnowIn();
                        precipUnit = context.getString(R.string.unit_in);
                        break;
                    case Units.MILLIMETERS:
                        precipValue = forecast.getExtras().getQpfSnowCm() * 10;
                        precipUnit = context.getString(R.string.unit_mm);
                        break;
                }

                detailExtras.put(WeatherDetailsType.POPSNOW, new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                                String.format(LocaleUtils.getLocale(), "%s %s", df.format(precipValue), precipUnit)
                        )
                );
            }
            if (forecast.getExtras().getCloudiness() != null && forecast.getExtras().getCloudiness() >= 0)
                detailExtras.put(WeatherDetailsType.POPCLOUDINESS, new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, forecast.getExtras().getCloudiness() + "%"));

            if (forecast.getExtras().getHumidity() != null) {
                detailExtras.put(WeatherDetailsType.HUMIDITY, new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        String.format(LocaleUtils.getLocale(), "%d%%", forecast.getExtras().getHumidity())));
            }

            if (forecast.getExtras().getDewpointF() != null && (!ObjectsCompat.equals(forecast.getExtras().getDewpointF(), forecast.getExtras().getDewpointC()))) {
                detailExtras.put(WeatherDetailsType.DEWPOINT, new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        String.format(LocaleUtils.getLocale(), "%d°",
                                isFahrenheit ?
                                        Math.round(forecast.getExtras().getDewpointF()) :
                                        Math.round(forecast.getExtras().getDewpointC())
                        ))
                );
            }

            if (forecast.getExtras().getUvIndex() != null && forecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(forecast.getExtras().getUvIndex());

                detailExtras.put(WeatherDetailsType.UV, new DetailItemViewModel(uv));
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

                detailExtras.put(WeatherDetailsType.PRESSURE, new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                        String.format(LocaleUtils.getLocale(), "%s %s", df.format(pressureVal), pressureUnit)));
            }

            if (forecast.getExtras().getWindMph() != null && forecast.getExtras().getWindKph() != null && forecast.getExtras().getWindMph() >= 0 &&
                    forecast.getExtras().getWindDegrees() != null && forecast.getExtras().getWindDegrees() >= 0) {
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

                windDirection = forecast.getExtras().getWindDegrees();
                windDir = WeatherUtils.getWindDirection(windDirection);

                detailExtras.put(WeatherDetailsType.WINDSPEED, new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(LocaleUtils.getLocale(), "%s, %s", windSpeed, windDir), windDirection + 180));
            }

            if (forecast.getExtras().getWindGustMph() != null && forecast.getExtras().getWindGustKph() != null && forecast.getExtras().getWindGustMph() >= 0) {
                final String unit = settingsMgr.getSpeedUnit();
                int speedVal;
                String speedUnit;

                switch (unit) {
                    case Units.MILES_PER_HOUR:
                    default:
                        speedVal = Math.round(forecast.getExtras().getWindGustMph());
                        speedUnit = context.getString(R.string.unit_mph);
                        break;
                    case Units.KILOMETERS_PER_HOUR:
                        speedVal = Math.round(forecast.getExtras().getWindGustKph());
                        speedUnit = context.getString(R.string.unit_kph);
                        break;
                    case Units.METERS_PER_SECOND:
                        speedVal = Math.round(ConversionMethods.kphToMsec(forecast.getExtras().getWindGustKph()));
                        speedUnit = context.getString(R.string.unit_msec);
                        break;
                }

                String windGustSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit);
                detailExtras.put(WeatherDetailsType.WINDGUST, new DetailItemViewModel(WeatherDetailsType.WINDGUST, windGustSpeed));
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

                detailExtras.put(WeatherDetailsType.VISIBILITY, new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(LocaleUtils.getLocale(), "%d %s", visibilityVal, visibilityUnit)));
            }
        }

        if (txtForecasts.length > 0) {
            try {
                boolean dayAndNt = txtForecasts.length == 2;
                StringBuilder sb = new StringBuilder();

                TextForecast fctDay = txtForecasts[0];
                sb.append(isFahrenheit ? fctDay.getFcttext() : fctDay.getFcttextMetric());

                if (dayAndNt) {
                    sb.append(StringUtils.lineSeparator()).append(StringUtils.lineSeparator());

                    TextForecast fctNt = txtForecasts[1];
                    sb.append(isFahrenheit ? fctNt.getFcttext() : fctNt.getFcttextMetric());
                }

                conditionLongDesc = sb.toString();
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }
        }
    }

    public String getLoTemp() {
        return loTemp;
    }

    public void setLoTemp(String loTemp) {
        this.loTemp = loTemp;
    }

    public String getConditionLongDesc() {
        return conditionLongDesc;
    }

    public void setConditionLongDesc(String conditionLongDesc) {
        this.conditionLongDesc = conditionLongDesc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ForecastItemViewModel that = (ForecastItemViewModel) o;

        if (getLoTemp() != null ? !getLoTemp().equals(that.getLoTemp()) : that.getLoTemp() != null)
            return false;
        return getConditionLongDesc() != null ? getConditionLongDesc().equals(that.getConditionLongDesc()) : that.getConditionLongDesc() == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getLoTemp() != null ? getLoTemp().hashCode() : 0);
        result = 31 * result + (getConditionLongDesc() != null ? getConditionLongDesc().hashCode() : 0);
        return result;
    }
}
