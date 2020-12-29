package com.thewizrd.shared_resources.weatherdata.here;

import android.util.Log;

import com.ibm.icu.util.ULocale;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.LocationUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProviderInterface;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import org.threeten.bp.ZoneOffset;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class HEREWeatherProvider extends WeatherProviderImpl implements WeatherAlertProviderInterface {
    private static final String WEATHER_GLOBAL_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?" +
            "product=alerts&product=forecast_7days_simple&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true" +
            "&%s&language=%s&metric=false";
    private static final String WEATHER_US_CA_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?" +
            "product=nws_alerts&product=forecast_7days_simple&product=forecast_hourly&product=forecast_astronomy&product=observation&oneobservation=true" +
            "&%s&language=%s&metric=false";
    private static final String ALERT_GLOBAL_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?product=alerts&%s&language=%s&metric=false";
    private static final String ALERT_US_CA_QUERY_URL = "https://weather.ls.hereapi.com/weather/1.0/report.json?product=nws_alerts&%s&language=%s&metric=false";

    public HEREWeatherProvider() {
        super();

        locationProvider = RemoteConfig.getLocationProvider(getWeatherAPI());
        if (locationProvider == null) {
            locationProvider = new HERELocationProvider();
        }
    }

    @Override
    public String getWeatherAPI() {
        return WeatherAPI.HERE;
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
        return true;
    }

    @Override
    public boolean needsExternalAlertData() {
        return false;
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
    public Weather getWeather(String location_query, String country_code) throws WeatherException {
        Weather weather;

        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;
        WeatherException wEx = null;

        try {
            final String authorization = HEREOAuthUtils.getBearerToken(false);

            if (StringUtils.isNullOrWhitespace(authorization)) {
                throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            final String url;
            if (LocationUtils.isUSorCanada(country_code)) {
                url = String.format(WEATHER_US_CA_QUERY_URL, location_query, locale);
            } else {
                url = String.format(WEATHER_GLOBAL_QUERY_URL, location_query, locale);
            }

            Request request = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build())
                    .url(url)
                    .addHeader("Authorization", authorization)
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load weather
            Rootobject root = JSONParser.deserializer(stream, Rootobject.class);

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
                weather.setWeatherAlerts(new ArrayList<WeatherAlert>(root.getAlerts().getAlerts().size()));

                for (AlertsItem result : root.getAlerts().getAlerts()) {
                    weather.getWeatherAlerts().add(new WeatherAlert(result));
                }
            } else if (root.getNwsAlerts() != null && (root.getNwsAlerts().getWatch() != null || root.getNwsAlerts().getWarning() != null)) {
                final int numOfAlerts = (root.getNwsAlerts().getWatch() != null ? root.getNwsAlerts().getWatch().size() : 0) +
                        (root.getNwsAlerts().getWarning() != null ? root.getNwsAlerts().getWarning().size() : 0);

                weather.setWeatherAlerts(new HashSet<WeatherAlert>(numOfAlerts));

                final double lat = weather.getLocation().getLatitude();
                final double lon = weather.getLocation().getLongitude();

                if (root.getNwsAlerts().getWatch() != null) {
                    for (WatchItem watchItem : root.getNwsAlerts().getWatch()) {
                        // Add watch item if location is within 20km of the center of the alert zone
                        if (ConversionMethods.calculateHaversine(lat, lon, watchItem.getLatitude(), watchItem.getLongitude()) < 20000) {
                            weather.getWeatherAlerts().add(new WeatherAlert(watchItem));
                        }
                    }
                }
                if (root.getNwsAlerts().getWarning() != null) {
                    for (WarningItem warningItem : root.getNwsAlerts().getWarning()) {
                        // Add warning item if location is within 25km of the center of the alert zone
                        if (ConversionMethods.calculateHaversine(lat, lon, warningItem.getLatitude(), warningItem.getLongitude()) < 25000) {
                            weather.getWeatherAlerts().add(new WeatherAlert(warningItem));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting weather data");
        } finally {
            if (response != null)
                response.close();
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

        if (weather.getWeatherAlerts() != null && weather.getWeatherAlerts().size() > 0) {
            for (WeatherAlert alert : weather.getWeatherAlerts()) {
                if (!alert.getDate().getOffset().equals(offset)) {
                    alert.setDate(alert.getDate().withZoneSameLocal(offset));
                }

                if (!alert.getExpiresDate().getOffset().equals(offset)) {
                    alert.setExpiresDate(alert.getExpiresDate().withZoneSameLocal(offset));
                }
            }
        }

        // Update tz for weather properties
        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(location.getTzOffset()));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(location.getTzOffset()));

        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
        }

        return weather;
    }

    @Override
    public Collection<WeatherAlert> getAlerts(LocationData location) {
        Collection<WeatherAlert> alerts = null;

        ULocale uLocale = ULocale.forLocale(LocaleUtils.getLocale());
        String locale = localeToLangCode(uLocale.getLanguage(), uLocale.toLanguageTag());

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            final String authorization = HEREOAuthUtils.getBearerToken(false);

            if (StringUtils.isNullOrWhitespace(authorization)) {
                throw new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }

            final String country_code = location.getCountryCode();
            String url;
            if (LocationUtils.isUSorCanada(country_code)) {
                url = String.format(ALERT_US_CA_QUERY_URL, location.getQuery(), locale);
            } else {
                url = String.format(ALERT_GLOBAL_QUERY_URL, location.getQuery(), locale);
            }

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authorization)
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load data
            Rootobject root = JSONParser.deserializer(stream, Rootobject.class);

            // End Stream
            stream.close();

            // Add weather alerts if available
            if (root.getAlerts() != null && root.getAlerts().getAlerts().size() > 0) {
                alerts = new ArrayList<>(root.getAlerts().getAlerts().size());

                for (AlertsItem result : root.getAlerts().getAlerts()) {
                    alerts.add(new WeatherAlert(result));
                }
            } else if (root.getNwsAlerts() != null && (root.getNwsAlerts().getWatch() != null || root.getNwsAlerts().getWarning() != null)) {
                final int numOfAlerts = (root.getNwsAlerts().getWatch() != null ? root.getNwsAlerts().getWatch().size() : 0) +
                        (root.getNwsAlerts().getWarning() != null ? root.getNwsAlerts().getWarning().size() : 0);

                alerts = new HashSet<>(numOfAlerts);

                final double lat = location.getLatitude();
                final double lon = location.getLongitude();

                if (root.getNwsAlerts().getWatch() != null) {
                    for (WatchItem watchItem : root.getNwsAlerts().getWatch()) {
                        // Add watch item if location is within 20km of the center of the alert zone
                        if (ConversionMethods.calculateHaversine(lat, lon, watchItem.getLatitude(), watchItem.getLongitude()) < 20000) {
                            alerts.add(new WeatherAlert(watchItem));
                        }
                    }
                }
                if (root.getNwsAlerts().getWarning() != null) {
                    for (WarningItem warningItem : root.getNwsAlerts().getWarning()) {
                        // Add warning item if location is within 25km of the center of the alert zone
                        if (ConversionMethods.calculateHaversine(lat, lon, warningItem.getLatitude(), warningItem.getLongitude()) < 25000) {
                            alerts.add(new WeatherAlert(warningItem));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "HEREWeatherProvider: error getting weather alert data");
        } finally {
            if (response != null)
                response.close();
        }

        if (alerts == null)
            alerts = Collections.emptyList();

        return alerts;
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");
        return String.format(Locale.ROOT, "latitude=%s&longitude=%s", df.format(weather.getLocation().getLatitude()), df.format(weather.getLocation().getLongitude()));
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");
        return String.format(Locale.ROOT, "latitude=%s&longitude=%s", df.format(location.getLatitude()), df.format(location.getLongitude()));
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
