package com.thewizrd.shared_resources.controls;

import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.UV;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForecastItemViewModel extends ObservableViewModel {
    private WeatherManager wm;

    private String weatherIcon;
    private String date;
    private String condition;
    private String hiTemp;
    private String loTemp;

    private String pop;
    private int windDirection;
    private String windSpeed;
    private String windDir;

    private String conditionLongDesc;

    private List<DetailItemViewModel> detailExtras;

    public ForecastItemViewModel() {
        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>();
    }

    public ForecastItemViewModel(Forecast forecast, TextForecastItemViewModel... txtForecasts) {
        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>();

        weatherIcon = forecast.getIcon();
        date = forecast.getDate().format(DateTimeFormatter.ofPattern("EEE dd", Locale.getDefault()));
        condition = forecast.getCondition();
        try {
            hiTemp = (Settings.isFahrenheit() ?
                    String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getHighF()))) : String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getHighC())))) + "º ";
        } catch (NumberFormatException nFe) {
            hiTemp = "--º ";
            Logger.writeLine(Log.ERROR, nFe);
        }
        try {
            loTemp = (Settings.isFahrenheit() ?
                    String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getLowF()))) : String.format(Locale.ROOT, "%d", Math.round(Double.valueOf(forecast.getLowC())))) + "º ";
        } catch (NumberFormatException nFe) {
            loTemp = "--º ";
            Logger.writeLine(Log.ERROR, nFe);
        }

        // Extras
        if (forecast.getExtras() != null) {
            detailExtras.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                    Settings.isFahrenheit() ?
                            String.format(Locale.getDefault(), "%dº", Math.round(forecast.getExtras().getFeelslikeF())) :
                            String.format(Locale.getDefault(), "%dº", Math.round(forecast.getExtras().getFeelslikeC()))));

            String chance = pop = forecast.getExtras().getPop() + "%";
            String qpfRain = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", forecast.getExtras().getQpfRainIn()) :
                    String.format(Locale.getDefault(), "%.2f mm", forecast.getExtras().getQpfRainMm());
            String qpfSnow = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%.2f in", forecast.getExtras().getQpfSnowIn()) :
                    String.format(Locale.getDefault(), "%.2f cm", forecast.getExtras().getQpfSnowCm());

            if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                if (forecast.getExtras().getQpfRainIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                if (forecast.getExtras().getQpfSnowIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, chance));
            } else {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, chance));
                if (forecast.getExtras().getQpfRainIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                if (forecast.getExtras().getQpfSnowIn() >= 0)
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
            }

            if (!StringUtils.isNullOrWhitespace(forecast.getExtras().getHumidity())) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        forecast.getExtras().getHumidity().endsWith("%") ?
                                forecast.getExtras().getHumidity() :
                                forecast.getExtras().getHumidity() + "%"));
            }

            if (!StringUtils.isNullOrWhitespace(forecast.getExtras().getDewpointF())) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.valueOf(forecast.getExtras().getDewpointF()))) :
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.valueOf(forecast.getExtras().getDewpointC())))));
            }

            if (forecast.getExtras().getUvIndex() >= 0) {
                UV uv = new UV(forecast.getExtras().getUvIndex());

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.ROOT, "%s, %s", uv.getIndex(), uv.getDescription())));
            }

            if (!StringUtils.isNullOrWhitespace(forecast.getExtras().getPressureIn())) {
                String pressureVal = Settings.isFahrenheit() ?
                        forecast.getExtras().getPressureIn() :
                        forecast.getExtras().getPressureMb();

                String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

                try {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                            String.format(Locale.getDefault(), "%s %s", pressureVal, pressureUnit)));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }

            if (forecast.getExtras().getWindMph() >= 0) {
                windSpeed = Settings.isFahrenheit() ?
                        String.format(Locale.getDefault(), "%d mph", Math.round(forecast.getExtras().getWindMph())) :
                        String.format(Locale.getDefault(), "%d kph", Math.round(forecast.getExtras().getWindKph()));
                windDirection = forecast.getExtras().getWindDegrees();
                windDir = WeatherUtils.getWindDirection(windDirection);

                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        String.format(Locale.ROOT, "%s, %s", windSpeed, windDir), windDirection));
            }

            if (!StringUtils.isNullOrWhitespace(forecast.getExtras().getVisibilityMi())) {
                String visibilityVal = Settings.isFahrenheit() ?
                        forecast.getExtras().getVisibilityMi() :
                        forecast.getExtras().getVisibilityKm();

                String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

                try {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                            String.format(Locale.getDefault(), "%s %s", visibilityVal, visibilityUnit)));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }
        }

        if (txtForecasts.length > 0) {
            try {
                boolean dayAndNt = txtForecasts.length == 2;
                StringBuilder sb = new StringBuilder();

                TextForecastItemViewModel fctDay = txtForecasts[0];
                sb.append(String.format(Locale.ROOT, "%s - %s", fctDay.getTitle(), fctDay.getFctText()));

                if (dayAndNt) {
                    sb.append(StringUtils.lineSeparator()).append(StringUtils.lineSeparator());

                    TextForecastItemViewModel fctNt = txtForecasts[1];
                    sb.append(String.format(Locale.ROOT, "%s - %s", fctNt.getTitle(), fctNt.getFctText()));
                }

                conditionLongDesc = sb.toString();
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }
        }

        notifyChange();
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public void setWeatherIcon(String weatherIcon) {
        this.weatherIcon = weatherIcon;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getHiTemp() {
        return hiTemp;
    }

    public void setHiTemp(String hiTemp) {
        this.hiTemp = hiTemp;
    }

    public String getLoTemp() {
        return loTemp;
    }

    public void setLoTemp(String loTemp) {
        this.loTemp = loTemp;
    }

    public String getPop() {
        return pop;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(int windDirection) {
        this.windDirection = windDirection;
    }

    public String getWindDirLabel() {
        return windDir;
    }

    public void setWindDirLabel(String windDirection) {
        this.windDir = windDirection;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getConditionLongDesc() {
        return conditionLongDesc;
    }

    public void setConditionLongDesc(String conditionLongDesc) {
        this.conditionLongDesc = conditionLongDesc;
    }

    public List<DetailItemViewModel> getExtras() {
        return detailExtras;
    }

    public void setExtras(List<DetailItemViewModel> extras) {
        this.detailExtras = extras;
    }
}
