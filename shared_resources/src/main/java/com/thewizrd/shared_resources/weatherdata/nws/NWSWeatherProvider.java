package com.thewizrd.shared_resources.weatherdata.nws;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider;
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Astronomy;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.shared_resources.weatherdata.nws.hourly.HourlyForecastResponse;
import com.thewizrd.shared_resources.weatherdata.nws.hourly.Location;
import com.thewizrd.shared_resources.weatherdata.nws.hourly.PeriodsItem;
import com.thewizrd.shared_resources.weatherdata.nws.observation.ForecastResponse;
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NWSWeatherProvider extends WeatherProviderImpl {
    private static final String FORECAST_QUERY_URL = "https://forecast.weather.gov/MapClick.php?%s&FcstType=json";
    private static final String HRFORECAST_QUERY_URL = "https://forecast.weather.gov/MapClick.php?%s&FcstType=digitalJSON";

    public NWSWeatherProvider() {
        super();

        locationProvider = RemoteConfig.getLocationProvider(getWeatherAPI());
        if (locationProvider == null) {
            locationProvider = new WeatherApiLocationProvider();
        }
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
        Response observationResponse = null;
        Response forecastResponse = null;
        WeatherException wEx = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            Request observationRequest = new Request.Builder()
                    .cacheControl(new CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build())
                    .url(String.format(FORECAST_QUERY_URL, location_query))
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            observationResponse = client.newCall(observationRequest).execute();

            // Check for errors
            checkForErrors(observationResponse.code());

            final InputStream observationStream = observationResponse.body().byteStream();

            // Load point json data
            ForecastResponse observationData = JSONParser.deserializer(observationStream, ForecastResponse.class);

            // End Stream
            observationStream.close();

            Request hrForecastRequest = new Request.Builder()
                    .url(String.format(HRFORECAST_QUERY_URL, location_query))
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            forecastResponse = client.newCall(hrForecastRequest).execute();

            // Check for errors
            checkForErrors(forecastResponse.code());

            final InputStream forecastStream = forecastResponse.body().byteStream();

            // Load point json data
            HourlyForecastResponse forecastData = createHourlyForecastResponse(forecastStream);

            // End Stream
            forecastStream.close();

            weather = new Weather(observationData, forecastData);
        } catch (Exception ex) {
            weather = null;
            if (ex instanceof IOException) {
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            }
            Logger.writeLine(Log.ERROR, ex, "NWSWeatherProvider: error getting weather data");
        } finally {
            if (observationResponse != null)
                observationResponse.close();
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

    private HourlyForecastResponse createHourlyForecastResponse(@NonNull InputStream forecastStream) {
        HourlyForecastResponse forecastData = new HourlyForecastResponse();
        JsonStreamParser forecastParser = new JsonStreamParser(new InputStreamReader(forecastStream));

        if (forecastParser.hasNext()) {
            JsonElement element = forecastParser.next();
            JsonObject fcastRoot = element.getAsJsonObject();

            forecastData.setCreationDate(fcastRoot.get("creationDate").getAsString());
            forecastData.setLocation(new Location());

            JsonObject location = fcastRoot.getAsJsonObject("location");
            forecastData.getLocation().setLatitude(location.getAsJsonPrimitive("latitude").getAsDouble());
            forecastData.getLocation().setLongitude(location.getAsJsonPrimitive("longitude").getAsDouble());

            JsonObject periodNameList = fcastRoot.getAsJsonObject("PeriodNameList");
            SortedSet<String> sortedKeys = new TreeSet<>(periodNameList.keySet());

            forecastData.setPeriodsItems(new ArrayList<>(sortedKeys.size()));

            for (final String periodNumber : sortedKeys) {
                final String periodName = periodNameList.getAsJsonPrimitive(periodNumber).getAsString();

                if (!fcastRoot.has(periodName))
                    continue;

                final PeriodsItem item = new PeriodsItem();

                JsonObject periodObj = fcastRoot.getAsJsonObject(periodName);
                JsonArray unixTimeArr = periodObj.getAsJsonArray("unixtime");
                JsonArray windChillArr = periodObj.getAsJsonArray("windChill");
                JsonArray windSpeedArr = periodObj.getAsJsonArray("windSpeed");
                JsonArray cloudAmtArr = periodObj.getAsJsonArray("cloudAmount");
                JsonArray popArr = periodObj.getAsJsonArray("pop");
                JsonArray humidityArr = periodObj.getAsJsonArray("relativeHumidity");
                JsonArray windGustArr = periodObj.getAsJsonArray("windGust");
                JsonArray tempArr = periodObj.getAsJsonArray("temperature");
                JsonArray windDirArr = periodObj.getAsJsonArray("windDirection");
                JsonArray iconArr = periodObj.getAsJsonArray("iconLink");
                JsonArray conditionTxtArr = periodObj.getAsJsonArray("weather");

                item.setPeriodName(periodObj.getAsJsonPrimitive("periodName").getAsString());

                item.setUnixtime(new ArrayList<>(unixTimeArr.size()));
                for (JsonElement jsonElement : unixTimeArr) {
                    String time = jsonElement.getAsString();
                    item.getUnixtime().add(time);
                }

                item.setWindChill(new ArrayList<>(windChillArr.size()));
                for (JsonElement jsonElement : windChillArr) {
                    String windChill = jsonElement.getAsString();
                    item.getWindChill().add(windChill);
                }

                item.setWindSpeed(new ArrayList<>(windSpeedArr.size()));
                for (JsonElement jsonElement : windSpeedArr) {
                    String windSpeed = jsonElement.getAsString();
                    item.getWindSpeed().add(windSpeed);
                }

                item.setCloudAmount(new ArrayList<>(cloudAmtArr.size()));
                for (JsonElement jsonElement : cloudAmtArr) {
                    String cloudAmt = jsonElement.getAsString();
                    item.getCloudAmount().add(cloudAmt);
                }

                item.setPop(new ArrayList<>(popArr.size()));
                for (JsonElement jsonElement : popArr) {
                    String pop = jsonElement.getAsString();
                    item.getPop().add(pop);
                }

                item.setRelativeHumidity(new ArrayList<>(humidityArr.size()));
                for (JsonElement jsonElement : humidityArr) {
                    String humidity = jsonElement.getAsString();
                    item.getRelativeHumidity().add(humidity);
                }

                item.setWindGust(new ArrayList<>(windGustArr.size()));
                for (JsonElement jsonElement : windGustArr) {
                    String windGust = jsonElement.getAsString();
                    item.getWindGust().add(windGust);
                }

                item.setTemperature(new ArrayList<>(tempArr.size()));
                for (JsonElement jsonElement : tempArr) {
                    String temp = jsonElement.getAsString();
                    item.getTemperature().add(temp);
                }

                item.setWindDirection(new ArrayList<>(windDirArr.size()));
                for (JsonElement jsonElement : windDirArr) {
                    String windDir = jsonElement.getAsString();
                    item.getWindDirection().add(windDir);
                }

                item.setIconLink(new ArrayList<>(iconArr.size()));
                for (JsonElement jsonElement : iconArr) {
                    String icon = jsonElement.getAsString();
                    item.getIconLink().add(icon);
                }

                item.setWeather(new ArrayList<>(conditionTxtArr.size()));
                for (JsonElement jsonElement : conditionTxtArr) {
                    String condition = jsonElement.getAsString();
                    item.getWeather().add(condition);
                }

                forecastData.getPeriodsItems().add(item);
            }
        }

        return forecastData;
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

        ZoneOffset offset = location.getTzOffset();

        weather.setUpdateTime(weather.getUpdateTime().withZoneSameInstant(offset));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(offset));

        // NWS does not provide astrodata; calculate this ourselves (using their calculator)
        Astronomy solCalcData = new SolCalcAstroProvider().getAstronomyData(location, weather.getCondition().getObservationTime());
        weather.setAstronomy(new SunMoonCalcProvider().getAstronomyData(location, weather.getCondition().getObservationTime()));
        weather.getAstronomy().setSunrise(solCalcData.getSunrise());
        weather.getAstronomy().setSunset(solCalcData.getSunset());

        // Update icons
        LocalTime now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime();
        LocalTime sunrise = weather.getAstronomy().getSunrise().toLocalTime();
        LocalTime sunset = weather.getAstronomy().getSunset().toLocalTime();

        weather.getCondition().setIcon(getWeatherIcon(now.isBefore(sunrise) || now.isAfter(sunset), weather.getCondition().getIcon()));

        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            ZonedDateTime hrf_date = hr_forecast.getDate().withZoneSameInstant(offset);
            hr_forecast.setDate(hrf_date);

            LocalTime hrf_localTime = hrf_date.toLocalTime();
            hr_forecast.setIcon(getWeatherIcon(hrf_localTime.isBefore(sunrise) || hrf_localTime.isAfter(sunset), hr_forecast.getIcon()));
        }

        return weather;
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
        return String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(location.getLatitude()), location.getLongitude());
    }

    @Override
    public String getWeatherIcon(String icon) {
        return getWeatherIcon(false, icon);
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
            return context.getString(R.string.weather_notavailable);

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
            return super.getWeatherCondition(icon);
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
