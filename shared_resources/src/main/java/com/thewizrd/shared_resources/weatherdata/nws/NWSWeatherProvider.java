package com.thewizrd.shared_resources.weatherdata.nws;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class NWSWeatherProvider extends WeatherProviderImpl {

    public NWSWeatherProvider() {
        super();
        locationProvider = new HERELocationProvider();
    }

    @Override
    public String getWeatherAPI() {
        return WeatherAPI.NWS;
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

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        URL weatherURL = null;
        HttpURLConnection client = null;

        WeatherException wEx = null;

        try {
            String queryAPI = "https://api.weather.gov/points/%s";
            weatherURL = new URL(String.format(queryAPI, location_query));

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            // Add headers to request
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept", "application/ld+json");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));

            // Check for errors
            checkForErrors(client.getResponseCode());

            if ("gzip".equals(client.getContentEncoding())) {
                stream = new GZIPInputStream(client.getInputStream());
            } else {
                stream = client.getInputStream();
            }

            // Reset exception
            wEx = null;

            // Load point json data
            PointsResponse pointsResponse = null;
            pointsResponse = JSONParser.deserializer(stream, PointsResponse.class);

            // End Stream
            stream.close();

            final String forecastUrl = pointsResponse.getForecast();
            final String forecastHourlyUrl = pointsResponse.getForecastHourly();
            final String observationStationsUrl = pointsResponse.getObservationStations();

            ForecastResponse forecastResponse = new AsyncTaskEx<ForecastResponse, Exception>().await(new CallableEx<ForecastResponse, Exception>() {
                @Override
                public ForecastResponse call() throws Exception {
                    return getForecastResponse(forecastUrl);
                }
            });
            HourlyForecastResponse hourlyForecastResponse = new AsyncTaskEx<HourlyForecastResponse, Exception>().await(new CallableEx<HourlyForecastResponse, Exception>() {
                @Override
                public HourlyForecastResponse call() throws Exception {
                    return getHourlyForecastResponse(forecastHourlyUrl);
                }
            });
            ObservationStationsResponse stationsResponse = new AsyncTaskEx<ObservationStationsResponse, Exception>().await(new CallableEx<ObservationStationsResponse, Exception>() {
                @Override
                public ObservationStationsResponse call() throws Exception {
                    return getObservationStationsResponse(observationStationsUrl);
                }
            });

            final String stationUrl = stationsResponse.getObservationStations().get(0);
            ObservationCurrentResponse obsCurrentResponse = new AsyncTaskEx<ObservationCurrentResponse, Exception>().await(new CallableEx<ObservationCurrentResponse, Exception>() {
                @Override
                public ObservationCurrentResponse call() throws Exception {
                    return getObservationCurrentResponse(stationUrl);
                }
            });

            weather = new Weather(pointsResponse, forecastResponse, hourlyForecastResponse, obsCurrentResponse);
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "NWSWeatherProvider: error getting weather data");
        } finally {
            if (client != null)
                client.disconnect();
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

    private ForecastResponse getForecastResponse(String url) throws Exception {
        ForecastResponse response = null;

        URL weatherURL = null;
        HttpURLConnection client = null;

        try {
            weatherURL = new URL(url + "?units=us");

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            // Add headers to request
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept", "application/ld+json");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));

            // Check for errors
            checkForErrors(client.getResponseCode());

            if ("gzip".equals(client.getContentEncoding())) {
                stream = new GZIPInputStream(client.getInputStream());
            } else {
                stream = client.getInputStream();
            }

            // Load point json data
            response = JSONParser.deserializer(stream, ForecastResponse.class);

            // End Stream
            stream.close();

        } catch (Exception ex) {
            response = null;
            throw ex;
        } finally {
            if (client != null)
                client.disconnect();
        }

        return response;
    }

    private HourlyForecastResponse getHourlyForecastResponse(String url) throws Exception {
        HourlyForecastResponse response = null;

        URL weatherURL = null;
        HttpURLConnection client = null;

        try {
            weatherURL = new URL(url + "?units=us");

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            // Add headers to request
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept", "application/ld+json");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));

            // Check for errors
            checkForErrors(client.getResponseCode());

            if ("gzip".equals(client.getContentEncoding())) {
                stream = new GZIPInputStream(client.getInputStream());
            } else {
                stream = client.getInputStream();
            }

            // Load point json data
            response = JSONParser.deserializer(stream, HourlyForecastResponse.class);

            // End Stream
            stream.close();

        } catch (WeatherException wEx) {
            // Allow continuing w/o the data
            response = null;
        } catch (Exception ex) {
            response = null;
            throw ex;
        } finally {
            if (client != null)
                client.disconnect();
        }

        return response;
    }

    private ObservationStationsResponse getObservationStationsResponse(String url) throws Exception {
        ObservationStationsResponse response = null;

        URL weatherURL = null;
        HttpURLConnection client = null;

        try {
            weatherURL = new URL(url);

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            // Add headers to request
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept", "application/ld+json");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));

            // Check for errors
            checkForErrors(client.getResponseCode());

            if ("gzip".equals(client.getContentEncoding())) {
                stream = new GZIPInputStream(client.getInputStream());
            } else {
                stream = client.getInputStream();
            }

            // Load point json data
            response = JSONParser.deserializer(stream, ObservationStationsResponse.class);

            // End Stream
            stream.close();

        } catch (Exception ex) {
            response = null;
            throw ex;
        } finally {
            if (client != null)
                client.disconnect();
        }

        return response;
    }

    private ObservationCurrentResponse getObservationCurrentResponse(String url) throws Exception {
        ObservationCurrentResponse response = null;

        URL weatherURL = null;
        HttpURLConnection client = null;

        try {
            weatherURL = new URL(url + "/observations/latest?require_qc=true");

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);

            // Add headers to request
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept", "application/ld+json");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));

            // Check for errors
            checkForErrors(client.getResponseCode());

            if ("gzip".equals(client.getContentEncoding())) {
                stream = new GZIPInputStream(client.getInputStream());
            } else {
                stream = client.getInputStream();
            }

            // Load point json data
            response = JSONParser.deserializer(stream, ObservationCurrentResponse.class);

            // End Stream
            stream.close();

        } catch (Exception ex) {
            response = null;
            throw ex;
        } finally {
            if (client != null)
                client.disconnect();
        }

        return response;
    }

    private void checkForErrors(int responseCode) throws WeatherException {
        // Check for errors
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                break;
            // 400 (OK since this isn't a valid request)
            default:
            case HttpURLConnection.HTTP_BAD_REQUEST:
                throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
                // 404 (Not found - Invalid query)
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new WeatherException(WeatherUtils.ErrorStatus.QUERYNOTFOUND);
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw new WeatherException(WeatherUtils.ErrorStatus.UNKNOWN);
        }
    }

    @Override
    public Weather getWeather(final LocationData location) throws WeatherException {
        Weather weather = super.getWeather(location);

        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(location.getTzOffset()));

        // NWS does not provide astrodata; calculate this ourselves (using their calculator)
        weather.setAstronomy(new SolCalcAstroProvider().getAstronomyData(location, weather.getUpdateTime()));

        return weather;
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        return String.format(Locale.ROOT, "%s,%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("#.####");
        return String.format(Locale.ROOT, "%s,%s", df.format(location.getLatitude()), location.getLongitude());
    }

    @Override
    public String getWeatherIcon(String icon) {
        // Example: https://api.weather.gov/icons/land/day/tsra_hi,20?size=medium
        return getWeatherIcon(icon.contains("/night/"), icon);
    }

    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        // Example: https://api.weather.gov/icons/land/day/tsra_hi,20?size=medium
        String weatherIcon = "";

        if (icon.contains("fog")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_FOG;
            else
                weatherIcon = WeatherIcons.DAY_FOG;
        } else if (icon.contains("blizzard")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_WIND;
            else
                weatherIcon = WeatherIcons.DAY_SNOW_WIND;
        } else if (icon.contains("cold")) {
            weatherIcon = WeatherIcons.SNOWFLAKE_COLD;
        } else if (icon.contains("hot")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_CLEAR;
            else
                weatherIcon = WeatherIcons.DAY_HOT;
        } else if (icon.contains("haze")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_FOG;
            else
                weatherIcon = WeatherIcons.DAY_HAZE;
        } else if (icon.contains("smoke")) {
            weatherIcon = WeatherIcons.SMOKE;
        } else if (icon.contains("dust")) {
            weatherIcon = WeatherIcons.DUST;
        } else if (icon.contains("tropical_storm") || icon.contains("tsra")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM;
            else
                weatherIcon = WeatherIcons.DAY_THUNDERSTORM;
        } else if (icon.contains("hurricane")) {
            weatherIcon = WeatherIcons.HURRICANE;
        } else if (icon.contains("tornado")) {
            weatherIcon = WeatherIcons.TORNADO;
        } else if (icon.contains("rain_showers")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_SHOWERS;
            else
                weatherIcon = WeatherIcons.DAY_SHOWERS;
        } else if (icon.contains("fzra") || icon.contains("rain_sleet") || icon.contains("rain_snow")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_RAIN_MIX;
            else
                weatherIcon = WeatherIcons.DAY_RAIN_MIX;
        } else if (icon.contains("sleet")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_SLEET;
            else
                weatherIcon = WeatherIcons.DAY_SLEET;
        } else if (icon.contains("rain")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_RAIN;
            else
                weatherIcon = WeatherIcons.DAY_RAIN;
        } else if (icon.contains("snow")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_SNOW;
            else
                weatherIcon = WeatherIcons.DAY_SNOW;
        } else if (icon.contains("wind_bkn") || icon.contains("wind_ovc") || icon.contains("wind_sct")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY_WINDY;
            else
                weatherIcon = WeatherIcons.DAY_CLOUDY_WINDY;
        } else if (icon.contains("wind")) {
            if (isNight)
                weatherIcon = WeatherIcons.WINDY;
            else
                weatherIcon = WeatherIcons.DAY_WINDY;
        } else if (icon.contains("ovc") || icon.contains("sct") || icon.contains("few")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
        } else if (icon.contains("bkn")) {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_ALT_CLOUDY;
            else
                weatherIcon = WeatherIcons.DAY_CLOUDY;
        } else {
            if (isNight)
                weatherIcon = WeatherIcons.NIGHT_CLEAR;
            else
                weatherIcon = WeatherIcons.DAY_SUNNY;
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

        // The following cases can be present at any time of day
        if (WeatherIcons.SNOWFLAKE_COLD.equals(weather.getCondition().getIcon())) {
            if (!isNight) {
                // Fallback to sunset/rise time just in case
                ZoneId id = ZoneId.of(weather.getLocation().getTzLong());
                ZoneOffset tz = id.getRules().getOffset(Instant.now());
                if (tz == null)
                    tz = weather.getLocation().getTzOffset();

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
        }

        return isNight;
    }
}
