package com.thewizrd.shared_resources.controls;

import android.util.Log;

import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.TextForecast;
import com.thewizrd.shared_resources.weatherdata.UV;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Locale;

public class ForecastItemViewModel extends BaseForecastItemViewModel {
    private String loTemp;
    private String conditionLongDesc;

    public ForecastItemViewModel(Forecast forecast, TextForecast... txtForecasts) {
        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>(WeatherDetailsType.values().length);

        weatherIcon = forecast.getIcon();
        date = forecast.getDate().format(DateTimeFormatter.ofPattern("EEE dd", Locale.getDefault()));
        shortDate = date;
        condition = forecast.getCondition();
        try {
            if (forecast.getHighF() != null && forecast.getHighC() != null) {
                int value = Settings.isFahrenheit() ? Math.round(forecast.getHighF()) : Math.round(forecast.getHighC());
                hiTemp = String.format(Locale.getDefault(), "%d°", value);
            } else {
                hiTemp = "--";
            }
        } catch (NumberFormatException nFe) {
            hiTemp = "--";
            Logger.writeLine(Log.ERROR, nFe);
        }
        try {
            if (forecast.getLowF() != null && forecast.getLowC() != null) {
                int value = Settings.isFahrenheit() ? Math.round(forecast.getLowF()) : Math.round(forecast.getLowC());
                loTemp = String.format(Locale.getDefault(), "%d°", value);
            } else {
                loTemp = "--";
            }
        } catch (NumberFormatException nFe) {
            loTemp = "--";
            Logger.writeLine(Log.ERROR, nFe);
        }

        // Extras
        if (forecast.getExtras() != null) {
            if (forecast.getExtras().getFeelslikeF() != null && (!ObjectsCompat.equals(forecast.getExtras().getFeelslikeF(), forecast.getExtras().getFeelslikeC()))) {
                int value = Settings.isFahrenheit() ? Math.round(forecast.getExtras().getFeelslikeF()) : Math.round(forecast.getExtras().getFeelslikeC());
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        String.format(Locale.getDefault(), "%d°", value)));
            }

            if (forecast.getExtras().getPop() != null && forecast.getExtras().getPop() >= 0)
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, forecast.getExtras().getPop() + "%"));
            if (forecast.getExtras().getQpfRainIn() != null && forecast.getExtras().getQpfRainIn() >= 0) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN,
                                String.format(Locale.getDefault(), "%.2f %s",
                                        Settings.isFahrenheit() ? forecast.getExtras().getQpfRainIn() : forecast.getExtras().getQpfRainMm(),
                                        WeatherUtils.getPrecipitationUnit(false))
                        )
                );
            }
            if (forecast.getExtras().getQpfSnowIn() != null && forecast.getExtras().getQpfSnowIn() >= 0) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW,
                                String.format(Locale.getDefault(), "%.2f %s",
                                        Settings.isFahrenheit() ? forecast.getExtras().getQpfSnowIn() : forecast.getExtras().getQpfSnowCm(),
                                        WeatherUtils.getPrecipitationUnit(true))
                        )
                );
            }
            if (forecast.getExtras().getCloudiness() != null && forecast.getExtras().getCloudiness() >= 0)
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, forecast.getExtras().getCloudiness() + "%"));

            if (forecast.getExtras().getHumidity() != null) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        String.format(Locale.getDefault(), "%d%%", forecast.getExtras().getHumidity())));
            }

            if (forecast.getExtras().getDewpointF() != null && (!ObjectsCompat.equals(forecast.getExtras().getDewpointF(), forecast.getExtras().getDewpointC()))) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        String.format(Locale.getDefault(), "%d°",
                                Settings.isFahrenheit() ?
                                        Math.round(forecast.getExtras().getDewpointF()) :
                                        Math.round(forecast.getExtras().getDewpointC())
                        ))
                );
            }

            if (forecast.getExtras().getUvIndex() != null && forecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(forecast.getExtras().getUvIndex());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.ROOT, "%.1f, %s", uv.getIndex(), uv.getDescription())));
            }

            if (forecast.getExtras().getPressureIn() != null && forecast.getExtras().getPressureMb() != null) {
                float pressureVal = Settings.isFahrenheit() ? forecast.getExtras().getPressureIn() : forecast.getExtras().getPressureMb();
                String pressureUnit = WeatherUtils.getPressureUnit();

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                        String.format(Locale.getDefault(), "%.2f %s", pressureVal, pressureUnit)));
            }

            if (forecast.getExtras().getWindMph() != null && forecast.getExtras().getWindKph() != null && forecast.getExtras().getWindMph() >= 0 &&
                    forecast.getExtras().getWindDegrees() != null && forecast.getExtras().getWindDegrees() >= 0) {
                int speedVal = Settings.isFahrenheit() ? Math.round(forecast.getExtras().getWindMph()) : Math.round(forecast.getExtras().getWindKph());
                String speedUnit = WeatherUtils.getSpeedUnit();

                windSpeed = String.format(Locale.getDefault(), "%d %s", speedVal, speedUnit);

                windDirection = forecast.getExtras().getWindDegrees();
                windDir = WeatherUtils.getWindDirection(windDirection);

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(Locale.getDefault(), "%s, %s", windSpeed, windDir), windDirection));
            }

            if (forecast.getExtras().getWindGustMph() != null && forecast.getExtras().getWindGustKph() != null && forecast.getExtras().getWindGustMph() >= 0) {
                int speedVal = Settings.isFahrenheit() ? Math.round(forecast.getExtras().getWindGustMph()) : Math.round(forecast.getExtras().getWindGustKph());
                String speedUnit = WeatherUtils.getSpeedUnit();

                String windGustSpeed = String.format(Locale.getDefault(), "%d %s", speedVal, speedUnit);
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDGUST, windGustSpeed));
            }

            if (forecast.getExtras().getVisibilityMi() != null && forecast.getExtras().getVisibilityMi() >= 0) {
                float visibilityVal = Settings.isFahrenheit() ? forecast.getExtras().getVisibilityMi() : forecast.getExtras().getVisibilityKm();
                String visibilityUnit = WeatherUtils.getDistanceUnit();

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(Locale.getDefault(), "%.2f %s", visibilityVal, visibilityUnit)));
            }
        }

        if (txtForecasts.length > 0) {
            try {
                boolean dayAndNt = txtForecasts.length == 2;
                StringBuilder sb = new StringBuilder();

                TextForecast fctDay = txtForecasts[0];
                sb.append(Settings.isFahrenheit() ? fctDay.getFcttext() : fctDay.getFcttextMetric());

                if (dayAndNt) {
                    sb.append(StringUtils.lineSeparator()).append(StringUtils.lineSeparator());

                    TextForecast fctNt = txtForecasts[1];
                    sb.append(Settings.isFahrenheit() ? fctNt.getFcttext() : fctNt.getFcttextMetric());
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
