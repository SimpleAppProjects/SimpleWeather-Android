package com.thewizrd.shared_resources.weatherdata.metno;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider;
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public final class MetnoWeatherProvider extends WeatherProviderImpl {
    private static final String FORECAST_QUERY_URL = "https://api.met.no/weatherapi/locationforecast/2.0/complete.json?%s";
    private static final String SUNRISE_QUERY_URL = "https://api.met.no/weatherapi/sunrise/2.0/.json?%s&date=%s&offset=+00:00";

    public MetnoWeatherProvider() {
        super();

        locationProvider = RemoteConfig.getLocationProvider(getWeatherAPI());
        if (locationProvider == null) {
            locationProvider = new LocationIQProvider();
        }
    }

    @Override
    public String getWeatherAPI() {
        return WeatherAPI.METNO;
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
        Weather weather = null;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        okhttp3.Response forecastResponse = null;
        okhttp3.Response sunriseResponse = null;
        WeatherException wEx = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request forecastRequest = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build())
                    .url(String.format(FORECAST_QUERY_URL, location_query))
                    .addHeader("Accept-Encoding", "gzip")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            final String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT));
            Request sunriseRequest = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(3, TimeUnit.HOURS)
                            .build())
                    .url(String.format(SUNRISE_QUERY_URL, location_query, date))
                    .addHeader("Accept-Encoding", "gzip")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            forecastResponse = client.newCall(forecastRequest).execute();
            final InputStream forecastStream = OkHttp3Utils.getStream(forecastResponse);
            sunriseResponse = client.newCall(sunriseRequest).execute();
            final InputStream sunrisesetStream = OkHttp3Utils.getStream(sunriseResponse);

            // Load weather
            Response foreRoot = JSONParser.deserializer(forecastStream, Response.class);
            AstroResponse astroRoot = JSONParser.deserializer(sunrisesetStream, AstroResponse.class);

            // End Stream
            forecastStream.close();
            sunrisesetStream.close();

            weather = new Weather(foreRoot, astroRoot);

        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "MetnoWeatherProvider: error getting weather data");
        } finally {
            if (forecastResponse != null)
                forecastResponse.close();
            if (sunriseResponse != null)
                sunriseResponse.close();
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

        // OWM reports datetime in UTC; add location tz_offset
        ZoneOffset offset = location.getTzOffset();
        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(offset));

        // The time of day is set to max if the sun never sets/rises and
        // DateTime is set to min if not found
        // Don't change this if its set that way
        if (weather.getAstronomy().getSunrise().isAfter(LocalDateTime.MIN) &&
                weather.getAstronomy().getSunrise().toLocalTime().isBefore(LocalTime.MAX))
            weather.getAstronomy().setSunrise(weather.getAstronomy().getSunrise().plusSeconds(offset.getTotalSeconds()));
        if (weather.getAstronomy().getSunset().isAfter(LocalDateTime.MIN) &&
                weather.getAstronomy().getSunset().toLocalTime().isBefore(LocalTime.MAX))
            weather.getAstronomy().setSunset(weather.getAstronomy().getSunset().plusSeconds(offset.getTotalSeconds()));
        if (weather.getAstronomy().getMoonrise().isAfter(LocalDateTime.MIN) &&
                weather.getAstronomy().getMoonrise().toLocalTime().isBefore(LocalTime.MAX))
            weather.getAstronomy().setMoonrise(weather.getAstronomy().getMoonrise().plusSeconds(offset.getTotalSeconds()));
        if (weather.getAstronomy().getMoonset().isAfter(LocalDateTime.MIN) &&
                weather.getAstronomy().getMoonset().toLocalTime().isBefore(LocalTime.MAX))
            weather.getAstronomy().setMoonset(weather.getAstronomy().getMoonset().plusSeconds(offset.getTotalSeconds()));

        // Set condition here
        LocalTime now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime();
        LocalTime sunrise = weather.getAstronomy().getSunrise().toLocalTime();
        LocalTime sunset = weather.getAstronomy().getSunset().toLocalTime();

        weather.getCondition().setWeather(getWeatherCondition(weather.getCondition().getIcon()));
        weather.getCondition().setIcon(getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.getCondition().getIcon()));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(offset));

        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
            forecast.setCondition(getWeatherCondition(forecast.getIcon()));
            forecast.setIcon(getWeatherIcon(forecast.getIcon()));
        }

        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            ZonedDateTime hrf_date = hr_forecast.getDate().withZoneSameInstant(offset);
            hr_forecast.setDate(hrf_date);

            LocalTime hrf_localTime = hrf_date.toLocalTime();
            hr_forecast.setCondition(getWeatherCondition(hr_forecast.getIcon()));
            hr_forecast.setIcon(getWeatherIcon(hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset), hr_forecast.getIcon()));
        }

        return weather;
    }

    private static String getNeutralIconName(String icon_variant) {
        if (icon_variant == null)
            return "";

        return icon_variant.replace("_day", "").replace("_night", "").replace("_polartwilight", "");
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
        return getWeatherIcon(false, icon);
    }

    // Needed b/c icons don't show whether night or not
    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        if (icon == null)
            return WeatherIcons.NA;

        icon = getNeutralIconName(icon);

        switch (icon) {
            case "clearsky":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_CLEAR;
                else
                    weatherIcon = WeatherIcons.DAY_SUNNY;
                break;

            case "fair":
            case "partlycloudy":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
                else
                    weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
                break;

            case "cloudy":
                weatherIcon = WeatherIcons.CLOUDY;
                break;

            case "rainshowers":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SPRINKLE;
                else
                    weatherIcon = WeatherIcons.DAY_SPRINKLE;
                break;

            case "rainshowersandthunder":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM;
                else
                    weatherIcon = WeatherIcons.DAY_THUNDERSTORM;
                break;

            case "sleetshowers":
            case "lightsleetshowers":
            case "heavysleetshowers":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SLEET;
                else
                    weatherIcon = WeatherIcons.DAY_SLEET;
                break;

            case "snowshowers":
            case "lightsnowshowers":
            case "heavysnowshowers":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SNOW;
                else
                    weatherIcon = WeatherIcons.DAY_SNOW;
                break;

            case "rain":
            case "lightrain":
                weatherIcon = WeatherIcons.SPRINKLE;
                break;

            case "heavyrain":
                weatherIcon = WeatherIcons.RAIN;
                break;

            case "heavyrainandthunder":
                weatherIcon = WeatherIcons.THUNDERSTORM;
                break;

            case "sleet":
            case "lightsleet":
            case "heavysleet":
                weatherIcon = WeatherIcons.SLEET;
                break;

            case "snow":
            case "lightsnow":
                weatherIcon = WeatherIcons.SNOW;
                break;

            case "snowandthunder":
            case "snowshowersandthunder":
            case "lightssnowshowersandthunder":
            case "heavysnowshowersandthunder":
            case "lightsnowandthunder":
            case "heavysnowandthunder":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM;
                else
                    weatherIcon = WeatherIcons.DAY_SNOW_THUNDERSTORM;
                break;

            case "fog":
                weatherIcon = WeatherIcons.FOG;
                break;

            case "sleetshowersandthunder":
            case "sleetandthunder":
            case "lightssleetshowersandthunder":
            case "heavysleetshowersandthunder":
            case "lightsleetandthunder":
            case "heavysleetandthunder":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SLEET_STORM;
                else
                    weatherIcon = WeatherIcons.DAY_SLEET_STORM;
                break;

            case "rainandthunder":
            case "lightrainandthunder":
            case "lightrainshowersandthunder":
            case "heavyrainshowersandthunder":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_STORM_SHOWERS;
                else
                    weatherIcon = WeatherIcons.DAY_STORM_SHOWERS;
                break;

            case "lightrainshowers":
            case "heavyrainshowers":
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_RAIN;
                else
                    weatherIcon = WeatherIcons.DAY_RAIN;
                break;

            case "heavysnow":
                weatherIcon = WeatherIcons.SNOW_WIND;
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

    @Override
    public String getWeatherCondition(String icon) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        if (icon == null)
            return context.getString(R.string.weather_notavailable);

        icon = getNeutralIconName(icon);

        switch (icon) {
            case "clearsky":
                return context.getString(R.string.weather_clearsky);

            case "fair":
                return context.getString(R.string.weather_fair);
            case "partlycloudy":
                return context.getString(R.string.weather_partlycloudy);

            case "cloudy":
                return context.getString(R.string.weather_cloudy);

            case "rainshowers":
                return context.getString(R.string.weather_rainshowers);

            case "rainshowersandthunder":
                return context.getString(R.string.weather_tstorms);

            case "sleetshowers":
            case "lightsleetshowers":
            case "sleet":
            case "lightsleet":
            case "heavysleet":
            case "heavysleetshowers":
                return context.getString(R.string.weather_sleet);

            case "snow":
            case "snowshowers":
                return context.getString(R.string.weather_snow);

            case "lightsnowshowers":
            case "lightsnow":
                return context.getString(R.string.weather_lightsnowshowers);

            case "heavysnowshowers":
            case "heavysnow":
                return context.getString(R.string.weather_heavysnow);

            case "rain":
                return context.getString(R.string.weather_rain);
            case "lightrain":
                return context.getString(R.string.weather_lightrain);

            case "heavyrain":
                return context.getString(R.string.weather_heavyrain);

            case "rainandthunder":
            case "lightrainandthunder":
            case "lightrainshowersandthunder":
            case "heavyrainshowersandthunder":
            case "heavyrainandthunder":
                return context.getString(R.string.weather_tstorms);

            case "snowandthunder":
            case "snowshowersandthunder":
            case "lightssnowshowersandthunder":
            case "heavysnowshowersandthunder":
            case "lightsnowandthunder":
            case "heavysnowandthunder":
                return context.getString(R.string.weather_snow_tstorms);

            case "fog":
                return context.getString(R.string.weather_fog);

            case "sleetshowersandthunder":
            case "sleetandthunder":
            case "lightssleetshowersandthunder":
            case "heavysleetshowersandthunder":
            case "lightsleetandthunder":
            case "heavysleetandthunder":
                return context.getString(R.string.weather_sleet_tstorms);

            case "lightrainshowers":
            case "heavyrainshowers":
                return context.getString(R.string.weather_rainshowers);

            default:
                return super.getWeatherCondition(icon);
        }
    }

    // Met.no conditions can be for any time of day
    // So use sunrise/set data as fallback
    @Override
    public boolean isNight(Weather weather) {
        boolean isNight = super.isNight(weather);

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

        return isNight;
    }
}
