package com.thewizrd.shared_resources.weatherdata.metno;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public final class MetnoWeatherProvider extends WeatherProviderImpl {

    public MetnoWeatherProvider() {
        super();
        locationProvider = new LocationIQProvider();
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
            forecastAPI = "https://api.met.no/weatherapi/locationforecast/2.0/complete.json?%s";
            forecastURL = new URL(String.format(forecastAPI, location_query));
            sunrisesetAPI = "https://api.met.no/weatherapi/sunrise/2.0/.json?%s&date=%s&offset=+00:00";
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT));
            sunrisesetURL = new URL(String.format(sunrisesetAPI, location_query, date));

            InputStream forecastStream = null;
            InputStream sunrisesetStream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) forecastURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept-Encoding", "gzip");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));
            if ("gzip".equals(client.getContentEncoding())) {
                forecastStream = new GZIPInputStream(client.getInputStream());
            } else {
                forecastStream = client.getInputStream();
            }

            client = (HttpURLConnection) sunrisesetURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);
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
        if (weather.getAstronomy().getMoonrise().compareTo(LocalDateTime.MIN) > 0 &&
                weather.getAstronomy().getMoonrise().toLocalTime().compareTo(LocalTime.MAX) < 0)
            weather.getAstronomy().setMoonrise(weather.getAstronomy().getMoonrise().plusSeconds(offset.getTotalSeconds()));
        if (weather.getAstronomy().getMoonset().compareTo(LocalDateTime.MIN) > 0 &&
                weather.getAstronomy().getMoonset().toLocalTime().compareTo(LocalTime.MAX) < 0)
            weather.getAstronomy().setMoonset(weather.getAstronomy().getMoonset().plusSeconds(offset.getTotalSeconds()));

        // Set condition here
        LocalTime now = ZonedDateTime.now(ZoneOffset.UTC).withZoneSameInstant(offset).toLocalTime();
        LocalTime sunrise = weather.getAstronomy().getSunrise().toLocalTime();
        LocalTime sunset = weather.getAstronomy().getSunset().toLocalTime();

        weather.getCondition().setWeather(getWeatherCondition(weather.getCondition().getIcon()));
        weather.getCondition().setIcon(getWeatherIcon(now.compareTo(sunrise) < 0 || now.compareTo(sunset) > 0, weather.getCondition().getIcon()));
        weather.getCondition().setObservationTime(weather.getCondition().getObservationTime().withZoneSameInstant(offset));

        for (Forecast forecast : weather.getForecast()) {
            forecast.setDate(forecast.getDate().plusSeconds(offset.getTotalSeconds()));
            forecast.setCondition(getWeatherCondition(forecast.getIcon()));
            forecast.setIcon(getWeatherIcon(forecast.getIcon()));
        }

        for (HourlyForecast hr_forecast : weather.getHrForecast()) {
            hr_forecast.setDate(hr_forecast.getDate().withZoneSameInstant(offset));

            hr_forecast.setCondition(getWeatherCondition(hr_forecast.getIcon()));
            hr_forecast.setIcon(getWeatherIcon(hr_forecast.getIcon() != null &&
                            (hr_forecast.getIcon().endsWith("_night") || hr_forecast.getIcon().endsWith("_polartwilight")),
                    hr_forecast.getIcon()));
        }

        return weather;
    }

    private static final String LEGEND_JSON = "{'fog':{'variants':null,'desc_en':'Fog','old_id':'15','desc_nb':'Tåke','desc_nn':'Skodde'},'heavysnow':{'desc_nb':'Kraftig snø','desc_nn':'Kraftig snø','variants':null,'old_id':'50','desc_en':'Heavy snow'},'sleet':{'desc_nb':'Sludd','desc_nn':'Sludd','variants':null,'old_id':'12','desc_en':'Sleet'},'lightrainshowers':{'desc_nn':'Lette regnbyer','desc_nb':'Lette regnbyger','desc_en':'Light rain showers','old_id':'40','variants':['day','night','polartwilight']},'clearsky':{'desc_nb':'Klarvær','desc_nn':'Klårvêr','variants':['day','night','polartwilight'],'old_id':'1','desc_en':'Clear sky'},'sleetshowersandthunder':{'desc_nn':'Sluddbyer og torevêr','desc_nb':'Sluddbyger og torden','old_id':'20','desc_en':'Sleet showers and thunder','variants':['day','night','polartwilight']},'lightrainshowersandthunder':{'desc_nn':'Lette regnbyer og torevêr','desc_nb':'Lette regnbyger og torden','old_id':'24','desc_en':'Light rain showers and thunder','variants':['day','night','polartwilight']},'heavysnowshowers':{'variants':['day','night','polartwilight'],'old_id':'45','desc_en':'Heavy snow showers','desc_nb':'Kraftige snøbyger','desc_nn':'Kraftige snøbyer'},'lightssnowshowersandthunder':{'variants':['day','night','polartwilight'],'desc_en':'Lights snow showers and thunder','old_id':'28','desc_nb':'Lette snøbyger og torden','desc_nn':'Lette snøbyer og torevêr'},'lightsnowandthunder':{'old_id':'33','desc_en':'Light snow and thunder','variants':null,'desc_nn':'Lett snø og torevêr','desc_nb':'Lett snø og torden'},'lightrainandthunder':{'variants':null,'desc_en':'Light rain and thunder','old_id':'30','desc_nb':'Lett regn og torden','desc_nn':'Lett regn og torevêr'},'snowshowersandthunder':{'variants':['day','night','polartwilight'],'desc_en':'Snow showers and thunder','old_id':'21','desc_nb':'Snøbyger og torden','desc_nn':'Snøbyer og torevêr'},'partlycloudy':{'desc_en':'Partly cloudy','old_id':'3','variants':['day','night','polartwilight'],'desc_nn':'Delvis skya','desc_nb':'Delvis skyet'},'sleetandthunder':{'desc_nb':'Sludd og torden','desc_nn':'Sludd og torevêr','variants':null,'desc_en':'Sleet and thunder','old_id':'23'},'lightsleet':{'desc_nn':'Lett sludd','desc_nb':'Lett sludd','desc_en':'Light sleet','old_id':'47','variants':null},'lightsleetshowers':{'old_id':'42','desc_en':'Light sleet showers','variants':['day','night','polartwilight'],'desc_nn':'Lette sluddbyer','desc_nb':'Lette sluddbyger'},'heavysnowshowersandthunder':{'desc_nn':'Kraftige snøbyer og torevêr','desc_nb':'Kraftige snøbyger og torden','desc_en':'Heavy snow showers and thunder','old_id':'29','variants':['day','night','polartwilight']},'heavyrainandthunder':{'desc_nn':'Kraftig regn og torevêr','desc_nb':'Kraftig regn og torden','desc_en':'Heavy rain and thunder','old_id':'11','variants':null},'lightssleetshowersandthunder':{'old_id':'26','desc_en':'Lights sleet showers and thunder','variants':['day','night','polartwilight'],'desc_nn':'Lette sluddbyer og torevêr','desc_nb':'Lette sluddbyger og torden'},'lightsnow':{'desc_nb':'Lett snø','desc_nn':'Lett snø','variants':null,'old_id':'49','desc_en':'Light snow'},'rainshowersandthunder':{'variants':['day','night','polartwilight'],'old_id':'6','desc_en':'Rain showers and thunder','desc_nb':'Regnbyger og torden','desc_nn':'Regnbyer og torevêr'},'heavyrainshowersandthunder':{'variants':['day','night','polartwilight'],'old_id':'25','desc_en':'Heavy rain showers and thunder','desc_nb':'Kraftige regnbyger og torden','desc_nn':'Kraftige regnbyer og torevêr'},'heavyrainshowers':{'variants':['day','night','polartwilight'],'desc_en':'Heavy rain showers','old_id':'41','desc_nb':'Kraftige regnbyger','desc_nn':'Kraftige regnbyer'},'fair':{'variants':['day','night','polartwilight'],'desc_en':'Fair','old_id':'2','desc_nb':'Lettskyet','desc_nn':'Lettskya'},'sleetshowers':{'variants':['day','night','polartwilight'],'old_id':'7','desc_en':'Sleet showers','desc_nb':'Sluddbyger','desc_nn':'Sluddbyer'},'lightsnowshowers':{'desc_nb':'Lette snøbyger','desc_nn':'Lette snøbyer','variants':['day','night','polartwilight'],'old_id':'44','desc_en':'Light snow showers'},'rainandthunder':{'desc_nb':'Regn og torden','desc_nn':'Regn og torevêr','variants':null,'old_id':'22','desc_en':'Rain and thunder'},'heavyrain':{'desc_en':'Heavy rain','old_id':'10','variants':null,'desc_nn':'Kraftig regn','desc_nb':'Kraftig regn'},'rainshowers':{'old_id':'5','desc_en':'Rain showers','variants':['day','night','polartwilight'],'desc_nn':'Regnbyer','desc_nb':'Regnbyger'},'snowandthunder':{'desc_en':'Snow and thunder','old_id':'14','variants':null,'desc_nn':'Snø og torevêr','desc_nb':'Snø og torden'},'lightrain':{'desc_en':'Light rain','old_id':'46','variants':null,'desc_nn':'Lett regn','desc_nb':'Lett regn'},'rain':{'variants':null,'desc_en':'Rain','old_id':'9','desc_nb':'Regn','desc_nn':'Regn'},'heavysleet':{'variants':null,'old_id':'48','desc_en':'Heavy sleet','desc_nb':'Kraftig sludd','desc_nn':'Kraftig sludd'},'heavysleetandthunder':{'desc_nb':'Kraftig sludd og torden','desc_nn':'Kraftig sludd og torevêr','variants':null,'old_id':'32','desc_en':'Heavy sleet and thunder'},'snow':{'old_id':'13','desc_en':'Snow','variants':null,'desc_nn':'Snø','desc_nb':'Snø'},'lightsleetandthunder':{'desc_en':'Light sleet and thunder','old_id':'31','variants':null,'desc_nn':'Lett sludd og torevêr','desc_nb':'Lett sludd og torden'},'cloudy':{'desc_nn':'Skya','desc_nb':'Skyet','old_id':'4','desc_en':'Cloudy','variants':null},'heavysnowandthunder':{'desc_nn':'Kraftig snø og torevêr','desc_nb':'Kraftig snø og torden','desc_en':'Heavy snow and thunder','old_id':'34','variants':null},'heavysleetshowersandthunder':{'desc_nb':'Kraftige sluddbyger og torden','desc_nn':'Kraftige sluddbyer og torevêr','variants':['day','night','polartwilight'],'old_id':'27','desc_en':'Heavy sleet showers and thunder'},'heavysleetshowers':{'variants':['day','night','polartwilight'],'old_id':'43','desc_en':'Heavy sleet showers','desc_nb':'Kraftige sluddbyger','desc_nn':'Kraftige sluddbyer'},'snowshowers':{'old_id':'8','desc_en':'Snow showers','variants':['day','night','polartwilight'],'desc_nn':'Snøbyer','desc_nb':'Snøbyger'}}";
    private static JsonObject sLegendObject;

    static {
        sLegendObject = JsonParser.parseString(LEGEND_JSON).getAsJsonObject();
    }

    private static String getNeutralIconName(String icon_variant) {
        if (icon_variant == null)
            return "";

        return icon_variant.replace("_day", "").replace("_night", "").replace("_polartwilight", "");
    }

    private static String getWeatherCondition(String icon) {
        String icon_neutral = getNeutralIconName(icon);

        JsonElement icon_obj = sLegendObject.get(icon_neutral);

        if (icon_obj != null) {
            JsonObject condition_obj = icon_obj.getAsJsonObject();
            String condition = condition_obj.get("desc_en").getAsString();
            return condition;
        }

        return Weather.NA;
    }

    @Override
    public String updateLocationQuery(Weather weather) {
        return String.format(Locale.ROOT, "lat=%s&lon=%s", weather.getLocation().getLatitude(), weather.getLocation().getLongitude());
    }

    @Override
    public String updateLocationQuery(LocationData location) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("#.####");

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
