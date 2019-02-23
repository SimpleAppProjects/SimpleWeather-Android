package com.thewizrd.shared_resources.controls;

import android.util.Log;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.UV;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForecastItemViewModel {
    private WeatherManager wm;

    private String weatherIcon;
    private String date;
    private String condition;
    private String hiTemp;
    private String loTemp;

    private List<DetailItemViewModel> detailExtras;

    public ForecastItemViewModel() {
        wm = WeatherManager.getInstance();
        detailExtras = new ArrayList<>();
    }

    public ForecastItemViewModel(Forecast forecast) {
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

            if (forecast.getExtras().getQpfRainIn() >= 0) {
                String chance = forecast.getExtras().getPop() + "%";
                String qpfRain = Settings.isFahrenheit() ?
                        String.format(Locale.getDefault(), "%.2f in", forecast.getExtras().getQpfRainIn()) :
                        String.format(Locale.getDefault(), "%.2f mm", forecast.getExtras().getQpfRainMm());
                String qpfSnow = Settings.isFahrenheit() ?
                        String.format(Locale.getDefault(), "%.2f in", forecast.getExtras().getQpfSnowIn()) :
                        String.format(Locale.getDefault(), "%.2f cm", forecast.getExtras().getQpfSnowCm());

                if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, chance));
                } else {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, chance));
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                }
            } else {
                String chance = forecast.getExtras().getPop() + "%";
                if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, chance));
                } else {
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, chance));
                }
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
                    float pressure = Float.parseFloat(pressureVal);
                    detailExtras.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                            String.format(Locale.getDefault(), "%s %s", Float.toString(pressure), pressureUnit)));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }

            if (forecast.getExtras().getWindMph() >= 0) {
                detailExtras.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%d mph", Math.round(forecast.getExtras().getWindMph())) :
                                String.format(Locale.getDefault(), "%d kph", Math.round(forecast.getExtras().getWindKph())),
                        getWindIconRotation(forecast.getExtras().getWindDegrees())));
            }

            if (!StringUtils.isNullOrWhitespace(forecast.getExtras().getVisibilityMi())) {
                String visibilityVal = Settings.isFahrenheit() ?
                        forecast.getExtras().getVisibilityMi() :
                        forecast.getExtras().getVisibilityKm();

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

    private int getWindIconRotation(int angle) {
        return angle - 180;
    }

    public List<DetailItemViewModel> getExtras() {
        return detailExtras;
    }

    public void setExtras(List<DetailItemViewModel> extras) {
        this.detailExtras = extras;
    }
}
