package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import android.util.Log;
import android.widget.Toast;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class YahooWeatherProvider extends WeatherProviderImpl {

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
    public Collection<LocationQueryViewModel> getLocations(String ac_query) {
        List<LocationQueryViewModel> locations = null;

        String yahooAPI = "https://query.yahooapis.com/v1/public/yql?q=";
        String query = "select * from geo.places where text=\"" + ac_query + "*\"";
        HttpURLConnection client = null;
        // Limit amount of results shown
        int maxResults = 10;

        try {
            // Connect to webstream
            URL queryURL = new URL(yahooAPI + query);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            locations = new ArrayList<>();
            Serializer deserializer = new Persister();
            AutoCompleteQuery root = deserializer.read(AutoCompleteQuery.class, stream, false);

            for (AutoCompleteQuery.Place result : root.getResults()) {
                // Filter: only store city results
                if ("Town".equals(result.getPlaceTypeName().getTextValue())
                        || "Suburb".equals(result.getPlaceTypeName().getTextValue())
                        || ("Zip Code".equals(result.getPlaceTypeName().getTextValue())
                        || "Postal Code".equals(result.getPlaceTypeName().getTextValue()) &&
                        (result.getLocality1() != null && "Town".equals(result.getLocality1().getType()))
                        || (result.getLocality1() != null && "Suburb".equals(result.getLocality1().getType()))))
                    locations.add(new LocationQueryViewModel(result));
                else
                    continue;

                // Limit amount of results
                maxResults--;
                if (maxResults <= 0)
                    break;
            }

            // End Stream
            stream.close();
        } catch (Exception ex) {
            locations = new ArrayList<>();
            Logger.writeLine(Log.ERROR, ex, "YahooWeatherProvider: error getting locations");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (locations == null || locations.size() == 0) {
            locations = Collections.singletonList(new LocationQueryViewModel());
        }

        return locations;
    }

    @Override
    public LocationQueryViewModel getLocation(WeatherUtils.Coordinate coord) {
        LocationQueryViewModel location = null;

        String yahooAPI = "https://query.yahooapis.com/v1/public/yql?q=";
        String location_query = String.format(Locale.ROOT, "(%f,%f)", coord.getLatitude(), coord.getLongitude());
        String query = "select * from geo.places where text=\"" + location_query + "\"";
        HttpURLConnection client = null;
        AutoCompleteQuery.Place result = null;
        WeatherException wEx = null;

        try {
            // Connect to webstream
            URL queryURL = new URL(yahooAPI + query);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            Serializer deserializer = new Persister();
            AutoCompleteQuery root = deserializer.read(AutoCompleteQuery.class, stream, false);

            if (root.getResults() != null)
                result = root.getResults().get(0);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
                final WeatherException finalWEx = wEx;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SimpleLibrary.getInstance().getApp().getAppContext(), finalWEx.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            Logger.writeLine(Log.ERROR, ex, "YahooWeatherProvider: error getting location");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (result != null && !StringUtils.isNullOrWhitespace(result.getWoeid()))
            location = new LocationQueryViewModel(result);
        else
            location = new LocationQueryViewModel();

        return location;
    }

    @Override
    public LocationQueryViewModel getLocation(String location_query) {
        LocationQueryViewModel location = null;

        String yahooAPI = "https://query.yahooapis.com/v1/public/yql?q=";
        String query = "select * from geo.places where woeid=\"" + location_query + "\"";
        HttpURLConnection client = null;
        AutoCompleteQuery.Place result = null;
        WeatherException wEx = null;

        try {
            // Connect to webstream
            URL queryURL = new URL(yahooAPI + query);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            Serializer deserializer = new Persister();
            AutoCompleteQuery root = deserializer.read(AutoCompleteQuery.class, stream, false);

            if (root.getResults() != null)
                result = root.getResults().get(0);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            result = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
                final WeatherException finalWEx = wEx;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SimpleLibrary.getInstance().getApp().getAppContext(), finalWEx.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            Logger.writeLine(Log.ERROR, ex, "YahooWeatherProvider: error getting location");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (result != null && !StringUtils.isNullOrWhitespace(result.getWoeid()))
            location = new LocationQueryViewModel(result);
        else
            location = new LocationQueryViewModel();

        return location;
    }

    @Override
    public boolean isKeyValid(String key) {
        return false;
    }

    @Override
    public String getAPIKey() {
        return null;
    }

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        String queryAPI = null;
        URL weatherURL = null;
        HttpURLConnection client = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());
        WeatherException wEx = null;

        try {
            try {
                int woeid = Integer.parseInt(location_query);
                queryAPI = "https://query.yahooapis.com/v1/public/yql?q=";
                String query = "select * from weather.forecast where woeid=\""
                        + woeid + "\" and u='F'&format=json";
                weatherURL = new URL(queryAPI + query);
            } catch (NumberFormatException ex) {
                queryAPI = "https://query.yahooapis.com/v1/public/yql?q=";
                String query = "select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\""
                        + location_query + "\") and u='F'&format=json";
                weatherURL = new URL(queryAPI + query);
            }

            client = (HttpURLConnection) weatherURL.openConnection();
            InputStream stream = client.getInputStream();

            // Reset exception
            wEx = null;

            // Load weather
            Rootobject root = null;
            root = (Rootobject) JSONParser.deserializer(stream, Rootobject.class);

            // End Stream
            stream.close();

            weather = new Weather(root);
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
                final WeatherException finalWEx = wEx;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SimpleLibrary.getInstance().getApp().getAppContext(), finalWEx.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            Logger.writeLine(Log.ERROR, ex, "YahooWeatherProvider: error getting weather data");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (weather == null || !weather.isValid()) {
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

        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(location.getTzOffset()));

        return weather;
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        String query = "";
        String coord = String.format(Locale.ROOT, "%s,%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
        LocationQueryViewModel qview = getLocation(new WeatherUtils.Coordinate(coord));

        if (StringUtils.isNullOrEmpty(qview.getLocationQuery()))
            query = String.format("(%s)", coord);
        else
            query = qview.getLocationQuery();

        return query;
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        String query = "";
        String coord = String.format(Locale.ROOT, "%s,%s", location.getLatitude(), location.getLongitude());
        LocationQueryViewModel qview = getLocation(new WeatherUtils.Coordinate(coord));

        if (StringUtils.isNullOrEmpty(qview.getLocationQuery()))
            query = String.format("(%s)", coord);
        else
            query = qview.getLocationQuery();

        return query;
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
                case 1: // Tropical Storm
                case 37:
                case 38: // Scattered Thunderstorms/showers
                case 39:
                case 45:
                case 47:
                    if (isNight)
                        weatherIcon = WeatherIcons.NIGHT_ALT_STORM_SHOWERS;
                    else
                        weatherIcon = WeatherIcons.DAY_STORM_SHOWERS;
                    break;
                case 2: // Hurricane
                    weatherIcon = WeatherIcons.HURRICANE;
                    break;
                case 3:
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
                case 12:
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
                case 43:
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
                case 29: // Partly Cloudy (Night)
                case 30: // Partly Cloudy (Day)
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
                    ZoneId id = ZoneId.of(weather.getLocation().getTzLong());
                    ZoneOffset tz = id.getRules().getOffset(Instant.now());
                    if (tz == null)
                        tz = weather.getLocation().getTzOffset();

                    LocalTime sunrise = weather.getAstronomy().getSunrise().toLocalTime();
                    LocalTime sunset = weather.getAstronomy().getSunset().toLocalTime();

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
