package com.thewizrd.shared_resources.weatherdata.openweather;

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
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

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

public final class OpenWeatherMapProvider extends WeatherProviderImpl {

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
    public Collection<LocationQueryViewModel> getLocations(String ac_query) {
        List<LocationQueryViewModel> locations = null;

        String queryAPI = "https://autocomplete.wunderground.com/aq?query=";
        String options = "&h=0&cities=1";
        HttpURLConnection client = null;
        // Limit amount of results shown
        int maxResults = 10;

        try {
            // Connect to webstream
            URL queryURL = new URL(queryAPI + ac_query + options);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            locations = new ArrayList<>();
            AC_Rootobject root = (AC_Rootobject) JSONParser.deserializer(stream, AC_Rootobject.class);

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
            Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting locations");
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
        Location result = null;
        WeatherException wEx = null;

        try {
            // Connect to webstream
            URL queryURL = new URL(queryAPI + query + options);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            Serializer deserializer = new Persister();
            result = deserializer.read(Location.class, stream, false);

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
            Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting location");
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
            URL queryURL = new URL(queryAPI + location_query + options);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            AC_Rootobject root = (AC_Rootobject) JSONParser.deserializer(stream, AC_Rootobject.class);
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
            Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting location");
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
        String queryAPI = "https://api.openweathermap.org/data/2.5/";
        String query = "forecast?appid=";
        HttpURLConnection client = null;
        boolean isValid = false;
        WeatherException wEx = null;

        try {
            if (StringUtils.isNullOrWhitespace(key)) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                throw wEx;
            }

            // Connect to webstream
            URL queryURL = new URL(queryAPI + query + key);
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

    @Override
    public String getAPIKey() {
        return Keys.getOWMKey();
    }

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        String currentAPI = null;
        URL currentURL = null;
        String forecastAPI = null;
        URL forecastURL = null;
        String query = null;
        HttpURLConnection client = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        try {
            query = String.format(Locale.ROOT, "id=%d", Integer.parseInt(location_query));
        } catch (NumberFormatException ex) {
            query = location_query;
        }

        String key = Settings.usePersonalKey() ? Settings.getAPIKEY() : getAPIKey();

        WeatherException wEx = null;

        try {
            currentAPI = "https://api.openweathermap.org/data/2.5/weather?%s&appid=%s&lang=" + locale;
            currentURL = new URL(String.format(currentAPI, query, key));
            forecastAPI = "https://api.openweathermap.org/data/2.5/forecast?%s&appid=%s&lang=" + locale;
            forecastURL = new URL(String.format(forecastAPI, query, key));

            InputStream currentStream = null;
            InputStream forecastStream = null;

            client = (HttpURLConnection) currentURL.openConnection();
            currentStream = client.getInputStream();
            client = (HttpURLConnection) forecastURL.openConnection();
            forecastStream = client.getInputStream();

            // Reset exception
            wEx = null;

            // Load weather
            CurrentRootobject currRoot = null;
            ForecastRootobject foreRoot = null;
            // TODO: put in async task?
            currRoot = (CurrentRootobject) JSONParser.deserializer(currentStream, CurrentRootobject.class);
            // TODO: put in async task?
            foreRoot = (ForecastRootobject) JSONParser.deserializer(forecastStream, ForecastRootobject.class);

            // End Stream
            currentStream.close();
            forecastStream.close();

            weather = new Weather(currRoot, foreRoot);

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
            Logger.writeLine(Log.ERROR, ex, "OpenWeatherMapProvider: error getting weather data");
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

        // OWM reports datetime in UTC; add location tz_offset
        ZoneOffset offset = location.getTzOffset();
        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(offset));
        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            hr_forecast.setDate(hr_forecast.getDate().withZoneSameInstant(offset));
        }
        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
        }
        weather.getAstronomy().setSunrise(weather.getAstronomy().getSunrise().plusSeconds(offset.getTotalSeconds()));
        weather.getAstronomy().setSunset(weather.getAstronomy().getSunset().plusSeconds(offset.getTotalSeconds()));

        return weather;
    }

    // Use location name here instead of query since we use the AutoComplete API
    @Override
    public void updateLocationData(LocationData location) {
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
        WeatherUtils.Coordinate coord = new WeatherUtils.Coordinate(Double.valueOf(weather.getLocation().getLatitude()), Double.valueOf(weather.getLocation().getLongitude()));
        LocationQueryViewModel qview = getLocation(coord);

        if (StringUtils.isNullOrEmpty(qview.getLocationQuery()))
            query = String.format(Locale.ROOT, "lat=%f&lon=%f", coord.getLatitude(), coord.getLongitude());
        else
            query = qview.getLocationQuery();

        return query;
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        String query = "";
        WeatherUtils.Coordinate coord = new WeatherUtils.Coordinate(location.getLatitude(), location.getLongitude());
        LocationQueryViewModel qview = getLocation(coord);

        if (StringUtils.isNullOrEmpty(qview.getLocationQuery()))
            query = String.format(Locale.ROOT, "lat=%f&lon=%f", coord.getLatitude(), coord.getLongitude());
        else
            query = qview.getLocationQuery();

        return query;
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
                weatherIcon = WeatherIcons.STORM_SHOWERS;
                break;

            case "211": // thunderstorm
            case "212": // heavy thunderstorm
            case "221": // ragged thunderstorm
            case "202": // thunderstorm w/ heavy rain
            case "232": // thunderstorm w/ heavy drizzle
                weatherIcon = WeatherIcons.THUNDERSTORM;
                break;

            case "300": // light intensity drizzle
            case "301": // drizzle
            case "321": // shower drizzle
                weatherIcon = WeatherIcons.SPRINKLE;
                break;

            case "302": // heavy intensity drizzle
            case "311": // drizzle rain
            case "312": // heavy intensity drizzle rain
            case "314": // heavy shower rain and drizzle
                weatherIcon = WeatherIcons.RAIN;
                break;

            case "500": // light rain
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SPRINKLE;
                else
                    weatherIcon = WeatherIcons.DAY_SPRINKLE;
                break;

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
                weatherIcon = WeatherIcons.RAIN_MIX;
                break;

            case "313": // shower rain and drizzle
            case "520": // light intensity shower rain
            case "521": // shower rain
            case "522": // heavy intensity shower rain
            case "701": // mist
                weatherIcon = WeatherIcons.SHOWERS;
                break;

            case "531": // ragged shower rain
            case "901": // tropical storm
                weatherIcon = WeatherIcons.STORM_SHOWERS;
                break;

            case "600": // light snow
            case "601": // snow
            case "621": // shower snow
            case "622": // heavy shower snow
                weatherIcon = WeatherIcons.SNOW;
                break;

            case "602": // heavy snow
                weatherIcon = WeatherIcons.SNOW_WIND;
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
                weatherIcon = WeatherIcons.FOG;
                break;

            // cloudy-gusts
            case "771": // squalls
                weatherIcon = WeatherIcons.CLOUDY_GUSTS;
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

            // cloudy-gusts
            case "801": // few clouds
            case "802": // scattered clouds
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS;
                else
                    weatherIcon = WeatherIcons.DAY_CLOUDY_GUSTS;
                break;

            // cloudy-gusts
            case "803": // broken clouds
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY_WINDY;
                else
                    weatherIcon = WeatherIcons.DAY_CLOUDY_WINDY;
                break;

            // cloudy
            case "804": // overcast clouds
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
                else
                    weatherIcon = WeatherIcons.DAY_CLOUDY;
                break;

            // hurricane
            case "902":
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
                weatherIcon = WeatherIcons.HAIL;
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
}
