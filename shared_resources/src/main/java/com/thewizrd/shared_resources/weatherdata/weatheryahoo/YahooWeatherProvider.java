package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import android.content.Context;
import android.util.Log;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.utils.oauth.OAuthRequest;
import com.thewizrd.shared_resources.weatherdata.AstroDataProviderInterface;
import com.thewizrd.shared_resources.weatherdata.Astronomy;
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
import java.text.DecimalFormat;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YahooWeatherProvider extends WeatherProviderImpl implements AstroDataProviderInterface {
    private static final String QUERY_URL = "https://weather-ydn-yql.media.yahoo.com/forecastrss?%s&format=json&u=f";

    public YahooWeatherProvider() {
        super();
        locationProvider = new HERELocationProvider();
    }

    @Override
    public String getWeatherAPI() {
        return WeatherAPI.YAHOO;
    }

    @Override
    public boolean supportsWeatherLocale() {
        return false;
    }

    @Override
    public boolean isKeyRequired() {
        return false;
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
    public boolean isKeyValid(String key) {
        return false;
    }

    @Override
    public String getAPIKey() {
        return null;
    }

    private String getAppID() {
        return Keys.getYahooAppID();
    }

    private String getCliID() {
        return Keys.getYahooCliID();
    }

    private String getCliSecr() {
        return Keys.getYahooCliSecr();
    }

    private Rootobject getRootobject(String location_query) throws WeatherException {
        Rootobject root;
        WeatherException wEx = null;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        OAuthRequest authRequest = new OAuthRequest(getCliID(), getCliSecr());
        Response response = null;

        try {
            final String url = String.format(QUERY_URL, location_query);
            final String authorization = authRequest.getAuthorizationHeader(url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authorization)
                    .addHeader("X-Yahoo-App-Id", getAppID())
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load weather
            root = JSONParser.deserializer(stream, Rootobject.class);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            root = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "YahooWeatherProvider: error getting weather data");
        } finally {
            if (response != null)
                response.close();
        }

        if (wEx != null)
            throw wEx;

        return root;
    }

    @Override
    public Weather getWeather(final String location_query, final String country_code) throws WeatherException {
        Weather weather;
        WeatherException wEx = null;

        try {
            // Load weather
            Rootobject root = getRootobject(location_query);

            weather = new Weather(root);
        } catch (WeatherException ex) {
            weather = null;
            wEx = ex;
        } catch (Exception ex) {
            weather = null;
            Logger.writeLine(Log.ERROR, ex, "YahooWeatherProvider: error getting weather data");
        }

        if (wEx == null && (weather == null || !weather.isValid())) {
            wEx = new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        } else if (weather != null) {
            weather.setQuery(location_query);
        }

        if (wEx != null)
            throw wEx;

        return weather;
    }

    @Override
    public Weather getWeather(LocationData location) throws WeatherException {
        Weather weather = super.getWeather(location);

        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(location.getTzOffset()));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(location.getTzOffset()));

        Astronomy old = weather.getAstronomy();
        Astronomy newAstro = new SunMoonCalcProvider().getAstronomyData(location, weather.getCondition().getObservationTime());
        newAstro.setSunrise(old.getSunrise());
        newAstro.setSunset(old.getSunset());
        weather.setAstronomy(newAstro);

        return weather;
    }

    @Override
    public Astronomy getAstronomyData(LocationData location) throws WeatherException {
        try {
            String query = updateLocationQuery(location);
            Rootobject root = getRootobject(query);
            return new Astronomy(root.getCurrentObservation().getAstronomy());
        } catch (WeatherException wEx) {
            throw wEx;
        } catch (Exception e) {
            throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        }
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
    public String getWeatherIcon(String icon) {
        boolean isNight = false;

        try {
            int code = Integer.parseInt(icon);

            switch (code) {
                case 27: // Mostly Cloudy (Night)
                case 29: // Partly Cloudy (Night)
                case 31: // Clear (Night)
                case 33: // Fair (Night)
                    isNight = true;
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException ex) {
            // DO nothing
        }

        return getWeatherIcon(isNight, icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        try {
            int code = Integer.parseInt(icon);

            switch (code) {
                case 0: // Tornado
                    weatherIcon = WeatherIcons.TORNADO;
                    break;
                case 37: // isolated thunderstorms
                case 38: // Scattered Thunderstorms/showers
                case 39: // scattered showers (day)
                case 45: // scattered showers (night)
                case 47: // scattered thundershowers
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_STORM_SHOWERS;
                    else
                        weatherIcon = WeatherIcons.DAY_STORM_SHOWERS;
                    break;
                case 1: // Tropical Storm
                case 2: // Hurricane
                    weatherIcon = WeatherIcons.HURRICANE;
                    break;
                case 3: // severe thunderstorms
                case 4: // Scattered Thunderstorms
                    weatherIcon = WeatherIcons.THUNDERSTORM;
                    break;
                case 5: // Mixed Rain/Snow
                case 6: // Mixed Rain/Sleet
                case 7: // Mixed Snow/Sleet
                case 18: // Sleet
                case 35: // Mixed Rain/Hail
                    weatherIcon = WeatherIcons.RAIN_MIX;
                    break;
                case 8: // Freezing Drizzle
                case 10: // Freezing Rain
                case 17: // Hail
                    weatherIcon = WeatherIcons.HAIL;
                    break;
                case 9: // Drizzle
                case 11: // Showers
                case 12: // rain
                case 40: // Scattered Showers
                    weatherIcon = WeatherIcons.SHOWERS;
                    break;
                case 13: // Snow Flurries
                case 14: // Light Snow Showers
                case 16: // Snow
                case 42: // Scattered Snow Showers
                case 46: // Snow Showers
                    weatherIcon = WeatherIcons.SNOW;
                    break;
                case 15: // Blowing Snow
                case 41: // Heavy Snow
                case 43: // blizzard
                    weatherIcon = WeatherIcons.SNOW_WIND;
                    break;
                case 19: // Dust
                    weatherIcon = WeatherIcons.DUST;
                    break;
                case 20: // Foggy
                    weatherIcon = WeatherIcons.FOG;
                    break;
                case 21: // Haze
                    if (isNight)
                        weatherIcon = WeatherIcons.WINDY;
                    else
                        weatherIcon = WeatherIcons.DAY_HAZE;
                    break;
                case 22: // Smoky
                    weatherIcon = WeatherIcons.SMOKE;
                    break;
                case 23: // Blustery
                case 24: // Windy
                    weatherIcon = WeatherIcons.STRONG_WIND;
                    break;
                case 25: // Cold
                    weatherIcon = WeatherIcons.SNOWFLAKE_COLD;
                    break;
                case 26: // Cloudy
                    weatherIcon = WeatherIcons.CLOUDY;
                    break;
                case 27: // Mostly Cloudy (Night)
                case 28: // Mostly Cloudy (Day)
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
                    else
                        weatherIcon = WeatherIcons.DAY_CLOUDY;
                    break;
                case 31: // Clear (Night)
                case 32: // Sunny
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_CLEAR;
                    else
                        weatherIcon = WeatherIcons.DAY_SUNNY;
                    break;
                case 29: // Partly Cloudy (Night)
                case 30: // Partly Cloudy (Day)
                case 33: // Fair (Night)
                case 34: // Fair (Day)
                case 44: // Partly Cloudy
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
                    else
                        weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
                    break;
                case 36: // HOT
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_CLEAR;
                    else
                        weatherIcon = WeatherIcons.DAY_HOT;
                    break;
                case 3200: // Not Available
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

    @Override
    public String getWeatherCondition(String icon) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        try {
            int code = Integer.parseInt(icon);

            switch (code) {
                case 0: // Tornado
                    return context.getString(R.string.weather_tornado);
                case 37: // isolated thunderstorms
                    return context.getString(R.string.weather_isotstorms);
                case 39: // scattered showers (day)
                case 45: // scattered showers (night)
                    return context.getString(R.string.weather_scatteredshowers);
                case 38: // Scattered Thunderstorms/showers
                case 47: // scattered thundershowers
                case 4: // Scattered Thunderstorms
                    return context.getString(R.string.weather_scatteredtstorms);
                case 1: // Tropical Storm
                    return context.getString(R.string.weather_tropicalstorm);
                case 2: // Hurricane
                    return context.getString(R.string.weather_hurricane);
                case 3: // severe thunderstorms
                    return context.getString(R.string.weather_severetstorms);
                case 5: // Mixed Rain/Snow
                    return context.getString(R.string.weather_rainandsnow);
                case 6: // Mixed Rain/Sleet
                    return context.getString(R.string.weather_rainandsleet);
                case 7: // Mixed Snow/Sleet
                    return context.getString(R.string.weather_snowandsleet);
                case 18: // Sleet
                    return context.getString(R.string.weather_sleet);
                case 17: // Hail
                    return context.getString(R.string.weather_hail);
                case 35: // Mixed Rain/Hail
                    return context.getString(R.string.weather_rainandhail);
                case 8: // Freezing Drizzle
                case 10: // Freezing Rain
                    return context.getString(R.string.weather_freezingrain);
                case 9: // Drizzle
                case 12: // rain
                    return context.getString(R.string.weather_rain);
                case 11: // Showers
                case 40: // Scattered Showers
                    return context.getString(R.string.weather_rainshowers);
                case 13: // Snow Flurries
                    return context.getString(R.string.weather_snowflurries);
                case 16: // Snow
                case 46: // Snow Showers
                    return context.getString(R.string.weather_snow);
                case 14: // Light Snow Showers
                case 42: // Scattered Snow Showers
                    return context.getString(R.string.weather_lightsnowshowers);
                case 15: // Blowing Snow
                    return context.getString(R.string.weather_blowingsnow);
                case 41: // Heavy Snow
                    return context.getString(R.string.weather_heavysnow);
                case 43: // blizzard
                    return context.getString(R.string.weather_blizzard);
                case 19: // Dust
                    return context.getString(R.string.weather_dust);
                case 20: // Foggy
                    return context.getString(R.string.weather_foggy);
                case 21: // Haze
                    return context.getString(R.string.weather_haze);
                case 22: // Smoky
                    return context.getString(R.string.weather_smoky);
                case 23: // Blustery
                case 24: // Windy
                    return context.getString(R.string.weather_windy);
                case 25: // Cold
                    return context.getString(R.string.weather_cold);
                case 26: // Cloudy
                    return context.getString(R.string.weather_cloudy);
                case 27: // Mostly Cloudy (Night)
                case 28: // Mostly Cloudy (Day)
                    return context.getString(R.string.weather_mostlycloudy);
                case 29: // Partly Cloudy (Night)
                case 30: // Partly Cloudy (Day)
                case 44: // Partly Cloudy
                    return context.getString(R.string.weather_partlycloudy);
                case 31: // Clear (Night)
                    return context.getString(R.string.weather_clear);
                case 32: // Sunny
                    return context.getString(R.string.weather_sunny);
                case 33: // Fair (Night)
                case 34: // Fair (Day)
                    return context.getString(R.string.weather_fair);
                case 36: // HOT
                    return context.getString(R.string.weather_hot);
            }
        } catch (NumberFormatException ignored) {
        }

        return super.getWeatherCondition(icon);
    }

    // Some conditions can be for any time of day
    // So use sunrise/set data as fallback
    @Override
    public boolean isNight(Weather weather) {
        boolean isNight = super.isNight(weather);

        switch (weather.getCondition().getIcon()) {
            // The following cases can be present at any time of day
            case WeatherIcons.CLOUDY:
            case WeatherIcons.SNOWFLAKE_COLD:
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
