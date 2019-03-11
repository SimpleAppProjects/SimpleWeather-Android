package com.thewizrd.shared_resources.weatherdata.metno;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.widget.Toast;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
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

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.TreeStrategy;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public final class MetnoWeatherProvider extends WeatherProviderImpl {

    public MetnoWeatherProvider() {
        super();
        locationProvider = new HERELocationProvider();
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

    class IgnoreClassAttrAnnotStrategy extends AnnotationStrategy {
        public IgnoreClassAttrAnnotStrategy() {
            super(new TreeStrategy("noClass", "noLength"));
        }
    }

    @Override
    public Weather getWeather(String location_query) throws WeatherException {
        Weather weather = null;

        String forecastAPI = null;
        URL forecastURL = null;
        String sunrisesetAPI = null;
        URL sunrisesetURL = null;
        String query = null;
        HttpURLConnection client = null;

        WeatherException wEx = null;

        try {
            forecastAPI = "https://api.met.no/weatherapi/locationforecastlts/1.3/?%s";
            forecastURL = new URL(String.format(forecastAPI, location_query));
            sunrisesetAPI = "https://api.met.no/weatherapi/sunrise/2.0/?%s&date=%s&offset=+00:00";
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT));
            sunrisesetURL = new URL(String.format(sunrisesetAPI, location_query, date));

            InputStream forecastStream = null;
            InputStream sunrisesetStream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) forecastURL.openConnection();
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept-Encoding", "gzip");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));
            if ("gzip".equals(client.getContentEncoding())) {
                forecastStream = new GZIPInputStream(client.getInputStream());
            } else {
                forecastStream = client.getInputStream();
            }

            client = (HttpURLConnection) sunrisesetURL.openConnection();
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept-Encoding", "gzip");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));
            if ("gzip".equals(client.getContentEncoding())) {
                sunrisesetStream = new GZIPInputStream(client.getInputStream());
            } else {
                sunrisesetStream = client.getInputStream();
            }

            // Reset exception
            wEx = null;

            // Load weather
            Weatherdata foreRoot = null;
            Astrodata astroRoot = null;
            // TODO: put in async task?
            Serializer deserializer = new Persister(new IgnoreClassAttrAnnotStrategy());
            foreRoot = deserializer.read(Weatherdata.class, forecastStream, false);
            // TODO: put in async task?
            astroRoot = deserializer.read(Astrodata.class, sunrisesetStream, false);

            // End Stream
            forecastStream.close();
            sunrisesetStream.close();

            weather = new Weather(foreRoot, astroRoot);

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
            Logger.writeLine(Log.ERROR, ex, "MetnoWeatherProvider: error getting weather data");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (weather == null || !weather.isValid()) {
            wEx = new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        } else if (weather != null) {
            weather.setQuery(location_query);

            for (Forecast forecast : weather.getForecast()) {
                forecast.setCondition(getWeatherCondition(forecast.getIcon()));
                forecast.setIcon(getWeatherIcon(forecast.getIcon()));
            }
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
        if (weather.getAstronomy().getSunrise().compareTo(LocalDateTime.MIN) > 0 &&
                weather.getAstronomy().getSunrise().toLocalTime().compareTo(LocalTime.MAX) < 0)
            weather.getAstronomy().setSunrise(weather.getAstronomy().getSunrise().plusSeconds(offset.getTotalSeconds()));
        if (weather.getAstronomy().getSunset().compareTo(LocalDateTime.MIN) > 0 &&
                weather.getAstronomy().getSunset().toLocalTime().compareTo(LocalTime.MAX) < 0)
            weather.getAstronomy().setSunset(weather.getAstronomy().getSunset().plusSeconds(offset.getTotalSeconds()));

        // Set condition here
        LocalTime now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime();
        LocalTime sunrise = weather.getAstronomy().getSunrise().toLocalTime();
        LocalTime sunset = weather.getAstronomy().getSunset().toLocalTime();

        weather.getCondition().setWeather(getWeatherCondition(weather.getCondition().getIcon()));
        weather.getCondition().setIcon(getWeatherIcon(now.compareTo(sunrise) < 0 || now.compareTo(sunset) > 0, weather.getCondition().getIcon()));

        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
        }

        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            hr_forecast.setDate(hr_forecast.getDate().withZoneSameInstant(offset));

            LocalTime hrnow = hr_forecast.getDate().toLocalTime();
            LocalTime sunriseTime = weather.getAstronomy().getSunrise().toLocalTime();
            LocalTime sunsetTime = weather.getAstronomy().getSunset().toLocalTime();

            hr_forecast.setCondition(getWeatherCondition(hr_forecast.getIcon()));
            hr_forecast.setIcon(getWeatherIcon(hrnow.compareTo(sunriseTime) < 0 || hrnow.compareTo(sunsetTime) > 0, hr_forecast.getIcon()));
        }

        return weather;
    }

    // TODO: Move this out
    public static String getWeatherCondition(String icon) {
        String condition = "";

        switch (icon) {
            case "1": // Sun
                condition = "Clear";
                break;
            case "2": // LightCloud
            case "3": // PartlyCloud
                condition = "Partly Cloudy";
                break;
            case "4": // Cloud
                condition = "Mostly Cloudy";
                break;
            case "5": // LightRainSun
            case "6": // LightRainThunderSun
            case "9": // LightRain
            case "22": // LightRainThunder
                condition = "Light Rain";
                break;
            case "7": // SleetSun
            case "12": // Sleet
            case "20": // SleetSunThunder
            case "23": // SleetThunder
            case "26": // LightSleetThunderSun
            case "27": // HeavySleetThunderSun
            case "31": // LightSleetThunder
            case "32": // HeavySleetThunder
            case "42": // LightSleetSun
            case "43": // HeavySleetSun
            case "47": // LightSleet
            case "48": // HeavySleet
                condition = "Sleet";
                break;
            case "8": // SnowSun
            case "13": // Snow
            case "14": // SnowThunder
            case "21": // SnowSunThunder
                condition = "Snow";
                break;
            case "10": // Rain
            case "11": // RainThunder
            case "25": // RainThunderSun
            case "41": // RainSun
                condition = "Rain";
                break;
            case "15": // Fog
                condition = "Fog";
                break;
            case "24": // DrizzleThunderSun
            case "30": // DrizzleThunder
            case "40": // DrizzleSun
            case "46": // Drizzle
                condition = "Drizzle";
                break;
            case "28": // LightSnowThunderSun
            case "33": // LightSnowThunder
            case "44": // LightSnowSun
            case "49": // LightSnow
                condition = "Light Snow";
                break;
            case "29": // HeavySnowThunderSun
            case "34": // HeavySnowThunder
            case "45": // HeavySnowSun
            case "50": // HeavySnow
                condition = "Heavy Snow";
                break;
            default:
                condition = Weather.NA;
                break;
        }

        return condition;
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
        return getWeatherIcon(false, icon);
    }

    // Needed b/c icons don't show whether night or not
    @Override
    public String getWeatherIcon(boolean isNight, String icon) {
        String weatherIcon = "";

        switch (icon) {
            case "1": // Sun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_CLEAR;
                else
                    weatherIcon = WeatherIcons.DAY_SUNNY;
                break;

            case "2": // LightCloud
            case "3": // PartlyCloud
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY;
                else
                    weatherIcon = WeatherIcons.DAY_SUNNY_OVERCAST;
                break;

            case "4": // Cloud
                weatherIcon = WeatherIcons.CLOUDY;
                break;

            case "5": // LightRainSun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SPRINKLE;
                else
                    weatherIcon = WeatherIcons.DAY_SPRINKLE;
                break;

            case "6": // LightRainThunderSun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_THUNDERSTORM;
                else
                    weatherIcon = WeatherIcons.DAY_THUNDERSTORM;
                break;

            case "7": // SleetSun
            case "42": // LightSleetSun
            case "43": // HeavySleetSun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SLEET;
                else
                    weatherIcon = WeatherIcons.DAY_SLEET;
                break;

            case "8": // SnowSun
            case "44": // LightSnowSun
            case "45": // HeavySnowSun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SNOW;
                else
                    weatherIcon = WeatherIcons.DAY_SNOW;
                break;

            case "9": // LightRain
            case "46": // Drizzle
                weatherIcon = WeatherIcons.SPRINKLE;
                break;

            case "10": // Rain
                weatherIcon = WeatherIcons.RAIN;
                break;

            case "11": // RainThunder
                weatherIcon = WeatherIcons.THUNDERSTORM;
                break;

            case "12": // Sleet
            case "47": // LightSleet
            case "48": // HeavySleet
                weatherIcon = WeatherIcons.SLEET;
                break;

            case "13": // Snow
            case "49": // LightSnow
                weatherIcon = WeatherIcons.SNOW;
                break;

            case "14": // SnowThunder
            case "21": // SnowSunThunder
            case "28": // LightSnowThunderSun
            case "29": // HeavySnowThunderSun
            case "33": // LightSnowThunder
            case "34": // HeavySnowThunder
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM;
                else
                    weatherIcon = WeatherIcons.DAY_SNOW_THUNDERSTORM;
                break;

            case "15": // Fog
                weatherIcon = WeatherIcons.FOG;
                break;

            case "20": // SleetSunThunder
            case "23": // SleetThunder
            case "26": // LightSleetThunderSun
            case "27": // HeavySleetThunderSun
            case "31": // LightSleetThunder
            case "32": // HeavySleetThunder
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_SLEET_STORM;
                else
                    weatherIcon = WeatherIcons.DAY_SLEET_STORM;
                break;

            case "22": // LightRainThunder
            case "30": // DrizzleThunder
            case "24": // DrizzleThunderSun
            case "25": // RainThunderSun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_STORM_SHOWERS;
                else
                    weatherIcon = WeatherIcons.DAY_STORM_SHOWERS;
                break;

            case "40": // DrizzleSun
            case "41": // RainSun
                if (isNight)
                    weatherIcon = WeatherIcons.NIGHT_ALT_RAIN;
                else
                    weatherIcon = WeatherIcons.DAY_RAIN;
                break;

            case "50": // HeavySnow
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

    // Met.no conditions can be for any time of day
    // So use sunrise/set data as fallback
    @Override
    public boolean isNight(Weather weather) {
        boolean isNight = super.isNight(weather);

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

        return isNight;
    }
}
