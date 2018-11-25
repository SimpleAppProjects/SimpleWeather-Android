package com.thewizrd.shared_resources.weatherdata.here;

import android.util.Log;
import android.widget.Toast;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.shared_resources.weatherdata.nws.NWSAlertProvider;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.threeten.bp.ZoneOffset;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HEREWeatherProvider extends WeatherProviderImpl {

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
        return false;
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
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting locations");
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
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting location");
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
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting location");
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
        String queryAPI = "https://weather.cit.api.here.com/weather/1.0/report.json";
        String app_id = key.split(";")[0];
        String app_code = key.split(";")[key.split(";").length - 1];
        HttpURLConnection client = null;
        boolean isValid = false;
        WeatherException wEx = null;

        try {
            if (StringUtils.isNullOrWhitespace(app_id) || StringUtils.isNullOrWhitespace(app_code)) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                throw wEx;
            }

            // Connect to webstream
            URL queryURL = new URL(String.format("%s?app_id=%s&app_code=%s", queryAPI, app_id, app_code));
            client = (HttpURLConnection) queryURL.openConnection();

            // Check for errors
            switch (client.getResponseCode()) {
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
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            isValid = false;
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (wEx != null) {
            final WeatherException finalWEx = wEx;
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SimpleLibrary.getInstance().getApp().getAppContext(), finalWEx.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        return isValid;
    }

    private String getAppID() {
        return Keys.getHEREAppID();
    }

    private String getAppCode() {
        return Keys.getHEREAppCode();
    }

    @Override
    public String getAPIKey() {
        if (StringUtils.isNullOrWhitespace(getAppID()) && StringUtils.isNullOrWhitespace(getAppCode()))
            return null;
        else
            return String.format("%s;%s", getAppID(), getAppCode());
    }

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        String queryAPI = null;
        URL weatherURL = null;
        HttpURLConnection client = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        queryAPI = "https://weather.cit.api.here.com/weather/1.0/report.json?product=alerts&product=forecast_7days_simple" +
                "&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true&%s" +
                "&language=%s&metric=false&app_id=%s&app_code=%s";

        String key = Settings.usePersonalKey() ? Settings.getAPIKEY() : getAPIKey();
        String app_id = key.split(";")[0];
        String app_code = key.split(";")[key.split(";").length - 1];

        WeatherException wEx = null;

        try {
            weatherURL = new URL(String.format(queryAPI, location_query, locale, app_id, app_code));

            client = (HttpURLConnection) weatherURL.openConnection();
            InputStream stream = client.getInputStream();

            // Reset exception
            wEx = null;

            // Load weather
            Rootobject root = null;
            root = (Rootobject) JSONParser.deserializer(stream, Rootobject.class);

            // Check for errors
            if (root.getType() != null) {
                switch (root.getType()) {
                    case "Invalid Request":
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
                        break;
                    case "Unauthorized":
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                        break;
                    default:
                        break;
                }
            }

            // End Stream
            stream.close();

            weather = new Weather(root);

            // Add weather alerts if available
            if (root.getAlerts() != null && root.getAlerts().getAlerts().size() > 0) {
                if (weather.getWeatherAlerts() == null)
                    weather.setWeatherAlerts(new ArrayList<WeatherAlert>());

                for (AlertsItem result : root.getAlerts().getAlerts()) {
                    weather.getWeatherAlerts().add(new WeatherAlert(result));
                }
            }
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
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting weather data");
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

        ZoneOffset offset = location.getTzOffset();

        if (weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
            for (WeatherAlert alert : weather.getWeatherAlerts()) {
                if (!alert.getDate().getOffset().equals(offset)) {
                    alert.setDate(alert.getDate().withZoneSameLocal(offset));
                }

                if (!alert.getExpiresDate().getOffset().equals(offset)) {
                    alert.setExpiresDate(alert.getExpiresDate().withZoneSameLocal(offset));
                }
            }
        } else if ((weather.getWeatherAlerts() == null || weather.getWeatherAlerts().size() == 0) && "US".equals(location.getCountryCode())) {
            weather.setWeatherAlerts(new NWSAlertProvider().getAlerts(location));
        }

        // Update tz for weather properties
        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(location.getTzOffset()));

        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
        }

        return weather;
    }

    @Override
    public List<WeatherAlert> getAlerts(LocationData location) {
        List<WeatherAlert> alerts = null;

        String queryAPI = null;
        URL weatherURL = null;
        HttpURLConnection client = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        queryAPI = "https://weather.cit.api.here.com/weather/1.0/report.json?product=alerts&%s" +
                "&language=%s&metric=false&app_id=%s&app_code=%s";

        String key = Settings.usePersonalKey() ? Settings.getAPIKEY() : getAPIKey();
        String app_id = key.split(";")[0];
        String app_code = key.split(";")[key.split(";").length - 1];

        try {
            weatherURL = new URL(String.format(queryAPI, location.getQuery(), locale, app_id, app_code));

            client = (HttpURLConnection) weatherURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            alerts = new ArrayList<>();

            Rootobject root = null;
            // TODO: async task it
            root = (Rootobject) JSONParser.deserializer(stream, Rootobject.class);

            for (AlertsItem result : root.getAlerts().getAlerts()) {
                alerts.add(new WeatherAlert(result));
            }

            // End Stream
            stream.close();
        } catch (Exception ex) {
            alerts = new ArrayList<>();
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting weather alert data");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (alerts == null)
            alerts = new ArrayList<>();

        return alerts;
    }

    // Fix format of query to pass to Yahoo API
    @Override
    public void updateLocationData(LocationData location) {
        String location_query = String.format(Locale.ROOT, "(%f,%f)", location.getLatitude(), location.getLongitude());

        LocationQueryViewModel qview = getLocation(location.getName());

        if (qview != null) {
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
        String query = "";
        String coord = String.format(Locale.ROOT, "%s,%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
        LocationQueryViewModel qview = getLocation(new WeatherUtils.Coordinate(coord));

        if (StringUtils.isNullOrEmpty(qview.getLocationQuery()))
            query = String.format("latitude=%s&longitude=%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
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
            query = String.format("latitude=%s&longitude=%s", location.getLatitude(), location.getLongitude());
        else
            query = qview.getLocationQuery();

        return query;
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        return name;
    }

    @Override
    public String getWeatherIcon(String icon) {
        boolean isNight = false;

        if (icon.startsWith("N_") || icon.contains("night_"))
            isNight = true;

        return getWeatherIcon(isNight, icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        if (icon.contains("mostly_sunny") || icon.contains("mostly_clear") || icon.contains("partly_cloudy")
                || icon.contains("passing_clounds") || icon.contains("more_sun_than_clouds") || icon.contains("scattered_clouds")
                || icon.contains("decreasing_cloudiness") || icon.contains("clearing_skies") || icon.contains("overcast")
                || icon.contains("low_clouds") || icon.contains("passing_clouds"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
        else if (icon.contains("cloudy") || icon.contains("a_mixture_of_sun_and_clouds") || icon.contains("increasing_cloudiness")
                || icon.contains("breaks_of_sun_late") || icon.contains("afternoon_clouds") || icon.contains("morning_clouds")
                || icon.contains("partly_sunny") || icon.contains("more_clouds_than_sun") || icon.contains("broken_clouds"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_CLOUDY;
        else if (icon.contains("high_level_clouds") || icon.contains("high_clouds"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY_HIGH;
            else
                weatherIcon = WeatherIcons.DAY_CLOUDY_HIGH;
        else if (icon.contains("flurries") || icon.contains("snowstorm") || icon.contains("blizzard"))
            weatherIcon = WeatherIcons.SNOW_WIND;
        else if (icon.contains("fog"))
            weatherIcon = WeatherIcons.FOG;
        else if (icon.contains("hazy") || icon.contains("haze"))
            if (isNight)
                weatherIcon = WeatherIcons.WINDY;
            else
                weatherIcon = WeatherIcons.DAY_HAZE;
        else if (icon.contains("sleet") || icon.contains("snow_changing_to_an_icy_mix") || icon.contains("an_icy_mix_changing_to_snow")
                || icon.contains("rain_changing_to_snow"))
            weatherIcon = WeatherIcons.SLEET;
        else if (icon.contains("mixture_of_precip") || icon.contains("icy_mix") || icon.contains("snow_changing_to_rain")
                || icon.contains("snow_rain_mix") || icon.contains("freezing_rain"))
            weatherIcon = WeatherIcons.RAIN_MIX;
        else if (icon.contains("hail"))
            weatherIcon = WeatherIcons.HAIL;
        else if (icon.contains("snow"))
            weatherIcon = WeatherIcons.SNOW;
        else if (icon.contains("sprinkles") || icon.contains("drizzle"))
            weatherIcon = WeatherIcons.SPRINKLE;
        else if (icon.contains("light_rain") || icon.contains("showers"))
            weatherIcon = WeatherIcons.SHOWERS;
        else if (icon.contains("rain") || icon.contains("flood"))
            weatherIcon = WeatherIcons.RAIN;
        else if (icon.contains("tstorms") || icon.contains("thunderstorms") || icon.contains("thundershowers")
                || icon.contains("tropical_storm"))
            weatherIcon = WeatherIcons.THUNDERSTORM;
        else if (icon.contains("smoke"))
            weatherIcon = WeatherIcons.SMOKE;
        else if (icon.contains("tornado"))
            weatherIcon = WeatherIcons.TORNADO;
        else if (icon.contains("hurricane"))
            weatherIcon = WeatherIcons.HURRICANE;
        else if (icon.contains("sandstorm"))
            weatherIcon = WeatherIcons.SANDSTORM;
        else if (icon.contains("duststorm"))
            weatherIcon = WeatherIcons.DUST;
        else if (icon.contains("clear") || icon.contains("sunny"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_CLEAR;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY;
        else if (icon.contains("cw_no_report_icon") || icon.startsWith("night_"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_CLEAR;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY;

        if (StringUtils.isNullOrWhitespace(weatherIcon)) {
            // Not Available
            weatherIcon = WeatherIcons.NA;
        }

        return weatherIcon;
    }
}
