package com.thewizrd.shared_resources.controls;

import android.text.format.DateFormat;
import android.view.View;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
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
    private String humidity;
    private String pressure;
    private int risingVisiblity;
    private String risingIcon;
    private String visibility;
    private String windChill;
    private int windDirection;
    private String windSpeed;
    private String sunrise;
    private String sunset;

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

    public String getHumidity() {
        return humidity;
    }

    public String getPressure() {
        return pressure;
    }

    public int getRisingVisiblity() {
        return risingVisiblity;
    }

    public String getRisingIcon() {
        return risingIcon;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getWindChill() {
        return windChill;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public String getSunrise() {
        return sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public List<ForecastItemViewModel> getForecasts() {
        return forecasts;
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
        extras = new WeatherExtrasViewModel();
    }

    public WeatherNowViewModel(Weather weather) {
        wm = WeatherManager.getInstance();

        forecasts = new ArrayList<>();
        extras = new WeatherExtrasViewModel();
        updateView(weather);
    }

    public void updateView(Weather weather) {
        if (weather.isValid()) {
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
            // Astronomy
            if (DateFormat.is24HourFormat(SimpleLibrary.getInstance().getApp().getAppContext())) {
                sunrise = weather.getAstronomy().getSunrise().format(DateTimeFormatter.ofPattern("HH:mm"));
                sunset = weather.getAstronomy().getSunset().format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                sunrise = weather.getAstronomy().getSunrise().format(DateTimeFormatter.ofPattern("h:mm a"));
                sunset = weather.getAstronomy().getSunset().format(DateTimeFormatter.ofPattern("h:mm a"));
            }

            // Wind
            windChill = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeF())) :
                    String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeC()));
            windSpeed = Settings.isFahrenheit() ?
                    String.format(Locale.getDefault(), "%d mph", Math.round(weather.getCondition().getWindMph())) :
                    String.format(Locale.getDefault(), "%d kph", Math.round(weather.getCondition().getWindKph()));
            updateWindDirection(weather.getCondition().getWindDegrees());

            // Atmosphere
            humidity = weather.getAtmosphere().getHumidity();
            if (!humidity.endsWith("%"))
                humidity += "%";

            String pressureVal = Settings.isFahrenheit() ?
                    weather.getAtmosphere().getPressureIn() :
                    weather.getAtmosphere().getPressureMb();

            String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

            try {
                float pressure = Float.parseFloat(pressureVal);
                this.pressure = String.format(Locale.getDefault(), "%s %s", Float.toString(pressure), pressureUnit);
            } catch (Exception e) {
                this.pressure = String.format(Locale.getDefault(), "-- %s", pressureUnit);
            }

            updatePressureState(weather.getAtmosphere().getPressureTrend());

            String visibilityVal = Settings.isFahrenheit() ?
                    weather.getAtmosphere().getVisibilityMi() :
                    weather.getAtmosphere().getVisibilityKm();

            String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

            try {
                float visibility = Float.parseFloat(visibilityVal);
                this.visibility = String.format(Locale.getDefault(), "%s %s", Float.toString(visibility), visibilityUnit);
            } catch (Exception e) {
                this.visibility = String.format(Locale.getDefault(), "-- %s", visibilityUnit);
            }

            // Add UI elements
            forecasts.clear();
            for (Forecast forecast : weather.getForecast()) {
                ForecastItemViewModel forecastView = new ForecastItemViewModel(forecast);
                forecasts.add(forecastView);
            }

            // Additional Details
            weatherSource = weather.getSource();
            String creditPrefix = SimpleLibrary.getInstance().getApp().getAppContext().getString(R.string.credit_prefix);

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

            extras.updateView(weather);

            // Language
            weatherLocale = weather.getLocale();
        }
    }

    private void updatePressureState(String state) {
        switch (state) {
            // Steady
            case "0":
            default:
                risingVisiblity = View.GONE;
                risingIcon = "";
                break;
            // Rising
            case "1":
            case "+":
            case "Rising":
                risingVisiblity = View.VISIBLE;
                risingIcon = "\uf058\uf058";
                break;
            // Falling
            case "2":
            case "-":
            case "Falling":
                risingVisiblity = View.VISIBLE;
                risingIcon = "\uf044\uf044";
                break;
        }
    }

    private void updateWindDirection(int angle) {
        windDirection = angle - 180;
    }
}
