package com.thewizrd.shared_resources.weatherdata.weatherunderground;

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
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
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

public final class WeatherUndergroundProvider extends WeatherProviderImpl {

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
                if (!"city".equals(result.getType()))
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
            Logger.writeLine(Log.ERROR, ex, "WeatherUndergroundProvider: error getting locations");
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
            Logger.writeLine(Log.ERROR, ex, "WeatherUndergroundProvider: error getting location");
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
            Logger.writeLine(Log.ERROR, ex, "WeatherUndergroundProvider: error getting location");
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
        String queryAPI = "https://api.wunderground.com/api/";
        String query = "/q/NY/New_York.json";
        HttpURLConnection client = null;
        boolean isValid = false;
        WeatherException wEx = null;

        try {
            if (StringUtils.isNullOrWhitespace(key)) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                throw wEx;
            }

            // Connect to webstream
            URL queryURL = new URL(queryAPI + key + query);
            client = (HttpURLConnection) queryURL.openConnection();
            InputStream stream = client.getInputStream();

            // Load data
            Rootobject root = (Rootobject) JSONParser.deserializer(stream, Rootobject.class);

            // Check for errors
            if (root.getResponse().getError() != null) {
                switch (root.getResponse().getError().getType()) {
                    case "keynotfound":
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.INVALIDAPIKEY);
                        isValid = false;
                        break;
                }
            } else {
                isValid = true;
            }

            // End Stream
            stream.close();
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
        return Keys.getWUndergroundKey();
    }

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        String queryAPI = null;
        URL weatherURL = null;
        HttpURLConnection client = null;

        ULocale uLocale = ULocale.forLocale(Locale.getDefault());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        String key = Settings.usePersonalKey() ? Settings.getAPIKEY() : getAPIKey();

        WeatherException wEx = null;

        try {
            queryAPI = "https://api.wunderground.com/api/" + key + "/astronomy/conditions/forecast10day/hourly/alerts/lang:" + locale;
            String options = ".json";
            weatherURL = new URL(queryAPI + location_query + options);

            client = (HttpURLConnection) weatherURL.openConnection();
            InputStream stream = client.getInputStream();

            // Reset exception
            wEx = null;

            // Load weather
            Rootobject root = null;
            // TODO: put in async task?
            root = (Rootobject) JSONParser.deserializer(stream, Rootobject.class);

            // Check for errors
            if (root.getResponse().getError() != null) {
                switch (root.getResponse().getError().getType()) {
                    case "querynotfound":
                        wEx = new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
                        break;
                    case "keynotfound":
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
            if (root.getAlerts() != null && root.getAlerts().size() > 0) {
                if (weather.getWeatherAlerts() == null)
                    weather.setWeatherAlerts(new ArrayList<WeatherAlert>());

                for (Alert result : root.getAlerts()) {
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
            Logger.writeLine(Log.ERROR, ex, "WeatherUndergroundProvider: error getting weather data");
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

        // Just update hourly forecast dates to timezone
        ZoneOffset offset = location.getTzOffset();

        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            if (!offset.equals(hr_forecast.getDate().getOffset()))
                hr_forecast.setDate(hr_forecast.getDate().withZoneSameLocal(offset));
        }

        // Update tz for weather alerts
        if (weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
            for (WeatherAlert alert : weather.getWeatherAlerts()) {
                if (!alert.getDate().getOffset().equals(offset)) {
                    alert.setDate(alert.getDate().withZoneSameInstant(offset));
                }

                if (!alert.getExpiresDate().getOffset().equals(offset)) {
                    alert.setExpiresDate(alert.getExpiresDate().withZoneSameInstant(offset));
                }
            }
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

        String key = Settings.usePersonalKey() ? Settings.getAPIKEY() : getAPIKey();

        WeatherException wEx = null;

        try {
            queryAPI = "https://api.wunderground.com/api/" + key + "/alerts/lang:" + locale;
            String options = ".json";
            weatherURL = new URL(queryAPI + location.getQuery() + options);

            client = (HttpURLConnection) weatherURL.openConnection();
            InputStream stream = client.getInputStream();

            // Reset exception
            wEx = null;

            // Load weather
            alerts = new ArrayList<>();

            Rootobject root = null;
            // TODO: put in async task?
            root = (Rootobject) JSONParser.deserializer(stream, Rootobject.class);

            for (Alert result : root.getAlerts()) {
                alerts.add(new WeatherAlert(result));
            }

            // End Stream
            stream.close();
        } catch (Exception ex) {
            alerts = new ArrayList<>();
            Logger.writeLine(Log.ERROR, ex, "WeatherUndergroundProvider: error getting weather alert data");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (alerts == null)
            alerts = new ArrayList<>();

        return alerts;
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
        String coord = String.format(Locale.ROOT, "%s,%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
        LocationQueryViewModel qview = getLocation(new WeatherUtils.Coordinate(coord));

        if (StringUtils.isNullOrEmpty(qview.getLocationQuery()))
            query = String.format("/q/%s", coord);
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
            query = String.format("/q/%s", coord);
        else
            query = qview.getLocationQuery();

        return query;
    }

    @Override
    public String localeToLangCode(String iso, String name) {
        String code = "EN";

        switch (iso) {
            // Afrikaans
            case "af":
                // Arabic
            case "ar":
                // Armenian
            case "hy":
                // Azerbaijani
            case "az":
                // Basque
            case "eu":
                // Burmese
            case "my":
                // Catalan
            case "ca":
                // Dhivehi
            case "dv":
                // Dutch
            case "nl":
                // Esperanto
            case "eo":
                // Estonian
            case "et":
                // Farsi / Persian
            case "fa":
                // Finnish
            case "fi":
                // Georgian
            case "ka":
                // Gujarati
            case "gu":
                // Haitian Creole
            case "ht":
                // Hindi
            case "hi":
                // Hungarian
            case "hu":
                // Icelandic
            case "is":
                // Ido
            case "io":
                // Indonesian
            case "id":
                // Italian
            case "it":
                // Khmer
            case "km":
                // Kurdish
            case "ku":
                // Latin
            case "la":
                // Latvian
            case "lv":
                // Lithuanian
            case "lt":
                // Macedonian
            case "mk":
                // Maltese
            case "mt":
                // Maori
            case "mi":
                // Marathi
            case "mr":
                // Mongolian
            case "mn":
                // Norwegian
            case "no":
                // Occitan
            case "oc":
                // Pashto
            case "ps":
                // Polish
            case "pl":
                // Punjabi
            case "pa":
                // Romanian
            case "ro":
                // Russian
            case "ru":
                // Serbian
            case "sr":
                // Slovak
            case "sk":
                // Slovenian
            case "sl":
                // Tagalog
            case "tl":
                // Thai
            case "th":
                // Turkish
            case "tr":
                // Turkmen
            case "tk":
                // Uzbek
            case "uz":
                // Welsh
            case "cy":
                // Yiddish
            case "yi":
                code = iso.toUpperCase();
                break;
            // Albanian
            case "sq":
                code = "AL";
                break;
            // Belarusian
            case "be":
                code = "BY";
                break;
            // Bulgarian
            case "bg":
                code = "BU";
                break;
            // English
            case "en":
                // British English
                if (name.equals("en-GB"))
                    code = "LI";
                else
                    code = "EN";
                break;
            // Chinese
            case "zh":
                switch (name) {
                    // Chinese - Traditional
                    case "zh-Hant":
                    case "zh-HK":
                    case "zh-MO":
                    case "zh-TW":
                        code = "TW";
                        break;
                    // Chinese - Simplified
                    default:
                        code = "CN";
                        break;
                }
                break;
            // Croatian
            case "hr":
                code = "CR";
                break;
            // Czech
            case "cs":
                code = "CZ";
                break;
            // Danish
            case "da":
                code = "DK";
                break;
            // French
            case "fr":
                if (name.equals("fr-CA"))
                    // French Canadian
                    code = "FC";
                else
                    code = "FR";
                break;
            // Galician
            case "gl":
                code = "GZ";
                break;
            // German
            case "de":
                code = "DL";
                break;
            // Greek
            case "el":
                code = "GR";
                break;
            // Hebrew
            case "he":
                code = "IL";
                break;
            // Irish
            case "ga":
                code = "IR";
                break;
            // Japanese
            case "ja":
                code = "JP";
                break;
            // Javanese
            case "jv":
                code = "JW";
                break;
            // Korean
            case "ko":
                code = "KR";
                break;
            // Portuguese
            case "pt":
                code = "BR";
                break;
            // Spanish
            case "es":
                code = "SP";
                break;
            // Swahili
            case "sw":
                code = "SI";
                break;
            // Swedish
            case "sv":
                code = "SW";
                break;
            // Swiss
            case "gsw":
                code = "CH";
                break;
            // Ukrainian
            case "uk":
                code = "UA";
                break;
            // Vietnamese
            case "vi":
                code = "VU";
                break;
            // Wolof
            case "wo":
                code = "SN";
                break;
                /*
                // Mandinka
                case "mandinka":
                    code = "GM";
                    break;
                // Plautdietsch
                case "plautdietsch":
                    code = "GN";
                    break;
                // Tatarish
                case "tatarish":
                    code = "TT";
                    break;
                // Yiddish - transliterated
                case "yiddish-tl":
                    code = "JI";
                    break;
                */
            default:
                // Low German
                if (name.equals("nds") || name.startsWith("nds-"))
                    code = "ND";
                else
                    code = "EN";
                break;
        }

        return code;
    }

    @Override
    public String getWeatherIcon(String icon) {
        boolean isNight = false;

        if (icon.contains("nt_"))
            isNight = true;

        return getWeatherIcon(isNight, icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        if (icon.contains("nt_mostlycloudy") || icon.contains("nt_partlysunny") || icon.contains("nt_cloudy"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_CLOUDY;
        else if (icon.contains("nt_partlycloudy") || icon.contains("nt_mostlysunny"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
        else if (icon.contains("nt_clear") || icon.contains("nt_sunny") || icon.contains("nt_unknown"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_CLEAR;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY;
        else if (icon.contains("chancerain"))
            weatherIcon = WeatherIcons.RAIN;
        else if (icon.contains("clear") || icon.contains("sunny"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_CLEAR;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY;
        else if (icon.contains("cloudy"))
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_CLOUDY;
        else if (icon.contains("flurries"))
            weatherIcon = WeatherIcons.SNOW_WIND;
        else if (icon.contains("fog"))
            weatherIcon = WeatherIcons.FOG;
        else if (icon.contains("hazy"))
            if (isNight)
                weatherIcon = WeatherIcons.WINDY;
            else
                weatherIcon = WeatherIcons.DAY_HAZE;
        else if (icon.contains("sleet") || icon.contains("sleat"))
            weatherIcon = WeatherIcons.SLEET;
        else if (icon.contains("rain"))
            weatherIcon = WeatherIcons.SHOWERS;
        else if (icon.contains("snow"))
            weatherIcon = WeatherIcons.SNOW;
        else if (icon.contains("tstorms"))
            weatherIcon = WeatherIcons.THUNDERSTORM;
        else if (icon.contains("unknown") || icon.contains("nt_"))
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
