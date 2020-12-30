package com.thewizrd.shared_resources.weatherdata.weatherunlocked;

import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class WeatherUnlockedProvider extends WeatherProviderImpl {
    private static final String BASE_URL = "http://api.weatherunlocked.com/api/";
    private static final String CURRENT_QUERY_URL = BASE_URL + "current/%s?app_id=%s&app_key=%s&lang=%s";
    private static final String FORECAST_QUERY_URL = BASE_URL + "forecast/%s?app_id=%s&app_key=%s&lang=%s";

    public WeatherUnlockedProvider() {
        super();

        locationProvider = RemoteConfig.getLocationProvider(getWeatherAPI());
        if (locationProvider == null) {
            locationProvider = new GoogleLocationProvider();
        }
    }

    @Override
    public String getWeatherAPI() {
        return WeatherAPI.WEATHERUNLOCKED;
    }

    @Override
    public boolean supportsWeatherLocale() {
        return true;
    }

    @Override
    public boolean isKeyRequired() {
        return false;
    }

    @Override
    public boolean supportsAlerts() {
        return false;
    }

    @Override
    public boolean needsExternalAlertData() {
        return true;
    }

    @Override
    public boolean isKeyValid(String key) {
        return false;
    }

    @Override
    public String getAPIKey() {
        return null;
    }

    private String getAppID() {
        return Keys.getWUnlockedAppID();
    }

    private String getAppKey() {
        return Keys.getWUnlockedKey();
    }

    @Override
    public Weather getWeather(final String location_query, final String country_code) throws WeatherException {
        Weather weather;

        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response currentResponse = null, forecastResponse = null;
        WeatherException wEx = null;

        try {
            Request currentRequest = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build())
                    .addHeader("Accept", "application/json")
                    .url(String.format(CURRENT_QUERY_URL, location_query, getAppID(), getAppKey(), locale))
                    .build();
            Request forecastRequest = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build())
                    .addHeader("Accept", "application/json")
                    .url(String.format(FORECAST_QUERY_URL, location_query, getAppID(), getAppKey(), locale))
                    .build();

            // Connect to webstream
            currentResponse = client.newCall(currentRequest).execute();
            forecastResponse = client.newCall(forecastRequest).execute();
            final InputStream currentStream = currentResponse.body().byteStream();
            final InputStream forecastStream = forecastResponse.body().byteStream();

            // Load weather
            CurrentResponse currRoot = JSONParser.deserializer(currentStream, CurrentResponse.class);
            ForecastResponse foreRoot = JSONParser.deserializer(forecastStream, ForecastResponse.class);

            // End Stream
            currentStream.close();
            forecastStream.close();

            weather = new Weather(currRoot, foreRoot);
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "WeatherUnlockedProvider: error getting weather data");
        } finally {
            if (currentResponse != null)
                currentResponse.close();
            if (forecastResponse != null)
                forecastResponse.close();
        }

        if (wEx == null && (weather == null || !weather.isValid())) {
            wEx = new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        } else if (weather != null) {
            if (supportsWeatherLocale())
                weather.setLocale(locale);

            weather.setQuery(location_query);
        }

        if (wEx != null)
            throw wEx;

        return weather;
    }

    @Override
    public Weather getWeather(LocationData location) throws WeatherException {
        Weather weather = super.getWeather(location);

        ZoneOffset offset = location.getTzOffset();
        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(offset));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(offset));

        weather.setAstronomy(new SunMoonCalcProvider().getAstronomyData(location, weather.getCondition().getObservationTime()));

        // Update icons
        LocalTime now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime();
        LocalTime sunrise = weather.getAstronomy().getSunrise().toLocalTime();
        LocalTime sunset = weather.getAstronomy().getSunset().toLocalTime();

        weather.getCondition().setIcon(getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.getCondition().getIcon()));

        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            ZonedDateTime hrf_date = hr_forecast.getDate().withZoneSameInstant(offset);
            hr_forecast.setDate(hrf_date);

            LocalTime hrf_localTime = hrf_date.toLocalTime();
            hr_forecast.setIcon(getWeatherIcon(hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset), hr_forecast.getIcon()));
        }

        return weather;
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.##");
        return String.format(Locale.ROOT, "%s,%s", df.format(weather.getLocation().getLatitude()), df.format(weather.getLocation().getLongitude()));
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.##");
        return String.format(Locale.ROOT, "%s,%s", df.format(location.getLatitude()), df.format(location.getLongitude()));
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        String code = "en";

        switch (iso) {
            // Danish
            case "da":
                // French
            case "fr":
                // Italian
            case "it":
                // German
            case "de":
                // Dutch
            case "nl":
                // Spanish
            case "es":
                // Norwegian
            case "no":
                // Swedish
            case "sv":
                // Turkish
            case "tr":
                // Bulgarian
            case "bg":
                // Czech
            case "cs":
                // Hungarian
            case "hu":
                // Polish
            case "pl":
                // Russian
            case "ru":
                // Slovak
            case "sk":
                code = iso;
                break;
            // Romanian
            case "ro":
                code = "rm";
                break;
            default:
                // Default is English
                code = "en";
                break;
        }

        return code;
    }

    @Override
    public String getWeatherIcon(String icon) {
        return getWeatherIcon(false, icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        try {
            int code = Integer.parseInt(icon);

            switch (code) {
                case 0: // Sunny skies/Clear skies
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_CLEAR;
                    else
                        weatherIcon = WeatherIcons.DAY_SUNNY;
                    break;
                case 1: // Partly cloudy skies
                case 3: // Cloudy skies
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
                    else
                        weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
                    break;
                case 2: // Cloudy skies
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
                    else
                        weatherIcon = WeatherIcons.DAY_CLOUDY;
                    break;
                case 10: // Haze
                case 45: // Fog
                case 49: // Freezing fog
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_FOG;
                    else
                        weatherIcon = WeatherIcons.DAY_FOG;
                    break;
                case 21: // Patchy rain possible
                case 50: // Patchy light drizzle
                case 51: // Light drizzle
                case 61: // Light rain
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_SPRINKLE;
                    else
                        weatherIcon = WeatherIcons.DAY_SPRINKLE;
                    break;
                case 22: // Patchy snow possible
                case 70: // Patchy snow possible
                case 71: // Light snow
                case 72: // Patchy moderate snow
                case 73: // Moderate snow
                case 85: // Light snow showers
                case 86: // Moderate or heavy snow showers
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_SNOW;
                    else
                        weatherIcon = WeatherIcons.DAY_SNOW;
                    break;
                case 23: // Patchy sleet possible
                case 24: // Patchy freezing drizzle possible
                case 56: // Freezing drizzle
                case 57: // Heavy freezing drizzle
                case 79: // Ice pellets
                case 83: // Light sleet showers
                case 84: // Moderate or heavy sleet showers
                case 87: // Light showers of ice pellets
                case 88: // Moderate or heavy showers of ice pellets
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_SLEET;
                    else
                        weatherIcon = WeatherIcons.DAY_SLEET;
                    break;
                case 29: // Thundery outbreaks possible
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_LIGHTNING;
                    else
                        weatherIcon = WeatherIcons.DAY_LIGHTNING;
                    break;
                case 38: // Blowing snow
                case 39: // Blizzard
                case 74: // Patchy heavy snow
                case 75: // Heavy snow
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_WIND;
                    else
                        weatherIcon = WeatherIcons.DAY_SNOW_WIND;
                    break;
                case 62: // Moderate rain at times
                case 63: // Moderate rain
                case 64: // Heavy rain at times
                case 65: // Heavy rain
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_RAIN;
                    else
                        weatherIcon = WeatherIcons.DAY_RAIN;
                    break;
                case 66: // Light freezing rain
                case 67: // Moderate or heavy freezing rain
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_RAIN_MIX;
                    else
                        weatherIcon = WeatherIcons.DAY_RAIN_MIX;
                    break;
                case 80: // Light rain shower
                case 81: // Moderate or heavy rain shower
                case 82: // Torrential rain shower
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_SHOWERS;
                    else
                        weatherIcon = WeatherIcons.DAY_SHOWERS;
                    break;
                case 91: // Patchy light rain with thunder
                case 92: // Moderate or heavy rain with thunder
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM;
                    else
                        weatherIcon = WeatherIcons.DAY_THUNDERSTORM;
                    break;
                case 93: // Patchy light snow with thunder
                case 94: // Moderate or heavy snow with thunder
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM;
                    else
                        weatherIcon = WeatherIcons.DAY_SNOW_THUNDERSTORM;
                    break;
                default:
                    weatherIcon = WeatherIcons.NA;
                    break;
            }
        } catch (NumberFormatException ex) {
            // DO nothing
        }

        if (StringUtils.isNullOrWhitespace(weatherIcon)) {
            // Not Available
            weatherIcon = WeatherIcons.NA;
        }

        return weatherIcon;
    }

    // Some conditions can be for any time of day
    // So use sunrise/set data as fallback
    @Override
    public boolean isNight(Weather weather) {
        boolean isNight = super.isNight(weather);

        if (!isNight) {
            // Fallback to sunset/rise time just in case
            ZoneOffset tz = null;
            if (!StringUtils.isNullOrWhitespace(weather.getLocation().getTzLong())) {
                ZoneId id = ZoneId.of(weather.getLocation().getTzLong());
                tz = id.getRules().getOffset(Instant.now());
            }
            if (tz == null) {
                tz = weather.getLocation().getTzOffset();
            }

            LocalTime sunrise;
            LocalTime sunset;
            if (weather.getAstronomy() != null) {
                sunrise = weather.getAstronomy().getSunrise().toLocalTime();
                sunset = weather.getAstronomy().getSunset().toLocalTime();
            } else {
                sunrise = LocalTime.of(6, 0);
                sunset = LocalTime.of(18, 0);
            }

            LocalTime now = ZonedDateTime.now(tz).toLocalTime();

            // Determine whether its night using sunset/rise times
            if (now.toNanoOfDay() < sunrise.toNanoOfDay() || now.toNanoOfDay() > sunset.toNanoOfDay())
                isNight = true;
        }

        return isNight;
    }
}
