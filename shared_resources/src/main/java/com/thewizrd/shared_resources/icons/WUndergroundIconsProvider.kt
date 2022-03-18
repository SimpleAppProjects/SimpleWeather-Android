package com.thewizrd.shared_resources.icons

import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R

class WUndergroundIconsProvider : WeatherIconProvider() {
    override fun getKey(): String {
        return "wui-ashley-jager"
    }

    override fun getDisplayName(): String {
        return "WeatherUnderground Icons"
    }

    override fun getAuthorName(): String {
        return "Ashley Jager"
    }

    override fun getAttributionLink(): String {
        return "https://github.com/manifestinteractive/weather-underground-icons"
    }

    override fun isFontIcon(): Boolean {
        return false
    }

    @DrawableRes
    override fun getWeatherIconResource(icon: String): Int {
        var weatherIcon = -1

        when (icon) {
            WeatherIcons.DAY_SUNNY,
            WeatherIcons.DAY_HOT,
            WeatherIcons.DAY_LIGHT_WIND -> weatherIcon =
                R.drawable.wui_day_sunny

            WeatherIcons.DAY_CLOUDY,
            WeatherIcons.DAY_CLOUDY_GUSTS,
            WeatherIcons.DAY_CLOUDY_WINDY,
            WeatherIcons.DAY_CLOUDY_HIGH -> weatherIcon =
                R.drawable.wui_day_mostlycloudy

            WeatherIcons.DAY_PARTLY_CLOUDY,
            WeatherIcons.DAY_SUNNY_OVERCAST -> weatherIcon =
                R.drawable.wui_day_partlycloudy

            // Night
            WeatherIcons.NIGHT_CLEAR -> weatherIcon =
                R.drawable.wui_nt_clear

            WeatherIcons.NIGHT_ALT_CLOUDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> weatherIcon =
                R.drawable.wui_nt_mostlycloudy

            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY,
            WeatherIcons.NIGHT_OVERCAST -> weatherIcon =
                R.drawable.wui_nt_partlycloudy
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> weatherIcon =
                R.drawable.wui_nt_mostlycloudy

            // Neutral
            WeatherIcons.DAY_FOG,
            WeatherIcons.DAY_HAZE,
            WeatherIcons.NIGHT_FOG,
            WeatherIcons.FOG -> weatherIcon = R.drawable.wui_fog

            WeatherIcons.DAY_LIGHTNING,
            WeatherIcons.NIGHT_ALT_LIGHTNING,
            WeatherIcons.LIGHTNING -> weatherIcon =
                R.drawable.wui_chancetstorms

            WeatherIcons.DAY_HAIL,
            WeatherIcons.DAY_SLEET,
            WeatherIcons.DAY_SLEET_STORM,
            WeatherIcons.NIGHT_ALT_HAIL,
            WeatherIcons.NIGHT_ALT_SLEET,
            WeatherIcons.NIGHT_ALT_SLEET_STORM,
            WeatherIcons.SLEET -> weatherIcon = R.drawable.wui_sleet

            WeatherIcons.DAY_RAIN_MIX,
            WeatherIcons.NIGHT_ALT_RAIN_MIX,
            WeatherIcons.RAIN_MIX,
            WeatherIcons.HAIL -> weatherIcon = R.drawable.wui_chancesleet

            WeatherIcons.DAY_RAIN,
            WeatherIcons.DAY_RAIN_WIND,
            WeatherIcons.DAY_SHOWERS,
            WeatherIcons.NIGHT_ALT_RAIN,
            WeatherIcons.NIGHT_ALT_RAIN_WIND,
            WeatherIcons.NIGHT_ALT_SHOWERS,
            WeatherIcons.RAIN,
            WeatherIcons.RAIN_WIND,
            WeatherIcons.SHOWERS -> weatherIcon = R.drawable.wui_rain

            WeatherIcons.DAY_SNOW,
            WeatherIcons.DAY_SNOW_THUNDERSTORM,
            WeatherIcons.DAY_SNOW_WIND,
            WeatherIcons.NIGHT_ALT_SNOW,
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_SNOW_WIND,
            WeatherIcons.SNOW,
            WeatherIcons.SNOW_WIND,
            WeatherIcons.SNOWFLAKE_COLD -> weatherIcon =
                R.drawable.wui_snow

            WeatherIcons.DAY_SPRINKLE,
            WeatherIcons.NIGHT_ALT_SPRINKLE,
            WeatherIcons.SPRINKLE -> weatherIcon =
                R.drawable.wui_chancerain

            WeatherIcons.DAY_STORM_SHOWERS,
            WeatherIcons.DAY_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
            WeatherIcons.NIGHT_ALT_THUNDERSTORM,
            WeatherIcons.STORM_SHOWERS,
            WeatherIcons.THUNDERSTORM -> weatherIcon =
                R.drawable.wui_tstorms

            WeatherIcons.DAY_WINDY -> weatherIcon = R.drawable.wi_windy

            WeatherIcons.CLOUD,
            WeatherIcons.CLOUDY,
            WeatherIcons.CLOUDY_GUSTS,
            WeatherIcons.CLOUDY_WINDY -> weatherIcon =
                R.drawable.wui_cloudy

            WeatherIcons.SMOG -> weatherIcon = R.drawable.wi_smog
            WeatherIcons.SMOKE -> weatherIcon = R.drawable.wi_smoke
            WeatherIcons.DUST -> weatherIcon = R.drawable.wi_dust
            WeatherIcons.WINDY -> weatherIcon = R.drawable.wi_windy
            WeatherIcons.STRONG_WIND -> weatherIcon = R.drawable.wi_strong_wind
            WeatherIcons.SANDSTORM -> weatherIcon = R.drawable.wi_sandstorm
            WeatherIcons.HURRICANE -> weatherIcon = R.drawable.wi_hurricane
            WeatherIcons.TORNADO -> weatherIcon = R.drawable.wi_tornado

            // Misc icons
            WeatherIcons.FIRE -> weatherIcon = R.drawable.wi_fire
            WeatherIcons.FLOOD -> weatherIcon = R.drawable.wi_flood
            WeatherIcons.VOLCANO -> weatherIcon = R.drawable.wi_volcano
            WeatherIcons.BAROMETER -> weatherIcon = R.drawable.wi_barometer
            WeatherIcons.HUMIDITY -> weatherIcon = R.drawable.wi_humidity
            WeatherIcons.MOONRISE -> weatherIcon = R.drawable.wi_moonrise
            WeatherIcons.MOONSET -> weatherIcon = R.drawable.wi_moonset
            WeatherIcons.RAINDROP -> weatherIcon = R.drawable.wi_raindrop
            WeatherIcons.RAINDROPS -> weatherIcon = R.drawable.wi_raindrops
            WeatherIcons.SUNRISE -> weatherIcon = R.drawable.wi_sunrise
            WeatherIcons.SUNSET -> weatherIcon = R.drawable.wi_sunset
            WeatherIcons.THERMOMETER -> weatherIcon = R.drawable.wi_thermometer
            WeatherIcons.UMBRELLA -> weatherIcon = R.drawable.wi_umbrella
            WeatherIcons.WIND_DIRECTION -> weatherIcon = R.drawable.wi_wind_direction
            WeatherIcons.DIRECTION_UP -> weatherIcon = R.drawable.wi_direction_up
            WeatherIcons.DIRECTION_DOWN -> weatherIcon = R.drawable.wi_direction_down

            // Beaufort
            WeatherIcons.WIND_BEAUFORT_0 -> weatherIcon = R.drawable.wi_wind_beaufort_0
            WeatherIcons.WIND_BEAUFORT_1 -> weatherIcon = R.drawable.wi_wind_beaufort_1
            WeatherIcons.WIND_BEAUFORT_2 -> weatherIcon = R.drawable.wi_wind_beaufort_2
            WeatherIcons.WIND_BEAUFORT_3 -> weatherIcon = R.drawable.wi_wind_beaufort_3
            WeatherIcons.WIND_BEAUFORT_4 -> weatherIcon = R.drawable.wi_wind_beaufort_4
            WeatherIcons.WIND_BEAUFORT_5 -> weatherIcon = R.drawable.wi_wind_beaufort_5
            WeatherIcons.WIND_BEAUFORT_6 -> weatherIcon = R.drawable.wi_wind_beaufort_6
            WeatherIcons.WIND_BEAUFORT_7 -> weatherIcon = R.drawable.wi_wind_beaufort_7
            WeatherIcons.WIND_BEAUFORT_8 -> weatherIcon = R.drawable.wi_wind_beaufort_8
            WeatherIcons.WIND_BEAUFORT_9 -> weatherIcon = R.drawable.wi_wind_beaufort_9
            WeatherIcons.WIND_BEAUFORT_10 -> weatherIcon = R.drawable.wi_wind_beaufort_10
            WeatherIcons.WIND_BEAUFORT_11 -> weatherIcon = R.drawable.wi_wind_beaufort_11
            WeatherIcons.WIND_BEAUFORT_12 -> weatherIcon = R.drawable.wi_wind_beaufort_12

            // Moon phase
            WeatherIcons.MOON_NEW -> weatherIcon = R.drawable.wi_moon_new
            WeatherIcons.MOON_WAXING_CRESCENT_3 -> weatherIcon =
                R.drawable.wi_moon_waxing_crescent_3
            WeatherIcons.MOON_FIRST_QUARTER -> weatherIcon = R.drawable.wi_moon_first_quarter
            WeatherIcons.MOON_WAXING_GIBBOUS_3 -> weatherIcon = R.drawable.wi_moon_waxing_gibbous_3
            WeatherIcons.MOON_FULL -> weatherIcon = R.drawable.wi_moon_full
            WeatherIcons.MOON_WANING_GIBBOUS_3 -> weatherIcon = R.drawable.wi_moon_waning_gibbous_3
            WeatherIcons.MOON_THIRD_QUARTER -> weatherIcon = R.drawable.wi_moon_third_quarter
            WeatherIcons.MOON_WANING_CRESCENT_3 -> weatherIcon =
                R.drawable.wi_moon_waning_crescent_3
            WeatherIcons.MOON_ALT_NEW -> weatherIcon = R.drawable.wi_moon_alt_new
            WeatherIcons.MOON_ALT_WAXING_CRESCENT_3 -> weatherIcon =
                R.drawable.wi_moon_alt_waxing_crescent_3
            WeatherIcons.MOON_ALT_FIRST_QUARTER -> weatherIcon =
                R.drawable.wi_moon_alt_first_quarter
            WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3 -> weatherIcon =
                R.drawable.wi_moon_alt_waxing_gibbous_3
            WeatherIcons.MOON_ALT_FULL -> weatherIcon = R.drawable.wi_moon_alt_full
            WeatherIcons.MOON_ALT_WANING_GIBBOUS_3 -> weatherIcon =
                R.drawable.wi_moon_alt_waning_gibbous_3
            WeatherIcons.MOON_ALT_THIRD_QUARTER -> weatherIcon =
                R.drawable.wi_moon_alt_third_quarter
            WeatherIcons.MOON_ALT_WANING_CRESCENT_3 -> weatherIcon =
                R.drawable.wi_moon_alt_waning_crescent_3

            WeatherIcons.UV_INDEX,
            WeatherIcons.UV_INDEX_1,
            WeatherIcons.UV_INDEX_2,
            WeatherIcons.UV_INDEX_3,
            WeatherIcons.UV_INDEX_4,
            WeatherIcons.UV_INDEX_5,
            WeatherIcons.UV_INDEX_6,
            WeatherIcons.UV_INDEX_7,
            WeatherIcons.UV_INDEX_8,
            WeatherIcons.UV_INDEX_9,
            WeatherIcons.UV_INDEX_10,
            WeatherIcons.UV_INDEX_11 -> weatherIcon = R.drawable.wui_day_sunny

            WeatherIcons.TREE_POLLEN -> weatherIcon = R.drawable.ic_outline_tree
            WeatherIcons.GRASS_POLLEN -> weatherIcon = R.drawable.ic_baseline_grass
            WeatherIcons.RAGWEED_POLLEN -> weatherIcon = R.drawable.ic_ragweed_pollen

            WeatherIcons.FAHRENHEIT -> weatherIcon = R.drawable.wi_fahrenheit
            WeatherIcons.CELSIUS -> weatherIcon = R.drawable.wi_celsius

            WeatherIcons.NA -> weatherIcon = R.drawable.wui_unknown
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.wui_unknown
        }

        return weatherIcon
    }
}