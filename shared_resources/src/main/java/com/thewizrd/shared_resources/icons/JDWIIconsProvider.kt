package com.thewizrd.shared_resources.icons

import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.utils.StringUtils

class JDWIIconsProvider : WeatherIconProvider() {
    override fun getKey(): String {
        return "mat-icons-jdwi"
    }

    override fun getDisplayName(): String {
        return "Material Icons"
    }

    override fun getAuthorName(): String {
        return "Jelle Dekkers${StringUtils.lineSeparator()}Material Icons"
    }

    override fun getAttributionLink(): String {
        return "https://github.com/MoRan1412/JDWI"
    }

    override fun isFontIcon(): Boolean {
        return false
    }

    @DrawableRes
    override fun getWeatherIconResource(icon: String): Int {
        var weatherIcon = when (icon) {
            // Day
            WeatherIcons.DAY_SUNNY,
            WeatherIcons.DAY_HOT,
            WeatherIcons.DAY_LIGHT_WIND -> R.drawable.jdwi_clear_day

            WeatherIcons.DAY_CLOUDY,
            WeatherIcons.DAY_CLOUDY_GUSTS,
            WeatherIcons.DAY_CLOUDY_WINDY,
            WeatherIcons.DAY_CLOUDY_HIGH -> R.drawable.jdwi_cloudy_day

            WeatherIcons.DAY_FOG,
            WeatherIcons.DAY_HAZE -> R.drawable.jdwi_fog_day

            WeatherIcons.DAY_HAIL,
            WeatherIcons.DAY_RAIN_MIX,
            WeatherIcons.DAY_SLEET,
            WeatherIcons.DAY_SLEET_STORM -> R.drawable.jdwi_sleet_day

            WeatherIcons.DAY_LIGHTNING,
            WeatherIcons.DAY_STORM_SHOWERS,
            WeatherIcons.DAY_THUNDERSTORM -> R.drawable.jdwi_storm_day

            WeatherIcons.DAY_RAIN,
            WeatherIcons.DAY_RAIN_WIND,
            WeatherIcons.DAY_SHOWERS,
            WeatherIcons.DAY_SPRINKLE -> R.drawable.jdwi_rain_day

            WeatherIcons.DAY_SNOW,
            WeatherIcons.DAY_SNOW_THUNDERSTORM,
            WeatherIcons.DAY_SNOW_WIND -> R.drawable.jdwi_snow_day

            WeatherIcons.DAY_PARTLY_CLOUDY,
            WeatherIcons.DAY_SUNNY_OVERCAST -> R.drawable.jdwi_partly_cloudy_day

            WeatherIcons.DAY_WINDY -> R.drawable.jdwi_windy

            // Night
            WeatherIcons.NIGHT_CLEAR,
            WeatherIcons.NIGHT_HOT,
            WeatherIcons.NIGHT_LIGHT_WIND -> R.drawable.jdwi_clear_night

            WeatherIcons.NIGHT_ALT_CLOUDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> R.drawable.jdwi_cloudy_night

            WeatherIcons.NIGHT_ALT_HAIL,
            WeatherIcons.NIGHT_ALT_RAIN_MIX,
            WeatherIcons.NIGHT_ALT_SLEET,
            WeatherIcons.NIGHT_ALT_SLEET_STORM -> R.drawable.jdwi_sleet_night

            WeatherIcons.NIGHT_ALT_LIGHTNING,
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
            WeatherIcons.NIGHT_ALT_THUNDERSTORM -> R.drawable.jdwi_storm_night

            WeatherIcons.NIGHT_ALT_RAIN,
            WeatherIcons.NIGHT_ALT_RAIN_WIND,
            WeatherIcons.NIGHT_ALT_SHOWERS,
            WeatherIcons.NIGHT_ALT_SPRINKLE -> R.drawable.jdwi_rain_night

            WeatherIcons.NIGHT_ALT_SNOW,
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_SNOW_WIND -> R.drawable.jdwi_snow_night

            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY,
            WeatherIcons.NIGHT_OVERCAST -> R.drawable.jdwi_partly_cloudy_night

            WeatherIcons.NIGHT_FOG,
            WeatherIcons.NIGHT_HAZE -> R.drawable.jdwi_fog_night

            WeatherIcons.NIGHT_WINDY -> R.drawable.jdwi_windy

            // Neutral
            WeatherIcons.CLOUD,
            WeatherIcons.CLOUDY,
            WeatherIcons.OVERCAST -> R.drawable.jdwi_cloudy

            WeatherIcons.FOG,
            WeatherIcons.HAZE -> R.drawable.jdwi_fog

            WeatherIcons.HAIL,
            WeatherIcons.RAIN_MIX,
            WeatherIcons.SLEET,
            WeatherIcons.SLEET_STORM -> R.drawable.jdwi_sleet

            WeatherIcons.HOT -> R.drawable.material_thermometer_gain

            WeatherIcons.CLOUDY_GUSTS,
            WeatherIcons.CLOUDY_WINDY,
            WeatherIcons.LIGHT_WIND,
            WeatherIcons.WINDY,
            WeatherIcons.STRONG_WIND -> R.drawable.jdwi_windy

            WeatherIcons.RAIN,
            WeatherIcons.RAIN_WIND,
            WeatherIcons.SHOWERS,
            WeatherIcons.SPRINKLE -> R.drawable.jdwi_rain

            WeatherIcons.SNOW,
            WeatherIcons.SNOW_THUNDERSTORM,
            WeatherIcons.SNOW_WIND -> R.drawable.jdwi_snow

            WeatherIcons.STORM_SHOWERS,
            WeatherIcons.THUNDERSTORM,
            WeatherIcons.LIGHTNING -> R.drawable.jdwi_storm

            WeatherIcons.SMOG -> R.drawable.wi_smog
            WeatherIcons.SMOKE -> R.drawable.wi_smoke
            WeatherIcons.DUST -> R.drawable.wi_dust
            WeatherIcons.SNOWFLAKE_COLD -> R.drawable.material_cold
            WeatherIcons.SANDSTORM -> R.drawable.wi_sandstorm
            WeatherIcons.HURRICANE -> R.drawable.material_storm
            WeatherIcons.TORNADO -> R.drawable.material_tornado
            WeatherIcons.FIRE -> R.drawable.material_fire
            WeatherIcons.FLOOD -> R.drawable.material_flood
            WeatherIcons.VOLCANO -> R.drawable.material_volcano
            WeatherIcons.BAROMETER -> R.drawable.jdwi_pressure
            WeatherIcons.HUMIDITY -> R.drawable.material_humidity_percentage
            WeatherIcons.MOONRISE -> R.drawable.jdwi_moonrise
            WeatherIcons.MOONSET -> R.drawable.jdwi_moonset
            WeatherIcons.RAINDROP,
            WeatherIcons.RAINDROPS -> R.drawable.material_water_drop

            WeatherIcons.SUNRISE -> R.drawable.jdwi_sunrise
            WeatherIcons.SUNSET -> R.drawable.jdwi_sunset
            WeatherIcons.THERMOMETER -> R.drawable.jdwi_temperature
            WeatherIcons.UMBRELLA -> R.drawable.jdwi_precipitation_alt
            WeatherIcons.WIND_DIRECTION -> R.drawable.jdwi_wind_direction
            WeatherIcons.DIRECTION_UP -> R.drawable.material_arrow_upward
            WeatherIcons.DIRECTION_DOWN -> R.drawable.material_arrow_downward

            WeatherIcons.WIND_BEAUFORT_0 -> R.drawable.wi_wind_beaufort_0
            WeatherIcons.WIND_BEAUFORT_1 -> R.drawable.wi_wind_beaufort_1
            WeatherIcons.WIND_BEAUFORT_2 -> R.drawable.wi_wind_beaufort_2
            WeatherIcons.WIND_BEAUFORT_3 -> R.drawable.wi_wind_beaufort_3
            WeatherIcons.WIND_BEAUFORT_4 -> R.drawable.wi_wind_beaufort_4
            WeatherIcons.WIND_BEAUFORT_5 -> R.drawable.wi_wind_beaufort_5
            WeatherIcons.WIND_BEAUFORT_6 -> R.drawable.wi_wind_beaufort_6
            WeatherIcons.WIND_BEAUFORT_7 -> R.drawable.wi_wind_beaufort_7
            WeatherIcons.WIND_BEAUFORT_8 -> R.drawable.wi_wind_beaufort_8
            WeatherIcons.WIND_BEAUFORT_9 -> R.drawable.wi_wind_beaufort_9
            WeatherIcons.WIND_BEAUFORT_10 -> R.drawable.wi_wind_beaufort_10
            WeatherIcons.WIND_BEAUFORT_11 -> R.drawable.wi_wind_beaufort_11
            WeatherIcons.WIND_BEAUFORT_12 -> R.drawable.wi_wind_beaufort_12

            WeatherIcons.MOON_NEW,
            WeatherIcons.MOON_ALT_NEW -> R.drawable.jdwi_new_moon

            WeatherIcons.MOON_WAXING_CRESCENT_3,
            WeatherIcons.MOON_ALT_WAXING_CRESCENT_3 -> R.drawable.jdwi_waxing_crescent

            WeatherIcons.MOON_FIRST_QUARTER,
            WeatherIcons.MOON_ALT_FIRST_QUARTER -> R.drawable.jdwi_first_quarter

            WeatherIcons.MOON_WAXING_GIBBOUS_3,
            WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3 -> R.drawable.jdwi_waxing_gibbous

            WeatherIcons.MOON_FULL,
            WeatherIcons.MOON_ALT_FULL -> R.drawable.jdwi_full_moon

            WeatherIcons.MOON_WANING_GIBBOUS_3,
            WeatherIcons.MOON_ALT_WANING_GIBBOUS_3 -> R.drawable.jdwi_waning_gibbous

            WeatherIcons.MOON_THIRD_QUARTER,
            WeatherIcons.MOON_ALT_THIRD_QUARTER -> R.drawable.jdwi_third_quarter

            WeatherIcons.MOON_WANING_CRESCENT_3,
            WeatherIcons.MOON_ALT_WANING_CRESCENT_3 -> R.drawable.jdwi_waning_crescent

            WeatherIcons.FAHRENHEIT -> R.drawable.wi_fahrenheit
            WeatherIcons.CELSIUS -> R.drawable.wi_celsius

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
            WeatherIcons.UV_INDEX_11 -> R.drawable.jdwi_uv_index

            WeatherIcons.TREE_POLLEN -> R.drawable.ic_outline_tree
            WeatherIcons.GRASS_POLLEN -> R.drawable.ic_baseline_grass
            WeatherIcons.RAGWEED_POLLEN -> R.drawable.ic_ragweed_pollen

            WeatherIcons.NA -> R.drawable.jdwi_unknown

            else -> -1
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.wi_na
        }

        return weatherIcon
    }
}