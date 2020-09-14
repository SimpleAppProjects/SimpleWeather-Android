package com.thewizrd.shared_resources.weatherdata.nws;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
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
import java.text.DecimalFormat;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NWSWeatherProvider extends WeatherProviderImpl {
    private static final String POINTS_QUERY_URL = "https://api.weather.gov/points/%s";

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
    public Weather getWeather(final String location_query, final String country_code) throws WeatherException {
        Weather weather;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response pointsResponse = null;
        WeatherException wEx = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request request = new Request.Builder()
                    .url(String.format(POINTS_QUERY_URL, location_query))
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            pointsResponse = client.newCall(request).execute();

            // Check for errors
            checkForErrors(pointsResponse.code());

            final InputStream stream = pointsResponse.body().byteStream();

            // Load point json data
            PointsResponse pointsResponseData = JSONParser.deserializer(stream, PointsResponse.class);

            // End Stream
            stream.close();

            final String forecastUrl = pointsResponseData.getForecast();
            final String forecastHourlyUrl = pointsResponseData.getForecastHourly();
            final String observationStationsUrl = pointsResponseData.getObservationStations();

            ForecastResponse forecastResponse = getForecastResponse(forecastUrl);
            HourlyForecastResponse hourlyForecastResponse = getHourlyForecastResponse(forecastHourlyUrl);
            ObservationStationsResponse stationsResponse = getObservationStationsResponse(observationStationsUrl);

            final String stationUrl = stationsResponse.getObservationStations().get(0);
            ObservationCurrentResponse obsCurrentResponse = getObservationCurrentResponse(stationUrl);

            weather = new Weather(pointsResponseData, forecastResponse, hourlyForecastResponse, obsCurrentResponse);
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "NWSWeatherProvider: error getting weather data");
        } finally {
            if (pointsResponse != null)
                pointsResponse.close();
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
        ForecastResponse responseData;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request request = new Request.Builder()
                    .url(url + "?units=us")
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();

            // Check for errors
            checkForErrors(response.code());

            final InputStream stream = response.body().byteStream();

            // Load point json data
            responseData = JSONParser.deserializer(stream, ForecastResponse.class);

            // End Stream
            stream.close();
        } finally {
            if (response != null)
                response.close();
        }

        return responseData;
    }

    private HourlyForecastResponse getHourlyForecastResponse(String url) throws Exception {
        HourlyForecastResponse responseData;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request request = new Request.Builder()
                    .url(url + "?units=us")
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();

            // Check for errors
            checkForErrors(response.code());

            final InputStream stream = response.body().byteStream();

            // Load point json data
            responseData = JSONParser.deserializer(stream, HourlyForecastResponse.class);

            // End Stream
            stream.close();
        } catch (WeatherException wEx) {
            // Allow continuing w/o the data
            responseData = null;
        } finally {
            if (response != null)
                response.close();
        }

        return responseData;
    }

    private ObservationStationsResponse getObservationStationsResponse(String url) throws Exception {
        ObservationStationsResponse responseData;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();

            // Check for errors
            checkForErrors(response.code());

            final InputStream stream = response.body().byteStream();

            // Load point json data
            responseData = JSONParser.deserializer(stream, ObservationStationsResponse.class);

            // End Stream
            stream.close();
        } finally {
            if (response != null)
                response.close();
        }

        return responseData;
    }

    private ObservationCurrentResponse getObservationCurrentResponse(String url) throws Exception {
        ObservationCurrentResponse responseData;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request request = new Request.Builder()
                    .url(url + "/observations/latest?require_qc=true")
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();

            // Check for errors
            checkForErrors(response.code());

            final InputStream stream = response.body().byteStream();

            // Load point json data
            responseData = JSONParser.deserializer(stream, ObservationCurrentResponse.class);

            // End Stream
            stream.close();
        } finally {
            if (response != null)
                response.close();
        }

        return responseData;
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
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");
        return String.format(Locale.ROOT, "%s,%s", df.format(weather.getLocation().getLatitude()), df.format(weather.getLocation().getLongitude()));
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");
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
        } else if (icon.contains("tropical_storm") || icon.contains("tsra") || icon.contains("hurricane")) {
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

    @Override
    public String getWeatherCondition(String icon) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        if (icon == null)
            return context.getString(R.string.weather_notavailable); // TODO: or blank

        if (icon.contains("fog")) {
            return context.getString(R.string.weather_fog);
        } else if (icon.contains("blizzard")) {
            return context.getString(R.string.weather_blizzard);
        } else if (icon.contains("cold")) {
            return context.getString(R.string.weather_cold);
        } else if (icon.contains("hot")) {
            return context.getString(R.string.weather_hot);
        } else if (icon.contains("haze")) {
            return context.getString(R.string.weather_haze);
        } else if (icon.contains("smoke")) {
            return context.getString(R.string.weather_smoky);
        } else if (icon.contains("dust")) {
            return context.getString(R.string.weather_dust);
        } else if (icon.contains("tropical_storm") || icon.contains("tsra")) {
            return context.getString(R.string.weather_tropicalstorm);
        } else if (icon.contains("hurricane")) {
            return context.getString(R.string.weather_hurricane);
        } else if (icon.contains("tornado")) {
            return context.getString(R.string.weather_tornado);
        } else if (icon.contains("rain_showers")) {
            return context.getString(R.string.weather_rainshowers);
        } else if (icon.contains("fzra")) {
            return context.getString(R.string.weather_freezingrain);
        } else if (icon.contains("rain_sleet")) {
            return context.getString(R.string.weather_rainandsleet);
        } else if (icon.contains("rain_snow")) {
            return context.getString(R.string.weather_rainandsnow);
        } else if (icon.contains("sleet")) {
            return context.getString(R.string.weather_sleet);
        } else if (icon.contains("rain")) {
            return context.getString(R.string.weather_rain);
        } else if (icon.contains("snow")) {
            return context.getString(R.string.weather_snow);
        } else if (icon.contains("wind_bkn") || icon.contains("wind_ovc") || icon.contains("wind_sct") || icon.contains("wind")) {
            return context.getString(R.string.weather_windy);
        } else if (icon.contains("ovc")) {
            return context.getString(R.string.weather_overcast);
        } else if (icon.contains("sct") || icon.contains("few")) {
            return context.getString(R.string.weather_partlycloudy);
        } else if (icon.contains("bkn")) {
            return context.getString(R.string.weather_cloudy);
        } else {
            return context.getString(R.string.weather_notavailable); // TODO: or blank
        }
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
        }

        return isNight;
    }
}
