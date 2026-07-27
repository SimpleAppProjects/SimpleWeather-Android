package com.thewizrd.shared_resources.icons

import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R

class GoogleWeatherIconsProvider : WeatherIconProvider() {
    override fun getKey(): String {
        return "goog-weather-icons"
    }

    override fun getDisplayName(): String {
        return "Google Weather Icons"
    }

    override fun getAuthorName(): String {
        return "Google"
    }

    override fun getAttributionLink(): String {
        return "https://developers.google.com/maps/documentation/weather/weather-condition-icons"
    }

    override fun isFontIcon(): Boolean {
        return false
    }

    @DrawableRes
    override fun getWeatherIconResource(icon: String): Int {
        var weatherIcon = when (icon) {
            // Day
            /* CLEAR */
            WeatherIcons.DAY_SUNNY,
            WeatherIcons.DAY_HOT,
            WeatherIcons.DAY_LIGHT_WIND -> R.drawable.goog_clear_day

            /* PARTLY_CLOUDY */
            WeatherIcons.DAY_PARTLY_CLOUDY -> R.drawable.goog_partly_cloudy_day

            /* MOSTLY_CLOUDY */
            WeatherIcons.DAY_CLOUDY,
            WeatherIcons.DAY_CLOUDY_HIGH,
                -> R.drawable.goog_mostly_cloudy_day

            // Night
            /* CLEAR */
            WeatherIcons.NIGHT_CLEAR,
            WeatherIcons.NIGHT_HOT,
            WeatherIcons.NIGHT_LIGHT_WIND -> R.drawable.goog_clear_night

            /* PARTLY_CLOUDY */
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> R.drawable.goog_partly_cloudy_night

            /* MOSTLY_CLOUDY */
            WeatherIcons.NIGHT_ALT_CLOUDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> R.drawable.goog_mostly_cloudy_night

            /* CLOUDY */
            WeatherIcons.DAY_FOG,
            WeatherIcons.DAY_HAZE,
            WeatherIcons.DAY_SUNNY_OVERCAST,
            WeatherIcons.NIGHT_FOG,
            WeatherIcons.NIGHT_HAZE,
            WeatherIcons.NIGHT_OVERCAST,
            WeatherIcons.FOG,
            WeatherIcons.HAZE,
            WeatherIcons.OVERCAST,
            WeatherIcons.CLOUDY,
            WeatherIcons.CLOUD -> R.drawable.goog_cloudy

            /* WINDY */
            WeatherIcons.DAY_CLOUDY_GUSTS,
            WeatherIcons.DAY_CLOUDY_WINDY,
            WeatherIcons.DAY_WINDY,
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY,
            WeatherIcons.NIGHT_WINDY,
            WeatherIcons.CLOUDY_GUSTS,
            WeatherIcons.CLOUDY_WINDY,
            WeatherIcons.LIGHT_WIND,
            WeatherIcons.WINDY,
            WeatherIcons.STRONG_WIND -> R.drawable.goog_windy

            /* WIND_AND_RAIN, RAIN_SHOWERS, LIGHT_TO_MODERATE_RAIN, RAIN */
            WeatherIcons.DAY_SHOWERS,
            WeatherIcons.DAY_RAIN,
            WeatherIcons.NIGHT_ALT_SHOWERS,
            WeatherIcons.NIGHT_ALT_RAIN,
            WeatherIcons.SHOWERS,
            WeatherIcons.RAIN -> R.drawable.goog_wind_rain

            /* HEAVY_RAIN_SHOWERS, MODERATE_TO_HEAVY_RAIN, HEAVY_RAIN, RAIN_PERIODICALLY_HEAVY */
            WeatherIcons.DAY_RAIN_WIND,
            WeatherIcons.NIGHT_ALT_RAIN_WIND,
            WeatherIcons.RAIN_WIND,
                -> R.drawable.goog_heavy_rain

            /* LIGHT_RAIN_SHOWERS, CHANCE_OF_SHOWERS, LIGHT_RAIN */
            WeatherIcons.DAY_SPRINKLE,
            WeatherIcons.NIGHT_ALT_SPRINKLE,
            WeatherIcons.SPRINKLE -> R.drawable.goog_light_rain

            /* SCATTERED_SHOWERS */
            // R.drawable.goog_scattered_showers

            /* LIGHT_SNOW_SHOWERS, LIGHT_SNOW */
            //R.drawable.goog_light_snow

            /* CHANCE_OF_SNOW_SHOWERS, LIGHT_TO_MODERATE_SNOW, SNOW, SNOWSTORM */
            WeatherIcons.DAY_SNOW,
            WeatherIcons.DAY_SNOW_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_SNOW,
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
            WeatherIcons.SNOW,
            WeatherIcons.SNOW_THUNDERSTORM -> R.drawable.goog_snow

            /* SCATTERED_SNOW_SHOWERS, SNOW_SHOWERS */
            //R.drawable.goog_snow_showers

            /* HEAVY_SNOW_SHOWERS, MODERATE_TO_HEAVY_SNOW, HEAVY_SNOW, SNOW_PERIODICALLY_HEAVY, HEAVY_SNOW_STORM */
            WeatherIcons.DAY_SNOW_WIND,
            WeatherIcons.NIGHT_ALT_SNOW_WIND,
            WeatherIcons.SNOW_WIND -> R.drawable.goog_heavy_snow

            /* BLOWING_SNOW */
            //R.drawable.goog_blowing_snow

            /* RAIN_AND_SNOW, HAIL_SHOWERS */
            WeatherIcons.DAY_RAIN_MIX,
            WeatherIcons.DAY_SLEET,
            WeatherIcons.DAY_SLEET_STORM,
            WeatherIcons.NIGHT_ALT_RAIN_MIX,
            WeatherIcons.NIGHT_ALT_SLEET,
            WeatherIcons.NIGHT_ALT_SLEET_STORM,
            WeatherIcons.RAIN_MIX,
            WeatherIcons.SLEET,
            WeatherIcons.SLEET_STORM -> R.drawable.goog_rain_mix

            /* HAIL */
            WeatherIcons.DAY_HAIL,
            WeatherIcons.NIGHT_ALT_HAIL,
            WeatherIcons.HAIL -> R.drawable.goog_hail

            /* THUNDERSTORM, THUNDERSHOWER, LIGHT_THUNDERSTORM_RAIN, HEAVY_THUNDERSTORM */
            WeatherIcons.DAY_STORM_SHOWERS,
            WeatherIcons.DAY_THUNDERSTORM,
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
            WeatherIcons.NIGHT_ALT_THUNDERSTORM,
            WeatherIcons.STORM_SHOWERS,
            WeatherIcons.THUNDERSTORM -> R.drawable.goog_tstorm

            /* SCATTERED_THUNDERSTORMS */
            WeatherIcons.DAY_LIGHTNING,
            WeatherIcons.NIGHT_ALT_LIGHTNING,
            WeatherIcons.LIGHTNING -> R.drawable.goog_scattered_tstorms

            // Misc
            WeatherIcons.HOT -> R.drawable.material_thermometer_gain

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

            WeatherIcons.VISIBILITY -> R.drawable.material_visibility
            WeatherIcons.AIR_QUALITY -> R.drawable.material_aq

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