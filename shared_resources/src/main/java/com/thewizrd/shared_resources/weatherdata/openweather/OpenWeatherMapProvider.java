package com.thewizrd.shared_resources.weatherdata.openweather;

import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.utils.ExceptionUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Astronomy;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.util.Locale;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OpenWeatherMapProvider extends WeatherProviderImpl {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String KEYCHECK_QUERY_URL = BASE_URL + "forecast?appid=%s";
    private static final String CURRENT_QUERY_URL = BASE_URL + "weather?%s&appid=%s&lang=%s";
    private static final String FORECAST_QUERY_URL = BASE_URL + "forecast?%s&appid=%s&lang=%s";

    public OpenWeatherMapProvider() {
        super();

        locationProvider = RemoteConfig.getLocationProvider(getWeatherAPI());
        if (locationProvider == null) {
            locationProvider = new GoogleLocationProvider();
        }
    }

    @Override
    public String getWeatherAPI() {
        return WeatherAPI.OPENWEATHERMAP;
    }

    @Override
    public boolean supportsWeatherLocale() {
        return true;
    }

    @Override
    public boolean isKeyRequired() {
        return true;
    }

    @Override
    public boolean supportsAlerts() {
        return true;
    }

    @Override
    public boolean needsExternalAlertData() {
        return true;
    }

    @Override
    public boolean isKeyValid(String key) throws WeatherException {
        if (StringUtils.isNullOrWhitespace(key)) {
            throw new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
        }

        boolean isValid = false;
        WeatherException wEx = null;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Request request = new Request.Builder()
                    .url(String.format(KEYCHECK_QUERY_URL, key))
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();

            // Check for errors
            switch (response.code()) {
                // 400 (OK since this isn't a valid request)
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    isValid = true;
                    break;
                // 401 (Unauthorized - Key is invalid)
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                    isValid = false;
                    break;
            }
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                wEx = ExceptionUtils.copyStackTrace(new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR), ex);
            }

            isValid = false;
        } finally {
            if (response != null)
                response.close();
        }

        if (wEx != null) {
            throw wEx;
        }

        return isValid;
    }

    @Override
    public String getAPIKey() {
        return Keys.getOWMKey();
    }

    @Override
    public Weather getWeather(final String location_query, final String country_code) throws WeatherException {
        Weather weather;

        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String query;
        try {
            query = String.format(Locale.ROOT, "id=%d", Integer.parseInt(location_query));
        } catch (NumberFormatException ex) {
            query = location_query;
        }

        String key = Settings.usePersonalKey() ? Settings.getAPIKEY() : getAPIKey();

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response currentResponse = null, forecastResponse = null;
        WeatherException wEx = null;

        try {
            Request currentRequest = new Request.Builder()
                    .url(String.format(CURRENT_QUERY_URL, query, key, locale))
                    .build();
            Request forecastRequest = new Request.Builder()
                    .url(String.format(FORECAST_QUERY_URL, query, key, locale))
                    .build();

            // Connect to webstream
            currentResponse = client.newCall(currentRequest).execute();
            forecastResponse = client.newCall(forecastRequest).execute();
            final InputStream currentStream = currentResponse.body().byteStream();
            final InputStream forecastStream = forecastResponse.body().byteStream();

            // Load weather
            CurrentRootobject currRoot = JSONParser.deserializer(currentStream, CurrentRootobject.class);
            ForecastRootobject foreRoot = JSONParser.deserializer(forecastStream, ForecastRootobject.class);

            // End Stream
            currentStream.close();
            forecastStream.close();

            weather = new Weather(currRoot, foreRoot);
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting weather data");
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

        // OWM reports datetime in UTC; add location tz_offset
        ZoneOffset offset = location.getTzOffset();
        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(offset));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(offset));
        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            hr_forecast.setDate(hr_forecast.getDate().withZoneSameInstant(offset));
        }
        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
        }
        weather.getAstronomy().setSunrise(weather.getAstronomy().getSunrise().plusSeconds(offset.getTotalSeconds()));
        weather.getAstronomy().setSunset(weather.getAstronomy().getSunset().plusSeconds(offset.getTotalSeconds()));

        Astronomy old = weather.getAstronomy();
        Astronomy newAstro = new SunMoonCalcProvider().getAstronomyData(location, weather.getCondition().getObservationTime());
        newAstro.setSunrise(old.getSunrise());
        newAstro.setSunset(old.getSunset());
        weather.setAstronomy(newAstro);

        return weather;
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(weather.getLocation().getLatitude()), df.format(weather.getLocation().getLongitude()));
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.getLatitude()), df.format(location.getLongitude()));
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        String code = "en";

        switch (iso) {
            // Arabic
            case "ar":
                // Bulgarian
            case "bg":
                // Catalan
            case "ca":
                // Croatian
            case "hr":
                // Dutch
            case "nl":
                // Farsi / Persian
            case "fa":
                // Finnish
            case "fi":
                // French
            case "fr":
                // Galician
            case "gl":
                // German
            case "de":
                // Greek
            case "el":
                // Hungarian
            case "hu":
                // Italian
            case "it":
                // Japanese
            case "ja":
                // Lithuanian
            case "lt":
                // Macedonian
            case "mk":
                // Polish
            case "pl":
                // Portuguese
            case "pt":
                // Romanian
            case "ro":
                // Russian
            case "ru":
                // Slovak
            case "sk":
                // Slovenian
            case "sl":
                // Spanish
            case "es":
                // Turkish
            case "tr":
                // Vietnamese
            case "vi":
                code = iso;
                break;
            // Chinese
            case "zh":
                switch (name) {
                    // Chinese - Traditional
                    case "zh-Hant":
                    case "zh-HK":
                    case "zh-MO":
                    case "zh-TW":
                        code = "zh_tw";
                        break;
                    // Chinese - Simplified
                    default:
                        code = "zh_cn";
                        break;
                }
                break;
            // Czech
            case "cs":
                code = "cz";
                break;
            // Korean
            case "ko":
                code = "kr";
                break;
            // Latvian
            case "lv":
                code = "la";
                break;
            // Swedish
            case "sv":
                code = "se";
                break;
            // Ukrainian
            case "uk":
                code = "ua";
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
        boolean isNight = false;

        if (icon.endsWith("n"))
            isNight = true;

        return getWeatherIcon(isNight, icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        switch (icon.substring(0, 3)) {
            case "200": // thunderstorm w/ light rain
            case "201": // thunderstorm w/ rain
            case "210": // light thunderstorm
            case "230": // thunderstorm w/ light drizzle
            case "231": // thunderstorm w/ drizzle
            case "531": // ragged shower rain
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_STORM_SHOWERS;
                else
                    weatherIcon = WeatherIcons.DAY_STORM_SHOWERS;
                break;

            case "211": // thunderstorm
            case "212": // heavy thunderstorm
            case "221": // ragged thunderstorm
            case "202": // thunderstorm w/ heavy rain
            case "232": // thunderstorm w/ heavy drizzle
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM;
                else
                    weatherIcon = WeatherIcons.DAY_THUNDERSTORM;
                break;

            case "300": // light intensity drizzle
            case "301": // drizzle
            case "321": // shower drizzle
            case "500": // light rain
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SPRINKLE;
                else
                    weatherIcon = WeatherIcons.DAY_SPRINKLE;
                break;

            case "302": // heavy intensity drizzle
            case "311": // drizzle rain
            case "312": // heavy intensity drizzle rain
            case "314": // heavy shower rain and drizzle
            case "501": // moderate rain
            case "502": // heavy intensity rain
            case "503": // very heavy rain
            case "504": // extreme rain
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_RAIN;
                else
                    weatherIcon = WeatherIcons.DAY_RAIN;
                break;

            case "310": // light intensity drizzle rain
            case "511": // freezing rain
            case "611": // sleet
            case "612": // shower sleet
            case "615": // light rain and snow
            case "616": // rain and snow
            case "620": // light shower snow
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_RAIN_MIX;
                else
                    weatherIcon = WeatherIcons.DAY_RAIN_MIX;
                break;

            case "313": // shower rain and drizzle
            case "520": // light intensity shower rain
            case "521": // shower rain
            case "522": // heavy intensity shower rain
            case "701": // mist
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SHOWERS;
                else
                    weatherIcon = WeatherIcons.DAY_SHOWERS;
                break;

            case "600": // light snow
            case "601": // snow
            case "621": // shower snow
            case "622": // heavy shower snow
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SNOW;
                else
                    weatherIcon = WeatherIcons.DAY_SNOW;
                break;

            case "602": // heavy snow
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_WIND;
                else
                    weatherIcon = WeatherIcons.DAY_SNOW_WIND;
                break;

            // smoke
            case "711":
                weatherIcon = WeatherIcons.SMOKE;
                break;

            // haze
            case "721":
                if (isNight)
                    weatherIcon = WeatherIcons.WINDY;
                else
                    weatherIcon = WeatherIcons.DAY_HAZE;
                break;

            // dust
            case "731":
            case "761":
            case "762":
                weatherIcon = WeatherIcons.DUST;
                break;

            // fog
            case "741":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_FOG;
                else
                    weatherIcon = WeatherIcons.DAY_FOG;
                break;

            // cloudy-gusts
            case "771": // squalls
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS;
                else
                    weatherIcon = WeatherIcons.DAY_CLOUDY_GUSTS;
                break;

            // tornado
            case "781":
            case "900":
                weatherIcon = WeatherIcons.TORNADO;
                break;

            // day-sunny
            case "800": // clear sky
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_CLEAR;
                else
                    weatherIcon = WeatherIcons.DAY_SUNNY;
                break;

            case "801": // few clouds
            case "802": // scattered clouds
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
                else
                    weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
                break;

            case "803": // broken clouds
            case "804": // overcast clouds
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
                else
                    weatherIcon = WeatherIcons.DAY_CLOUDY;
                break;

            case "901": // tropical storm
            case "902": // hurricane
                weatherIcon = WeatherIcons.HURRICANE;
                break;

            // cold
            case "903":
                weatherIcon = WeatherIcons.SNOWFLAKE_COLD;
                break;

            // hot
            case "904":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_CLEAR;
                else
                    weatherIcon = WeatherIcons.DAY_HOT;
                break;

            // windy
            case "905":
                weatherIcon = WeatherIcons.WINDY;
                break;

            // hail
            case "906":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_HAIL;
                else
                    weatherIcon = WeatherIcons.DAY_HAIL;
                break;

            // strong wind
            case "957":
                weatherIcon = WeatherIcons.STRONG_WIND;
                break;

            default:
                break;
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

        switch (weather.getCondition().getIcon()) {
            // The following cases can be present at any time of day
            case WeatherIcons.SMOKE:
            case WeatherIcons.WINDY:
            case WeatherIcons.DUST:
            case WeatherIcons.TORNADO:
            case WeatherIcons.HURRICANE:
            case WeatherIcons.SNOWFLAKE_COLD:
            case WeatherIcons.HAIL:
            case WeatherIcons.STRONG_WIND:
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
                break;
            default:
                break;
        }

        return isNight;
    }
}
