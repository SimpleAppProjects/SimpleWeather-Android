package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.SparseArray;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertSeverity;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType;
import com.thewizrd.shared_resources.weatherdata.WeatherBackground;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelperImpl;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class WeatherUtils {

    public static String getLastBuildDate(Weather weather) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();
        String date;
        String prefix;
        LocalDateTime update_time = weather.getUpdateTime().toLocalDateTime();
        String timeformat = update_time.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()));

        if (DateFormat.is24HourFormat(context))
            timeformat = update_time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        else
            timeformat = update_time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()));

        timeformat = String.format("%s %s", timeformat, weather.getLocation().getTzShort());

        if (update_time.getDayOfWeek() == ZonedDateTime.now().getDayOfWeek()) {
            prefix = context.getString(R.string.update_prefix_day);
            date = String.format("%s %s", prefix, timeformat);
        } else {
            prefix = context.getString(R.string.update_prefix);
            date = String.format("%s %s %s", prefix, update_time.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), timeformat);
        }

        return date;
    }

    public static String getFeelsLikeTemp(String temp_f, String wind_mph, String humidity_percent) {
        String feelslikeTemp = temp_f;

        float temp = -99.00f;
        float windmph = -1f;
        int humidity = -1;

        try {
            temp = Float.parseFloat(temp_f);
        } catch (NumberFormatException e) {
        }
        try {
            windmph = Float.parseFloat(wind_mph);
        } catch (NumberFormatException e) {
        }
        try {
            humidity = Integer.parseInt(humidity_percent);
        } catch (NumberFormatException e) {
        }

        if (temp > -99.00f) {
            if (temp < 50 && windmph > -1f) {
                feelslikeTemp = Float.toString(calculateWindChill(temp, windmph));
            } else if (temp > 80 && humidity > -1) {
                feelslikeTemp = Double.toString(calculateHeatIndex(temp, humidity));
            } else
                feelslikeTemp = temp_f;
        }

        return feelslikeTemp;
    }

    public static float calculateWindChill(float temp_f, float wind_mph) {
        if (temp_f < 50)
            return (float) (35.74f + (0.6215f * temp_f) - (35.75f * Math.pow(wind_mph, 0.16f)) + (0.4275f * temp_f * Math.pow(wind_mph, 0.16f)));
        else
            return temp_f;
    }

    public static double calculateHeatIndex(float temp_f, int humidity) {
        if (temp_f > 80) {
            double HI = -42.379
                    + (2.04901523 * temp_f)
                    + (10.14333127 * humidity)
                    - (0.22475541 * temp_f * humidity)
                    - (0.00683783 * Math.pow(temp_f, 2))
                    - (0.05481717 * Math.pow(humidity, 2))
                    + (0.00122874 * Math.pow(temp_f, 2) * humidity)
                    + (0.00085282 * temp_f * Math.pow(humidity, 2))
                    - (0.00000199 * Math.pow(temp_f, 2) * Math.pow(humidity, 2));

            if (humidity < 13 && (temp_f > 80 && temp_f < 112)) {
                double adj = ((13 - humidity) / 4f) * Math.sqrt((17 - Math.abs(temp_f - 95)) / 17);
                HI -= adj;
            } else if (humidity > 85 && (temp_f > 80 && temp_f < 87)) {
                double adj = ((humidity - 85) / 10f) * ((87 - temp_f) / 5);
                HI += adj;
            }

            if (HI > 80 && HI > temp_f)
                return HI;
            else
                return temp_f;
        } else
            return temp_f;
    }

    public static String getWindDirection(float angle) {
        if (angle >= 348.75 && angle <= 11.25) {
            return "N";
        } else if (angle >= 11.25 && angle <= 33.75) {
            return "NNE";
        } else if (angle >= 33.75 && angle <= 56.25) {
            return "NE";
        } else if (angle >= 56.25 && angle <= 78.75) {
            return "ENE";
        } else if (angle >= 78.75 && angle <= 101.25) {
            return "E";
        } else if (angle >= 101.25 && angle <= 123.75) {
            return "ESE";
        } else if (angle >= 123.75 && angle <= 146.25) {
            return "SE";
        } else if (angle >= 146.25 && angle <= 168.75) {
            return "SSE";
        } else if (angle >= 168.75 && angle <= 191.25) {
            return "S";
        } else if (angle >= 191.25 && angle <= 213.75) {
            return "SSW";
        } else if (angle >= 213.75 && angle <= 236.25) {
            return "SW";
        } else if (angle >= 236.25 && angle <= 258.75) {
            return "WSW";
        } else if (angle >= 258.75 && angle <= 281.25) {
            return "W";
        } else if (angle >= 281.25 && angle <= 303.75) {
            return "WNW";
        } else if (angle >= 303.75 && angle <= 326.25) {
            return "NW";
        } else/* if (angle >= 326.25 && angle <= 348.75)*/ {
            return "NNW";
        }
    }

    public static int getWindDirection(String direction) {
        if ("N".equals(direction)) {
            return 0;
        } else if ("NNE".equals(direction)) {
            return 22;
        } else if ("NE".equals(direction)) {
            return 45;
        } else if ("ENE".equals(direction)) {
            return 67;
        } else if ("E".equals(direction)) {
            return 90;
        } else if ("ESE".equals(direction)) {
            return 112;
        } else if ("SE".equals(direction)) {
            return 135;
        } else if ("SSE".equals(direction)) {
            return 157;
        } else if ("S".equals(direction)) {
            return 180;
        } else if ("SSW".equals(direction)) {
            return 202;
        } else if ("SW".equals(direction)) {
            return 225;
        } else if ("WSW".equals(direction)) {
            return 247;
        } else if ("W".equals(direction)) {
            return 270;
        } else if ("WNW".equals(direction)) {
            return 292;
        } else if ("NW".equals(direction)) {
            return 315;
        } else {
            return 337;
        }
    }

    public static int getDrawableFromAlertType(WeatherAlertType type) {
        int drawable = -1;

        switch (type) {
            case DENSEFOG:
                drawable = R.drawable.fog;
                break;
            case FIRE:
                drawable = R.drawable.fire;
                break;
            case FLOODWARNING:
            case FLOODWATCH:
                drawable = R.drawable.flood;
                break;
            case HEAT:
                drawable = R.drawable.hot;
                break;
            case HIGHWIND:
                drawable = R.drawable.strong_wind;
                break;
            case HURRICANELOCALSTATEMENT:
            case HURRICANEWINDWARNING:
                drawable = R.drawable.hurricane;
                break;
            case SEVERETHUNDERSTORMWARNING:
            case SEVERETHUNDERSTORMWATCH:
                drawable = R.drawable.thunderstorm;
                break;
            case TORNADOWARNING:
            case TORNADOWATCH:
                drawable = R.drawable.tornado;
                break;
            case VOLCANO:
                drawable = R.drawable.volcano;
                break;
            case WINTERWEATHER:
                drawable = R.drawable.snowflake_cold;
                break;
            case DENSESMOKE:
                drawable = R.drawable.smoke;
                break;
            case DUSTADVISORY:
                drawable = R.drawable.dust;
                break;
            case EARTHQUAKEWARNING:
                drawable = R.drawable.earthquake;
                break;
            case GALEWARNING:
                drawable = R.drawable.gale_warning;
                break;
            case SMALLCRAFT:
                drawable = R.drawable.small_craft_advisory;
                break;
            case STORMWARNING:
                drawable = R.drawable.storm_warning;
                break;
            case TSUNAMIWARNING:
            case TSUNAMIWATCH:
                drawable = R.drawable.tsunami;
                break;
            case SEVEREWEATHER:
            case SPECIALWEATHERALERT:
            default:
                drawable = R.drawable.ic_error_white;
                break;
        }

        return drawable;
    }

    public static int getColorFromAlertSeverity(WeatherAlertSeverity severity) {
        int color;

        switch (severity) {
            case SEVERE:
                color = Colors.ORANGERED;
                break;
            case EXTREME:
                color = Colors.RED;
                break;
            case MODERATE:
            default:
                color = Colors.ORANGE;
                break;
        }

        return color;
    }

    public static int getColorFromTempF(float temp_f) {
        int color;

        if (temp_f <= 47.5) {
            color = Colors.LIGHTBLUE;
        } else if (temp_f >= 85) {
            color = Colors.RED;
        } else if (temp_f >= 70) {
            color = Colors.ORANGE;
        } else {
            color = Colors.SIMPLEBLUE;
        }

        return color;
    }

    public static int getWeatherIconResource(String icon) {
        int weatherIcon = -1;

        switch (icon) {
            case WeatherIcons.DAY_SUNNY:
                weatherIcon = R.drawable.day_sunny;
                break;
            case WeatherIcons.DAY_CLOUDY:
                weatherIcon = R.drawable.day_cloudy;
                break;
            case WeatherIcons.DAY_CLOUDY_GUSTS:
                weatherIcon = R.drawable.day_cloudy_gusts;
                break;
            case WeatherIcons.DAY_CLOUDY_WINDY:
                weatherIcon = R.drawable.day_cloudy_windy;
                break;
            case WeatherIcons.DAY_FOG:
                weatherIcon = R.drawable.day_fog;
                break;
            case WeatherIcons.DAY_HAIL:
                weatherIcon = R.drawable.day_hail;
                break;
            case WeatherIcons.DAY_HAZE:
                weatherIcon = R.drawable.day_haze;
                break;
            case WeatherIcons.DAY_LIGHTNING:
                weatherIcon = R.drawable.day_lightning;
                break;
            case WeatherIcons.DAY_RAIN:
                weatherIcon = R.drawable.day_rain;
                break;
            case WeatherIcons.DAY_RAIN_MIX:
                weatherIcon = R.drawable.day_rain_mix;
                break;
            case WeatherIcons.DAY_RAIN_WIND:
                weatherIcon = R.drawable.day_rain_wind;
                break;
            case WeatherIcons.DAY_SHOWERS:
                weatherIcon = R.drawable.day_showers;
                break;
            case WeatherIcons.DAY_SLEET:
                weatherIcon = R.drawable.day_sleet;
                break;
            case WeatherIcons.DAY_SLEET_STORM:
                weatherIcon = R.drawable.day_sleet_storm;
                break;
            case WeatherIcons.DAY_SNOW:
                weatherIcon = R.drawable.day_snow;
                break;
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
                weatherIcon = R.drawable.day_snow_thunderstorm;
                break;
            case WeatherIcons.DAY_SNOW_WIND:
                weatherIcon = R.drawable.day_snow_wind;
                break;
            case WeatherIcons.DAY_SPRINKLE:
                weatherIcon = R.drawable.day_sprinkle;
                break;
            case WeatherIcons.DAY_STORM_SHOWERS:
                weatherIcon = R.drawable.day_storm_showers;
                break;
            case WeatherIcons.DAY_SUNNY_OVERCAST:
                weatherIcon = R.drawable.day_sunny_overcast;
                break;
            case WeatherIcons.DAY_THUNDERSTORM:
                weatherIcon = R.drawable.day_thunderstorm;
                break;
            case WeatherIcons.DAY_WINDY:
                weatherIcon = R.drawable.day_windy;
                break;
            case WeatherIcons.DAY_HOT:
                weatherIcon = R.drawable.hot;
                break;
            case WeatherIcons.DAY_CLOUDY_HIGH:
                weatherIcon = R.drawable.day_cloudy_high;
                break;
            case WeatherIcons.DAY_LIGHT_WIND:
                weatherIcon = R.drawable.day_light_wind;
                break;
            case WeatherIcons.NIGHT_CLEAR:
                weatherIcon = R.drawable.night_clear;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY:
                weatherIcon = R.drawable.night_alt_cloudy;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
                weatherIcon = R.drawable.night_alt_cloudy_gusts;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                weatherIcon = R.drawable.night_alt_cloudy_windy;
                break;
            case WeatherIcons.NIGHT_ALT_HAIL:
                weatherIcon = R.drawable.night_alt_hail;
                break;
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
                weatherIcon = R.drawable.night_alt_lightning;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN:
                weatherIcon = R.drawable.night_alt_rain;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
                weatherIcon = R.drawable.night_alt_rain_mix;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
                weatherIcon = R.drawable.night_alt_rain_wind;
                break;
            case WeatherIcons.NIGHT_ALT_SHOWERS:
                weatherIcon = R.drawable.night_alt_showers;
                break;
            case WeatherIcons.NIGHT_ALT_SLEET:
                weatherIcon = R.drawable.night_alt_sleet;
                break;
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
                weatherIcon = R.drawable.night_alt_sleet_storm;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW:
                weatherIcon = R.drawable.night_alt_snow;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
                weatherIcon = R.drawable.night_alt_snow_thunderstorm;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
                weatherIcon = R.drawable.night_alt_snow_wind;
                break;
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
                weatherIcon = R.drawable.night_alt_sprinkle;
                break;
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
                weatherIcon = R.drawable.night_alt_storm_showers;
                break;
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
                weatherIcon = R.drawable.night_alt_thunderstorm;
                break;
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                weatherIcon = R.drawable.night_alt_partly_cloudy;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
                weatherIcon = R.drawable.night_alt_cloudy_high;
                break;
            case WeatherIcons.NIGHT_FOG:
                weatherIcon = R.drawable.night_fog;
                break;
            case WeatherIcons.CLOUD:
                weatherIcon = R.drawable.cloud;
                break;
            case WeatherIcons.CLOUDY:
                weatherIcon = R.drawable.cloudy;
                break;
            case WeatherIcons.CLOUDY_GUSTS:
                weatherIcon = R.drawable.cloudy_gusts;
                break;
            case WeatherIcons.CLOUDY_WINDY:
                weatherIcon = R.drawable.cloudy_windy;
                break;
            case WeatherIcons.FOG:
                weatherIcon = R.drawable.fog;
                break;
            case WeatherIcons.HAIL:
                weatherIcon = R.drawable.hail;
                break;
            case WeatherIcons.RAIN:
                weatherIcon = R.drawable.rain;
                break;
            case WeatherIcons.RAIN_MIX:
                weatherIcon = R.drawable.rain_mix;
                break;
            case WeatherIcons.RAIN_WIND:
                weatherIcon = R.drawable.rain_wind;
                break;
            case WeatherIcons.SHOWERS:
                weatherIcon = R.drawable.showers;
                break;
            case WeatherIcons.SLEET:
                weatherIcon = R.drawable.sleet;
                break;
            case WeatherIcons.SNOW:
                weatherIcon = R.drawable.snow;
                break;
            case WeatherIcons.SPRINKLE:
                weatherIcon = R.drawable.sprinkle;
                break;
            case WeatherIcons.STORM_SHOWERS:
                weatherIcon = R.drawable.storm_showers;
                break;
            case WeatherIcons.THUNDERSTORM:
                weatherIcon = R.drawable.thunderstorm;
                break;
            case WeatherIcons.SNOW_WIND:
                weatherIcon = R.drawable.snow_wind;
                break;
            case WeatherIcons.SMOG:
                weatherIcon = R.drawable.smog;
                break;
            case WeatherIcons.SMOKE:
                weatherIcon = R.drawable.smoke;
                break;
            case WeatherIcons.LIGHTNING:
                weatherIcon = R.drawable.lightning;
                break;
            case WeatherIcons.DUST:
                weatherIcon = R.drawable.dust;
                break;
            case WeatherIcons.SNOWFLAKE_COLD:
                weatherIcon = R.drawable.snowflake_cold;
                break;
            case WeatherIcons.WINDY:
                weatherIcon = R.drawable.windy;
                break;
            case WeatherIcons.STRONG_WIND:
                weatherIcon = R.drawable.strong_wind;
                break;
            case WeatherIcons.SANDSTORM:
                weatherIcon = R.drawable.sandstorm;
                break;
            case WeatherIcons.HURRICANE:
                weatherIcon = R.drawable.hurricane;
                break;
            case WeatherIcons.TORNADO:
                weatherIcon = R.drawable.tornado;
                break;
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.na;
        }

        return weatherIcon;
    }

    public static ImageDataViewModel getImageData(Weather weather) {
        String icon = weather.getCondition().getIcon();
        String backgroundCode = null;
        WeatherManager wm = WeatherManager.getInstance();

        // Apply background based on weather condition
        switch (icon) {
            // Rain
            case WeatherIcons.DAY_RAIN:
            case WeatherIcons.DAY_RAIN_MIX:
            case WeatherIcons.DAY_RAIN_WIND:
            case WeatherIcons.DAY_SHOWERS:
            case WeatherIcons.DAY_SLEET:
            case WeatherIcons.DAY_SPRINKLE:
                backgroundCode = WeatherBackground.RAIN;
                break;

            case WeatherIcons.NIGHT_ALT_HAIL:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.RAIN:
            case WeatherIcons.RAIN_MIX:
            case WeatherIcons.RAIN_WIND:
            case WeatherIcons.SHOWERS:
            case WeatherIcons.SLEET:
            case WeatherIcons.SPRINKLE:
                backgroundCode = WeatherBackground.RAIN_NIGHT;
                break;
            // Tornado / Hurricane / Thunderstorm / Tropical Storm
            case WeatherIcons.DAY_LIGHTNING:
            case WeatherIcons.DAY_THUNDERSTORM:
                backgroundCode = WeatherBackground.TSTORMS_DAY;
                break;

            case WeatherIcons.NIGHT_ALT_LIGHTNING:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.LIGHTNING:
            case WeatherIcons.THUNDERSTORM:
                backgroundCode = WeatherBackground.TSTORMS_NIGHT;
                break;

            case WeatherIcons.DAY_STORM_SHOWERS:
            case WeatherIcons.DAY_SLEET_STORM:
            case WeatherIcons.STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
            case WeatherIcons.HAIL:
            case WeatherIcons.HURRICANE:
            case WeatherIcons.TORNADO:
                backgroundCode = WeatherBackground.STORMS;
                break;
            // Dust
            case WeatherIcons.DUST:
            case WeatherIcons.SANDSTORM:
                backgroundCode = WeatherBackground.DUST;
                break;
            // Foggy / Haze
            case WeatherIcons.DAY_FOG:
            case WeatherIcons.DAY_HAZE:
            case WeatherIcons.FOG:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.SMOG:
            case WeatherIcons.SMOKE:
                backgroundCode = WeatherBackground.FOG;
                break;
            // Snow / Snow Showers/Storm
            case WeatherIcons.DAY_SNOW:
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
            case WeatherIcons.SNOW:
                backgroundCode = WeatherBackground.SNOW;
                break;

            case WeatherIcons.SNOW_WIND:
            case WeatherIcons.DAY_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
                backgroundCode = WeatherBackground.SNOW_WINDY;
                break;
            /* Ambigious weather conditions */
            // (Mostly) Cloudy
            case WeatherIcons.CLOUD:
            case WeatherIcons.CLOUDY:
            case WeatherIcons.CLOUDY_GUSTS:
            case WeatherIcons.CLOUDY_WINDY:
            case WeatherIcons.DAY_CLOUDY:
            case WeatherIcons.DAY_CLOUDY_GUSTS:
            case WeatherIcons.DAY_CLOUDY_HIGH:
            case WeatherIcons.DAY_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                if (wm.isNight(weather))
                    backgroundCode = WeatherBackground.MOSTLYCLOUDY_NIGHT;
                else
                    backgroundCode = WeatherBackground.MOSTLYCLOUDY_DAY;
                break;
            // Partly Cloudy
            case WeatherIcons.DAY_SUNNY_OVERCAST:
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                if (wm.isNight(weather))
                    backgroundCode = WeatherBackground.PARTLYCLOUDY_NIGHT;
                else
                    backgroundCode = WeatherBackground.PARTLYCLOUDY_DAY;
                break;

            case WeatherIcons.DAY_SUNNY:
            case WeatherIcons.NA:
            case WeatherIcons.NIGHT_CLEAR:
            case WeatherIcons.SNOWFLAKE_COLD:
            case WeatherIcons.DAY_HOT:
            case WeatherIcons.WINDY:
            case WeatherIcons.STRONG_WIND:
            default:
                // Set background based using sunset/rise times
                if (wm.isNight(weather))
                    backgroundCode = WeatherBackground.NIGHT;
                else
                    backgroundCode = WeatherBackground.DAY;
                break;
        }

        // Check cache for image data
        ImageDataHelperImpl imageHelper = ImageDataHelper.getImageDataHelper();
        ImageData imageData = imageHelper.getCachedImageData(backgroundCode);
        // Check if cache is available and valid
        if (imageData != null && imageData.isValid()) {
            return new ImageDataViewModel(imageData);
        } else {
            imageData = imageHelper.getRemoteImageData(backgroundCode);
            if (imageData != null && imageData.isValid()) {
                return new ImageDataViewModel(imageData);
            } else {
                imageData = imageHelper.getDefaultImageData(backgroundCode, weather);
                if (imageData != null && imageData.isValid())
                    return new ImageDataViewModel(imageData);
            }
        }

        return null;
    }

    public static class Coordinate {
        private double lat = 0;
        private double _long = 0;

        public Coordinate(String coordinatePair) {
            setCoordinate(coordinatePair);
        }

        public Coordinate(double latitude, double longitude) {
            setCoordinate(latitude, longitude);
        }

        public Coordinate(Location location) {
            lat = location.getLatitude();
            _long = location.getLongitude();
        }

        public Coordinate(LocationData location) {
            lat = location.getLatitude();
            _long = location.getLongitude();
        }

        public void setCoordinate(String coordinatePair) {
            String[] coord = coordinatePair.split(",");
            lat = Double.parseDouble(coord[0]);
            _long = Double.parseDouble(coord[1]);
        }

        public void setCoordinate(double latitude, double longitude) {
            lat = latitude;
            _long = longitude;
        }

        public double getLatitude() {
            return lat;
        }

        public double getLongitude() {
            return _long;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s,%s", Double.toString(lat), Double.toString(_long));
        }
    }

    public enum ErrorStatus {
        UNKNOWN(-1),
        SUCCESS(0),
        NOWEATHER(1),
        NETWORKERROR(2),
        INVALIDAPIKEY(3),
        QUERYNOTFOUND(4);

        private final int value;

        public int getValue() {
            return value;
        }

        private ErrorStatus(int value) {
            this.value = value;
        }

        private static SparseArray<ErrorStatus> map = new SparseArray<>();

        static {
            for (ErrorStatus errorStatus : values()) {
                map.put(errorStatus.value, errorStatus);
            }
        }

        public static ErrorStatus valueOf(int value) {
            return map.get(value);
        }
    }
}
