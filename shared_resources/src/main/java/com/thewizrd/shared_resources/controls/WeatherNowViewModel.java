package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherNowViewModel {
    private String location;
    private String updateDate;

    // Current Condition
    private String curTemp;
    private String curCondition;
    private String weatherIcon;

    // Weather Details
    private String sunrise;
    private String sunset;
    private List<DetailItemViewModel> weatherDetails;

    // Forecast
    private List<ForecastItemViewModel> forecasts;

    // Additional Details
    private WeatherExtrasViewModel extras;

    // Background
    private String background;
    private int pendingBackground;

    private String weatherCredit;
    private String weatherSource;

    private String weatherLocale;

    public String getLocation() {
        return location;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public String getCurTemp() {
        return curTemp;
    }

    public String getCurCondition() {
        return curCondition;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public List<ForecastItemViewModel> getForecasts() {
        return forecasts;
    }

    public List<DetailItemViewModel> getWeatherDetails() {
        return weatherDetails;
    }

    public String getSunrise() {
        return sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public WeatherExtrasViewModel getExtras() {
        return extras;
    }

    public String getBackground() {
        return background;
    }

    public int getPendingBackground() {
        return pendingBackground;
    }

    public String getWeatherCredit() {
        return weatherCredit;
    }

    public String getWeatherSource() {
        return weatherSource;
    }

    public String getWeatherLocale() {
        return weatherLocale;
    }

    private WeatherManager wm;

    public WeatherNowViewModel() {
        wm = WeatherManager.getInstance();

        forecasts = new ArrayList<>();
        weatherDetails = new ArrayList<>();
        extras = new WeatherExtrasViewModel();
    }

    public WeatherNowViewModel(Weather weather) {
        wm = WeatherManager.getInstance();

        forecasts = new ArrayList<>();
        weatherDetails = new ArrayList<>();
        extras = new WeatherExtrasViewModel();
        updateView(weather);
    }

    public void updateView(Weather weather) {
        if (weather.isValid()) {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();

            // Update extras
            extras.updateView(weather);

            // Update backgrounds
            background = wm.getWeatherBackgroundURI(weather);
            pendingBackground = wm.getWeatherBackgroundColor(weather);

            // Location
            location = weather.getLocation().getName();

            // Date Updated
            updateDate = WeatherUtils.getLastBuildDate(weather);

            // Update current condition
            curTemp = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%d\uf045", Math.round(weather.getCondition().getTempF())) :
                    String.format(Locale.getDefault(), "%d\uf03c", Math.round(weather.getCondition().getTempC()));
            curCondition = (StringUtils.isNullOrWhitespace(weather.getCondition().getWeather()) ? "---" : weather.getCondition().getWeather());
            weatherIcon = weather.getCondition().getIcon();

            // WeatherDetails
            weatherDetails.clear();
            // Precipitation
            if (weather.getPrecipitation() != null) {
                String chance = weather.getPrecipitation().getPop() + "%";
                String qpfRain = Settings.isFahrenheit() ?
                        String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfRainIn()) :
                        String.format(Locale.getDefault(), "%.2f mm", weather.getPrecipitation().getQpfRainMm());
                String qpfSnow = Settings.isFahrenheit() ?
                        String.format(Locale.getDefault(), "%.2f in", weather.getPrecipitation().getQpfSnowIn()) :
                        String.format(Locale.getDefault(), "%.2f cm", weather.getPrecipitation().getQpfSnowCm());

                if (WeatherAPI.OPENWEATHERMAP.equals(Settings.getAPI()) || WeatherAPI.METNO.equals(Settings.getAPI())) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCLOUDINESS, chance));
                } else {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPCHANCE, chance));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPRAIN, qpfRain));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.POPSNOW, qpfSnow));
                }
            }

            // Atmosphere
            String pressureVal = Settings.isFahrenheit() ?
                    weather.getAtmosphere().getPressureIn() :
                    weather.getAtmosphere().getPressureMb();

            String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

            try {
                float pressure = Float.parseFloat(pressureVal);
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE,
                        String.format(Locale.getDefault(), "%s %s %s",
                                getPressureStateIcon(weather.getAtmosphere().getPressureTrend()), Float.toString(pressure), pressureUnit)));
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }

            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                    weather.getAtmosphere().getHumidity().endsWith("%") ?
                            weather.getAtmosphere().getHumidity() :
                            weather.getAtmosphere().getHumidity() + "%"));

            if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getDewpointF())) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.valueOf(weather.getAtmosphere().getDewpointF()))) :
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.valueOf(weather.getAtmosphere().getDewpointC())))));
            }

            String visibilityVal = Settings.isFahrenheit() ?
                    weather.getAtmosphere().getVisibilityMi() :
                    weather.getAtmosphere().getVisibilityKm();

            String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

            try {
                float visibility = Float.parseFloat(visibilityVal);
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                        String.format(Locale.getDefault(), "%s %s", Float.toString(visibility), visibilityUnit)));
            } catch (Exception e) {
                Logger.writeLine(Log.DEBUG, e);
            }

            if (weather.getCondition().getUV() != null) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.ROOT, "%s, %s",
                                weather.getCondition().getUV().getIndex(), weather.getCondition().getUV().getDescription())));
            }

            // Wind
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                    Settings.isFahrenheit() ?
                            String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeF())) :
                            String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeC()))));
            weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                    Settings.isFahrenheit() ?
                            String.format(Locale.getDefault(), "%d mph, %s", Math.round(weather.getCondition().getWindMph()), WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())) :
                            String.format(Locale.getDefault(), "%d kph, %s", Math.round(weather.getCondition().getWindKph()), WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())),
                    weather.getCondition().getWindDegrees()));

            if (weather.getCondition().getBeaufort() != null) {
                weatherDetails.add(new DetailItemViewModel(weather.getCondition().getBeaufort().getScale(),
                        weather.getCondition().getBeaufort().getDescription()));
            }

            // Astronomy
            if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
                sunrise = weather.getAstronomy().getSunrise().format(DateTimeFormatter.ofPattern("HH:mm"));
                sunset = weather.getAstronomy().getSunset().format(DateTimeFormatter.ofPattern("HH:mm"));

                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.SUNRISE, sunrise));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.SUNSET, sunset));
            } else {
                sunrise = weather.getAstronomy().getSunrise().format(DateTimeFormatter.ofPattern("h:mm a"));
                sunset = weather.getAstronomy().getSunset().format(DateTimeFormatter.ofPattern("h:mm a"));

                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.SUNRISE, sunrise));
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.SUNSET, sunset));
            }

            if (weather.getAstronomy().getMoonrise() != null && weather.getAstronomy().getMoonset() != null
                    && weather.getAstronomy().getMoonrise().compareTo(DateTimeUtils.getLocalDateTimeMIN()) > 0
                    && weather.getAstronomy().getMoonset().compareTo(DateTimeUtils.getLocalDateTimeMIN()) > 0) {
                if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONRISE,
                            weather.getAstronomy().getMoonrise().format(DateTimeFormatter.ofPattern("HH:mm"))));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONSET,
                            weather.getAstronomy().getMoonset().format(DateTimeFormatter.ofPattern("HH:mm"))));
                } else {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONRISE,
                            weather.getAstronomy().getMoonrise().format(DateTimeFormatter.ofPattern("h:mm a"))));
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.MOONSET,
                            weather.getAstronomy().getMoonset().format(DateTimeFormatter.ofPattern("h:mm a"))));
                }
            }

            if (weather.getAstronomy().getMoonPhase() != null) {
                weatherDetails.add(new DetailItemViewModel(weather.getAstronomy().getMoonPhase().getPhase(),
                        weather.getAstronomy().getMoonPhase().getDescription()));
            }

            // Add UI elements
            forecasts.clear();
            boolean isDayAndNt = extras.getTextForecast().size() == weather.getForecast().length * 2;
            boolean addTextFct = isDayAndNt || extras.getTextForecast().size() == weather.getForecast().length;
            for (int i = 0; i < weather.getForecast().length; i++) {
                Forecast forecast = weather.getForecast()[i];
                ForecastItemViewModel forecastView;

                if (addTextFct) {
                    if (isDayAndNt)
                        forecastView = new ForecastItemViewModel(forecast, extras.getTextForecast().get(i * 2), extras.getTextForecast().get((i * 2) + 1));
                    else
                        forecastView = new ForecastItemViewModel(forecast, extras.getTextForecast().get(i));
                } else {
                    forecastView = new ForecastItemViewModel(forecast);
                }

                forecasts.add(forecastView);
            }

            // Additional Details
            weatherSource = weather.getSource();
            String creditPrefix = context.getString(R.string.credit_prefix);

            if (WeatherAPI.WEATHERUNDERGROUND.equals(weather.getSource()))
                weatherCredit = String.format("%s WeatherUnderground", creditPrefix);
            else if (WeatherAPI.YAHOO.equals(weather.getSource()))
                weatherCredit = String.format("%s Yahoo!", creditPrefix);
            else if (WeatherAPI.OPENWEATHERMAP.equals(weather.getSource()))
                weatherCredit = String.format("%s OpenWeatherMap", creditPrefix);
            else if (WeatherAPI.METNO.equals(weather.getSource()))
                weatherCredit = String.format("%s MET Norway", creditPrefix);
            else if (WeatherAPI.HERE.equals(weather.getSource()))
                weatherCredit = String.format("%s HERE Weather", creditPrefix);

            // Language
            weatherLocale = weather.getLocale();
        }
    }

    private String getPressureStateIcon(String state) {
        switch (state) {
            // Steady
            case "0":
            default:
                return "";
            // Rising
            case "1":
            case "+":
            case "Rising":
                return "\uf058\uf058";
            // Falling
            case "2":
            case "-":
            case "Falling":
                return "\uf044\uf044";
        }
    }
}
