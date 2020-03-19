package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.TypefaceSpan;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.ObjectsCompat;
import androidx.databinding.Bindable;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.BR;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.WeatherIconTextSpan;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.ObservableForecastLoadingList;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class WeatherNowViewModel extends ObservableViewModel {
    private String location;
    private String updateDate;

    // Current Condition
    private CharSequence curTemp;
    private String curCondition;
    private String weatherIcon;

    // Weather Details
    private String sunrise;
    private String sunset;
    private List<DetailItemViewModel> weatherDetails;

    // Forecast
    private ObservableForecastLoadingList<ForecastItemViewModel> forecasts;

    // Additional Details
    private ObservableForecastLoadingList<HourlyForecastItemViewModel> hourlyForecasts;
    private List<WeatherAlertViewModel> alerts;

    // Background
    private String background;
    private int pendingBackground = -1;
    private int origPendingBackground = -1;
    private boolean isDark = true;

    private String weatherCredit;
    private String weatherSource;

    private String weatherLocale;

    @Bindable
    public String getLocation() {
        return location;
    }

    @Bindable
    public String getUpdateDate() {
        return updateDate;
    }

    @Bindable
    public CharSequence getCurTemp() {
        return curTemp;
    }

    @Bindable
    public String getCurCondition() {
        return curCondition;
    }

    @Bindable
    public String getWeatherIcon() {
        return weatherIcon;
    }

    @Bindable
    public List<ForecastItemViewModel> getForecasts() {
        return forecasts;
    }

    @Bindable
    public List<HourlyForecastItemViewModel> getHourlyForecasts() {
        return hourlyForecasts;
    }

    @Bindable
    public List<DetailItemViewModel> getWeatherDetails() {
        return weatherDetails;
    }

    @Bindable
    public List<WeatherAlertViewModel> getAlerts() {
        return alerts;
    }

    @Bindable
    public String getSunrise() {
        return sunrise;
    }

    @Bindable
    public String getSunset() {
        return sunset;
    }

    @Bindable
    public String getBackground() {
        return background;
    }

    @Bindable
    public int getPendingBackground() {
        return pendingBackground;
    }

    @Bindable
    public boolean isLightBackground() {
        return !isDark;
    }

    @Bindable
    public String getWeatherCredit() {
        return weatherCredit;
    }

    @Bindable
    public String getWeatherSource() {
        return weatherSource;
    }

    @Bindable
    public String getWeatherLocale() {
        return weatherLocale;
    }

    private WeatherManager wm;
    private Weather weather;

    public WeatherNowViewModel() {
        wm = WeatherManager.getInstance();

        forecasts = new ObservableForecastLoadingList<>(ForecastItemViewModel.class);
        forecasts.addOnListChangedCallback(new OnListChangedListener<ForecastItemViewModel>() {
            @Override
            public void onChanged(ArrayList<ForecastItemViewModel> sender, ListChangedArgs<ForecastItemViewModel> args) {
                notifyPropertyChanged(BR.forecasts);
            }
        });

        hourlyForecasts = new ObservableForecastLoadingList<>(HourlyForecastItemViewModel.class);
        hourlyForecasts.addOnListChangedCallback(new OnListChangedListener<HourlyForecastItemViewModel>() {
            @Override
            public void onChanged(ArrayList<HourlyForecastItemViewModel> sender, ListChangedArgs<HourlyForecastItemViewModel> args) {
                notifyPropertyChanged(BR.hourlyForecasts);
            }
        });

        weatherDetails = new ArrayList<>(WeatherDetailsType.values().length);
        alerts = new ArrayList<>();
    }

    public WeatherNowViewModel(Weather weather) {
        this();
        updateView(weather);
    }

    public void updatePendingBackground(Context context, boolean isPhone) {
        pendingBackground = origPendingBackground;

        if (isPhone && (!ColorsUtils.isSuperDark(pendingBackground) || ColorsUtils.isSuperLight(pendingBackground))) {
            final int systemNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            boolean isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES || systemNightMode == Configuration.UI_MODE_NIGHT_YES;
            if (isDarkMode) {
                pendingBackground = ColorUtils.blendARGB(pendingBackground, Colors.BLACK, 0.75f);
            }
        }

        isDark = pendingBackground != -1 && !ColorsUtils.isSuperLight(pendingBackground);
    }

    public void updateView(final Weather weather) {
        if (weather != null && weather.isValid() && !ObjectsCompat.equals(this.weather, weather)) {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            boolean isPhone = SimpleLibrary.getInstance().getApp().isPhone();

            this.weather = weather;

            // Update backgrounds
            String newBg = wm.getWeatherBackgroundURI(weather);
            if (!ObjectsCompat.equals(newBg, background)) {
                background = newBg;
                notifyPropertyChanged(BR.background);
            }

            origPendingBackground = wm.getWeatherBackgroundColor(weather);
            updatePendingBackground(context, isPhone);
            notifyPropertyChanged(BR.pendingBackground);

            // Location
            if (!ObjectsCompat.equals(location, weather.getLocation().getName())) {
                location = weather.getLocation().getName();
                notifyPropertyChanged(BR.location);
            }

            // Date Updated
            if (!ObjectsCompat.equals(updateDate, WeatherUtils.getLastBuildDate(weather))) {
                updateDate = WeatherUtils.getLastBuildDate(weather);
                notifyPropertyChanged(BR.updateDate);
            }

            // Update current condition
            SpannableStringBuilder curTempSSBuilder = new SpannableStringBuilder();
            if (weather.getCondition().getTempF() != weather.getCondition().getTempC()) {
                int temp = (int) (Settings.isFahrenheit() ? Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC()));
                curTempSSBuilder.append(Integer.toString(temp));
            } else {
                curTempSSBuilder.append("---");
            }
            String unitTemp = Settings.isFahrenheit() ? WeatherIcons.FAHRENHEIT : WeatherIcons.CELSIUS;
            curTempSSBuilder.append(unitTemp)
                    .setSpan(new WeatherIconTextSpan(context), curTempSSBuilder.length() - unitTemp.length(), curTempSSBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (!ObjectsCompat.equals(curTemp, curTempSSBuilder)) {
                curTemp = curTempSSBuilder;
                notifyPropertyChanged(BR.curTemp);
            }
            String newCondition = (StringUtils.isNullOrWhitespace(weather.getCondition().getWeather()) ? "---" : weather.getCondition().getWeather());
            if (!ObjectsCompat.equals(curCondition, newCondition)) {
                curCondition = newCondition;
                notifyPropertyChanged(BR.curCondition);
            }
            if (!ObjectsCompat.equals(weatherIcon, weather.getCondition().getIcon())) {
                weatherIcon = weather.getCondition().getIcon();
                notifyPropertyChanged(BR.weatherIcon);
            }

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
            if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getPressureMb())) {
                String pressureVal = Settings.isFahrenheit() ?
                        weather.getAtmosphere().getPressureIn() :
                        weather.getAtmosphere().getPressureMb();

                String pressureUnit = Settings.isFahrenheit() ? "in" : "mb";

                try {
                    CharSequence pressureStateIcon = getPressureStateIcon(weather.getAtmosphere().getPressureTrend());

                    SpannableStringBuilder ssBuilder = new SpannableStringBuilder();
                    ssBuilder.append(pressureStateIcon)
                            .append(" ")
                            .append(pressureVal)
                            .append(" ")
                            .append(pressureUnit);

                    if (pressureStateIcon.length() > 0) {
                        TypefaceSpan span = new WeatherIconTextSpan(context);
                        ssBuilder.setSpan(span, 0, pressureStateIcon.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.PRESSURE, ssBuilder));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }

            if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getHumidity())) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.HUMIDITY,
                        weather.getAtmosphere().getHumidity().endsWith("%") ?
                                weather.getAtmosphere().getHumidity() :
                                weather.getAtmosphere().getHumidity() + "%"));
            }

            if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getDewpointF())) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.DEWPOINT,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.parseFloat(weather.getAtmosphere().getDewpointF()))) :
                                String.format(Locale.getDefault(), "%dº", Math.round(Float.parseFloat(weather.getAtmosphere().getDewpointC())))));
            }

            if (!StringUtils.isNullOrWhitespace(weather.getAtmosphere().getVisibilityMi())) {
                String visibilityVal = Settings.isFahrenheit() ?
                        weather.getAtmosphere().getVisibilityMi() :
                        weather.getAtmosphere().getVisibilityKm();

                String visibilityUnit = Settings.isFahrenheit() ? "mi" : "km";

                try {
                    weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.VISIBILITY,
                            String.format(Locale.getDefault(), "%s %s", visibilityVal, visibilityUnit)));
                } catch (Exception e) {
                    Logger.writeLine(Log.DEBUG, e);
                }
            }

            if (weather.getCondition().getUv() != null) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.UV,
                        String.format(Locale.ROOT, "%s, %s",
                                weather.getCondition().getUv().getIndex(), weather.getCondition().getUv().getDescription())));
            }

            if (weather.getCondition().getFeelslikeF() != weather.getCondition().getFeelslikeC()) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.FEELSLIKE,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeF())) :
                                String.format(Locale.getDefault(), "%dº", Math.round(weather.getCondition().getFeelslikeC()))));
            }

            // Wind
            if (weather.getCondition().getWindMph() != weather.getCondition().getWindKph()) {
                weatherDetails.add(new DetailItemViewModel(WeatherDetailsType.WINDSPEED,
                        Settings.isFahrenheit() ?
                                String.format(Locale.getDefault(), "%d mph, %s", Math.round(weather.getCondition().getWindMph()), WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())) :
                                String.format(Locale.getDefault(), "%d kph, %s", Math.round(weather.getCondition().getWindKph()), WeatherUtils.getWindDirection(weather.getCondition().getWindDegrees())),
                        weather.getCondition().getWindDegrees()));
            }

            if (weather.getCondition().getBeaufort() != null) {
                weatherDetails.add(new DetailItemViewModel(weather.getCondition().getBeaufort().getScale(),
                        weather.getCondition().getBeaufort().getDescription()));
            }

            // Astronomy
            if (weather.getAstronomy() != null) {
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
            } else {
                sunrise = null;
                sunset = null;
            }
            notifyPropertyChanged(BR.sunrise);
            notifyPropertyChanged(BR.sunset);
            notifyPropertyChanged(BR.weatherDetails);

            // Add UI elements
            if (weather.getForecast() != null && weather.getForecast().size() > 0) {
                forecasts.clear();

                int textForecastSize = weather.getTxtForecast() != null ? weather.getTxtForecast().size() : 0;

                boolean isDayAndNt = textForecastSize == weather.getForecast().size() * 2;
                boolean addTextFct = isDayAndNt || textForecastSize == weather.getForecast().size();
                for (int i = 0; i < weather.getForecast().size(); i++) {
                    Forecast forecast = weather.getForecast().get(i);
                    ForecastItemViewModel forecastView;

                    if (addTextFct) {
                        if (isDayAndNt)
                            forecastView = new ForecastItemViewModel(forecast, weather.getTxtForecast().get(i * 2), weather.getTxtForecast().get((i * 2) + 1));
                        else
                            forecastView = new ForecastItemViewModel(forecast, weather.getTxtForecast().get(i));
                    } else {
                        forecastView = new ForecastItemViewModel(forecast);
                    }

                    forecasts.add(forecastView);
                }
            } else {
                // Let collection handle changes (clearing, etc.)
                forecasts.setWeather(weather);
            }
            notifyPropertyChanged(BR.forecasts);

            // Update extras
            if (weather.getHrForecast() != null && weather.getHrForecast().size() > 0) {
                hourlyForecasts.clear();

                for (final HourlyForecast hr_forecast : weather.getHrForecast()) {
                    HourlyForecastItemViewModel hrforecastView;
                    hrforecastView = new AsyncTask<HourlyForecastItemViewModel>().await(new Callable<HourlyForecastItemViewModel>() {
                        @Override
                        public HourlyForecastItemViewModel call() {
                            return new HourlyForecastItemViewModel(hr_forecast);
                        }
                    });
                    hourlyForecasts.add(hrforecastView);
                }
            } else {
                // Let collection handle changes (clearing, etc.)
                hourlyForecasts.setWeather(weather);
            }
            notifyPropertyChanged(BR.hourlyForecasts);

            alerts.clear();
            if (weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
                for (WeatherAlert alert : weather.getWeatherAlerts()) {
                    // Skip if alert has expired
                    if (alert.getExpiresDate().compareTo(ZonedDateTime.now()) <= 0)
                        continue;

                    WeatherAlertViewModel alertView = new WeatherAlertViewModel(alert);
                    alerts.add(alertView);
                }
            }
            notifyPropertyChanged(BR.alerts);

            // Additional Details
            if (!ObjectsCompat.equals(weatherSource, weather.getSource())) {
                weatherSource = weather.getSource();
                notifyPropertyChanged(BR.weatherSource);
            }

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
            else if (WeatherAPI.NWS.equals(weather.getSource()))
                weatherCredit = String.format("%s U.S. National Weather Service", creditPrefix);
            notifyPropertyChanged(BR.weatherCredit);

            // Language
            weatherLocale = weather.getLocale();
            notifyPropertyChanged(BR.weatherLocale);
        }
    }

    public void reset() {
        location = null;
        updateDate = null;
        curTemp = null;
        curCondition = null;
        weatherIcon = null;
        sunrise = null;
        sunset = null;
        weatherDetails.clear();
        forecasts.clear();
        hourlyForecasts.clear();
        alerts.clear();
        background = null;
        pendingBackground = -1;
        origPendingBackground = -1;
        isDark = true;
        weatherCredit = null;
        weatherSource = null;
        weatherLocale = null;

        weather = null;
        notifyChange();
    }

    public boolean isValid() {
        return weather != null && weather.isValid();
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
