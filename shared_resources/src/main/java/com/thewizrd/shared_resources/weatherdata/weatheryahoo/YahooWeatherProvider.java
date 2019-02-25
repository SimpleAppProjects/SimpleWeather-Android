package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import android.util.Log;
import android.widget.Toast;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

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
import java.net.URLEncoder;
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

        String queryAPI = "https://autocomplete.wunderground.com/aq?query=";
        String options = "&h=0&cities=1";
        HttpURLConnection client = null;
        // Limit amount of results shown
        int maxResults = 10;

        try {
            // Connect to webstream
            URL queryURL = new URL(queryAPI + URLEncoder.encode(ac_query, "UTF-8") + options);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            locations = new ArrayList<>();
            AC_Rootobject root = JSONParser.deserializer(stream, AC_Rootobject.class);

            for (AC_RESULTS result : root.getRESULTS()) {
                // Filter: only store city results
                if (!result.getType().equals("city"))
                    continue;

                locations.add(new LocationQueryViewModel(result));

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

        String queryAPI = "https://api.wunderground.com/auto/wui/geo/GeoLookupXML/index.xml?query=";
        String options = "";
        String query = String.format(Locale.ROOT, "%s,%s", coord.getLatitude(), coord.getLongitude());
        HttpURLConnection client = null;
        GeoLocation result = null;
        WeatherException wEx = null;

        try {
            // Connect to webstream
            URL queryURL = new URL(queryAPI + query + options);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            Serializer deserializer = new Persister();
            result = deserializer.read(GeoLocation.class, stream, false);

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

        if (result != null && !StringUtils.isNullOrWhitespace(result.getQuery()))
            location = new LocationQueryViewModel(result);
        else
            location = new LocationQueryViewModel();

        return location;
    }

    @Override
    public LocationQueryViewModel getLocation(String location_query) {
        LocationQueryViewModel location = null;

        String queryAPI = "https://autocomplete.wunderground.com/aq?query=";
        String options = "&h=0&cities=1";
        HttpURLConnection client = null;
        AC_RESULTS result = null;
        WeatherException wEx = null;

        try {
            // Connect to webstream
            URL queryURL = new URL(queryAPI + URLEncoder.encode(location_query, "UTF-8") + options);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            AC_Rootobject root = JSONParser.deserializer(stream, AC_Rootobject.class);
            result = root.getRESULTS().get(0);

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

        if (result != null && !StringUtils.isNullOrWhitespace(result.getL()))
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

    private String getAppID() {
        return Keys.getYahooAppID();
    }

    private String getCliID() {
        return Keys.getYahooCliID();
    }

    private String getCliSecr() {
        return Keys.getYahooCliSecr();
    }

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        String queryAPI = "https://weather-ydn-yql.media.yahoo.com/forecastrss";
        URL weatherURL = null;
        HttpURLConnection client = null;

        OAuthConsumer consumer = new OAuthConsumer(null, getCliID(), getCliSecr(), null);
        consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
        OAuthAccessor accessor = new OAuthAccessor(consumer);

        WeatherException wEx = null;

        try {
            String query = "?" + location_query + "&format=json&u=f";
            weatherURL = new URL(queryAPI + query);

            OAuthMessage request = accessor.newRequestMessage(OAuthMessage.GET, weatherURL.toString(), null);
            String authorization = request.getAuthorizationHeader(null);

            client = (HttpURLConnection) weatherURL.openConnection();
            // Add headers to request
            client.addRequestProperty("Authorization", authorization);
            client.addRequestProperty("Yahoo-App-Id", getAppID());
            client.addRequestProperty("Content-Type", "application/json");

            InputStream stream = client.getInputStream();

            // Reset exception
            wEx = null;

            // Load weather
            Rootobject root = null;
            root = JSONParser.deserializer(stream, Rootobject.class);

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

    // Use location name here instead of query since we use the AutoComplete API
    @Override
    public void updateLocationData(LocationData location) {
        LocationQueryViewModel qview = getLocation(location.getName());

        if (qview != null && !StringUtils.isNullOrWhitespace(qview.getLocationQuery())) {
            location.setName(qview.getLocationName());
            location.setLatitude(qview.getLocationLat());
            location.setLongitude(qview.getLocationLong());
            location.setTzLong(qview.getLocationTZLong());

            // Update DB here or somewhere else
            if (SimpleLibrary.getInstance().getApp().isPhone()) {
                Settings.updateLocation(location);
            } else {
                Settings.saveHomeData(location);
            }
        }
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        return String.format(Locale.ROOT, "lat=%s&lon=%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        return String.format(Locale.ROOT, "lat=%s&lon=%s", Double.toString(location.getLatitude()), Double.toString(location.getLongitude()));
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
