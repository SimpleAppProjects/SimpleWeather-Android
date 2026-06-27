package com.thewizrd.shared_resources.icons

import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R

class WeatherIconicProvider(private val isColored: Boolean = false) : WeatherIconProvider() {
    override fun getKey(): String {
        return "w-iconic-jackd248" + if (isColored) "-multicolor" else ""
    }

    override fun getDisplayName(): String {
        return "Weather Iconic" + if (isColored) " (Multi-Color)" else ""
    }

    override fun getAuthorName(): String {
        return "konradmichalik"
    }

    override fun getAttributionLink(): String {
        return "https://konradmichalik.github.io/weather-iconic/"
    }

    override fun isFontIcon(): Boolean {
        return !isColored
    }

    @DrawableRes
    override fun getWeatherIconResource(icon: String): Int {
        var weatherIcon = when (icon) {
            WeatherIcons.DAY_SUNNY -> if (isColored) R.drawable.wic_color_sun else R.drawable.wic_sun
            WeatherIcons.DAY_CLOUDY -> if (isColored) R.drawable.wic_color_sun_cloud else R.drawable.wic_sun_cloud
            WeatherIcons.DAY_CLOUDY_GUSTS -> if (isColored) R.drawable.wic_color_sun_cloud_wind else R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_CLOUDY_WINDY -> if (isColored) R.drawable.wic_color_sun_cloud_wind else R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_FOG -> if (isColored) R.drawable.wic_color_sun_fog else R.drawable.wic_sun_fog
            WeatherIcons.DAY_HAIL -> if (isColored) R.drawable.wic_color_sun_cloud_hail else R.drawable.wic_sun_cloud_hail
            WeatherIcons.DAY_HAZE -> if (isColored) R.drawable.wic_color_sun_cloud_haze else R.drawable.wic_sun_cloud_haze
            WeatherIcons.DAY_LIGHTNING -> if (isColored) R.drawable.wic_color_sun_cloud_lightning else R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_PARTLY_CLOUDY -> if (isColored) R.drawable.wic_color_sun_cloud else R.drawable.wic_sun_cloud
            WeatherIcons.DAY_RAIN -> if (isColored) R.drawable.wic_color_sun_cloud_rain else R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_RAIN_MIX -> if (isColored) R.drawable.wic_color_sun_cloud_rain else R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_RAIN_WIND -> if (isColored) R.drawable.wic_color_sun_cloud_rain else R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SHOWERS -> if (isColored) R.drawable.wic_color_sun_cloud_rain else R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SLEET -> if (isColored) R.drawable.wic_color_sun_cloud_rain else R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SLEET_STORM -> if (isColored) R.drawable.wic_color_sun_cloud_rain else R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SNOW -> if (isColored) R.drawable.wic_color_sun_cloud_snow_alt else R.drawable.wic_sun_cloud_snow_alt
            WeatherIcons.DAY_SNOW_THUNDERSTORM -> if (isColored) R.drawable.wic_color_sun_cloud_snow else R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SNOW_WIND -> if (isColored) R.drawable.wic_color_sun_cloud_snow else R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SPRINKLE -> if (isColored) R.drawable.wic_color_sun_cloud_rain_alt else R.drawable.wic_sun_cloud_rain_alt
            WeatherIcons.DAY_STORM_SHOWERS -> if (isColored) R.drawable.wic_color_sun_cloud_lightning else R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_SUNNY_OVERCAST -> if (isColored) R.drawable.wic_color_sun_cloud else R.drawable.wic_sun_cloud
            WeatherIcons.DAY_THUNDERSTORM -> if (isColored) R.drawable.wic_color_sun_cloud_lightning else R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_WINDY -> if (isColored) R.drawable.wic_color_wind else R.drawable.wic_wind
            WeatherIcons.DAY_HOT -> if (isColored) R.drawable.wic_color_sun else R.drawable.wic_sun
            WeatherIcons.DAY_CLOUDY_HIGH -> if (isColored) R.drawable.wic_color_sun_cloud else R.drawable.wic_sun_cloud
            WeatherIcons.DAY_LIGHT_WIND -> if (isColored) R.drawable.wic_color_wind else R.drawable.wic_wind

            WeatherIcons.NIGHT_CLEAR -> if (isColored) R.drawable.wic_color_moon else R.drawable.wic_moon
            WeatherIcons.NIGHT_ALT_CLOUDY -> if (isColored) R.drawable.wic_color_moon_cloud else R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS -> if (isColored) R.drawable.wic_color_moon_cloud_wind else R.drawable.wic_moon_cloud_wind
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> if (isColored) R.drawable.wic_color_moon_cloud_wind else R.drawable.wic_moon_cloud_wind
            WeatherIcons.NIGHT_ALT_HAIL -> if (isColored) R.drawable.wic_color_moon_cloud_hail else R.drawable.wic_moon_cloud_hail
            WeatherIcons.NIGHT_ALT_LIGHTNING -> if (isColored) R.drawable.wic_color_moon_cloud_lightning else R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_RAIN -> if (isColored) R.drawable.wic_color_moon_cloud_rain else R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_RAIN_MIX -> if (isColored) R.drawable.wic_color_moon_cloud_rain else R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_RAIN_WIND -> if (isColored) R.drawable.wic_color_moon_cloud_rain else R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SHOWERS -> if (isColored) R.drawable.wic_color_moon_cloud_rain else R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SLEET -> if (isColored) R.drawable.wic_color_moon_cloud_sleet else R.drawable.wic_moon_cloud_sleet
            WeatherIcons.NIGHT_ALT_SLEET_STORM -> if (isColored) R.drawable.wic_color_moon_cloud_sleet else R.drawable.wic_moon_cloud_sleet
            WeatherIcons.NIGHT_ALT_SNOW -> if (isColored) R.drawable.wic_color_moon_cloud_snow_alt else R.drawable.wic_moon_cloud_snow_alt
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM -> if (isColored) R.drawable.wic_color_moon_cloud_snow else R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SNOW_WIND -> if (isColored) R.drawable.wic_color_moon_cloud_snow else R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SPRINKLE -> if (isColored) R.drawable.wic_color_moon_cloud_rain_alt else R.drawable.wic_moon_cloud_rain_alt
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS -> if (isColored) R.drawable.wic_color_moon_cloud_lightning else R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_THUNDERSTORM -> if (isColored) R.drawable.wic_color_moon_cloud_lightning else R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> if (isColored) R.drawable.wic_color_moon_cloud else R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> if (isColored) R.drawable.wic_color_moon_cloud else R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_FOG -> if (isColored) R.drawable.wic_color_moon_fog else R.drawable.wic_moon_fog
            WeatherIcons.NIGHT_OVERCAST -> if (isColored) R.drawable.wic_color_moon_cloud else R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_HAZE -> if (isColored) R.drawable.wic_color_moon_cloud_haze else R.drawable.wic_moon_cloud_haze
            WeatherIcons.NIGHT_WINDY -> if (isColored) R.drawable.wic_color_wind else R.drawable.wic_wind
            WeatherIcons.NIGHT_HOT -> if (isColored) R.drawable.wic_color_moon else R.drawable.wic_moon
            WeatherIcons.NIGHT_LIGHT_WIND -> if (isColored) R.drawable.wic_color_wind else R.drawable.wic_wind

            WeatherIcons.CLOUD -> if (isColored) R.drawable.wic_color_cloud else R.drawable.wic_cloud
            WeatherIcons.CLOUDY -> if (isColored) R.drawable.wic_color_clouds else R.drawable.wic_clouds
            WeatherIcons.CLOUDY_GUSTS -> if (isColored) R.drawable.wic_color_cloud_wind else R.drawable.wic_cloud_wind
            WeatherIcons.CLOUDY_WINDY -> if (isColored) R.drawable.wic_color_cloud_wind else R.drawable.wic_cloud_wind
            WeatherIcons.FOG -> if (isColored) R.drawable.wic_color_fog else R.drawable.wic_fog
            WeatherIcons.HAIL -> if (isColored) R.drawable.wic_color_cloud_hail else R.drawable.wic_cloud_hail
            WeatherIcons.HAZE -> if (isColored) R.drawable.wic_color_cloud_haze else R.drawable.wic_cloud_haze
            WeatherIcons.HOT -> if (isColored) R.drawable.wic_color_thermometer_heat else R.drawable.wic_thermometer_heat
            WeatherIcons.LIGHT_WIND -> if (isColored) R.drawable.wic_color_wind else R.drawable.wic_wind
            WeatherIcons.RAIN -> if (isColored) R.drawable.wic_color_cloud_rain else R.drawable.wic_cloud_rain
            WeatherIcons.RAIN_MIX -> if (isColored) R.drawable.wic_color_cloud_sleet else R.drawable.wic_cloud_sleet
            WeatherIcons.RAIN_WIND -> if (isColored) R.drawable.wic_color_cloud_rain else R.drawable.wic_cloud_rain
            WeatherIcons.OVERCAST -> if (isColored) R.drawable.wic_color_clouds_alt else R.drawable.wic_clouds_alt
            WeatherIcons.SHOWERS -> if (isColored) R.drawable.wic_color_cloud_rain else R.drawable.wic_cloud_rain
            WeatherIcons.SLEET -> if (isColored) R.drawable.wic_color_cloud_sleet else R.drawable.wic_cloud_sleet
            WeatherIcons.SLEET_STORM -> if (isColored) R.drawable.wic_color_cloud_sleet else R.drawable.wic_cloud_sleet
            WeatherIcons.SNOW -> if (isColored) R.drawable.wic_color_cloud_snow_alt else R.drawable.wic_cloud_snow_alt
            WeatherIcons.SNOW_THUNDERSTORM -> if (isColored) R.drawable.wic_color_cloud_snow else R.drawable.wic_cloud_snow
            WeatherIcons.SPRINKLE -> if (isColored) R.drawable.wic_color_cloud_rain_alt else R.drawable.wic_cloud_rain_alt
            WeatherIcons.STORM_SHOWERS -> if (isColored) R.drawable.wic_color_lightning else R.drawable.wic_lightning
            WeatherIcons.THUNDERSTORM -> if (isColored) R.drawable.wic_color_lightning else R.drawable.wic_lightning
            WeatherIcons.SNOW_WIND -> if (isColored) R.drawable.wic_color_cloud_snow else R.drawable.wic_cloud_snow
            WeatherIcons.SMOG -> if (isColored) R.drawable.wic_color_cloud_smog else R.drawable.wic_cloud_smog
            WeatherIcons.SMOKE -> R.drawable.wi_smoke
            WeatherIcons.LIGHTNING -> if (isColored) R.drawable.wic_color_lightning else R.drawable.wic_lightning
            WeatherIcons.DUST -> R.drawable.wi_dust
            WeatherIcons.SNOWFLAKE_COLD -> if (isColored) R.drawable.wic_color_snowflake else R.drawable.wic_snowflake
            WeatherIcons.WINDY -> if (isColored) R.drawable.wic_color_wind else R.drawable.wic_wind
            WeatherIcons.STRONG_WIND -> if (isColored) R.drawable.wic_color_wind_high else R.drawable.wic_wind_high
            WeatherIcons.SANDSTORM -> if (isColored) R.drawable.wic_color_sandstorm else R.drawable.wic_sandstorm
            WeatherIcons.HURRICANE -> if (isColored) R.drawable.wic_color_hurricane else R.drawable.wic_hurricane
            WeatherIcons.TORNADO -> if (isColored) R.drawable.wic_color_tornado else R.drawable.wic_tornado
            WeatherIcons.FIRE -> if (isColored) R.drawable.wic_color_fire else R.drawable.wic_fire
            WeatherIcons.FLOOD -> if (isColored) R.drawable.wic_color_flood else R.drawable.wic_flood
            WeatherIcons.VOLCANO -> R.drawable.wi_volcano
            WeatherIcons.BAROMETER -> if (isColored) R.drawable.wic_color_barometer else R.drawable.wic_barometer
            WeatherIcons.HUMIDITY -> if (isColored) R.drawable.wic_color_humidity else R.drawable.wic_humidity
            WeatherIcons.MOONRISE -> if (isColored) R.drawable.wic_color_moonrise else R.drawable.wic_moonrise
            WeatherIcons.MOONSET -> if (isColored) R.drawable.wic_color_moonset else R.drawable.wic_moonset
            WeatherIcons.RAINDROP -> if (isColored) R.drawable.wic_color_raindrop else R.drawable.wic_raindrop
            WeatherIcons.RAINDROPS -> R.drawable.wi_raindrops
            WeatherIcons.SUNRISE -> if (isColored) R.drawable.wic_color_sunrise else R.drawable.wic_sunrise
            WeatherIcons.SUNSET -> if (isColored) R.drawable.wic_color_sunset else R.drawable.wic_sunset
            WeatherIcons.THERMOMETER -> if (isColored) R.drawable.wic_color_thermometer_medium else R.drawable.wic_thermometer_medium
            WeatherIcons.UMBRELLA -> if (isColored) R.drawable.wic_color_umbrella else R.drawable.wic_umbrella
            WeatherIcons.WIND_DIRECTION -> if (isColored) R.drawable.wic_color_compass else R.drawable.wic_compass
            WeatherIcons.DIRECTION_UP -> R.drawable.wi_direction_up
            WeatherIcons.DIRECTION_DOWN -> R.drawable.wi_direction_down

            WeatherIcons.VISIBILITY -> R.drawable.material_visibility
            WeatherIcons.AIR_QUALITY -> if (isColored) R.drawable.wic_color_fog else R.drawable.wic_fog

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

            WeatherIcons.MOON_NEW -> if (isColored) R.drawable.wic_color_moon_fullmoon else R.drawable.wic_moon_fullmoon
            WeatherIcons.MOON_WAXING_CRESCENT_3 -> if (isColored) R.drawable.wic_color_moon_waxing_crescent else R.drawable.wic_moon_waxing_crescent
            WeatherIcons.MOON_FIRST_QUARTER -> if (isColored) R.drawable.wic_color_moon_first_quarter else R.drawable.wic_moon_first_quarter
            WeatherIcons.MOON_WAXING_GIBBOUS_3 -> if (isColored) R.drawable.wic_color_moon_waxing_gibbous else R.drawable.wic_moon_waxing_gibbous
            WeatherIcons.MOON_FULL -> if (isColored) R.drawable.wic_color_moon_newmoon else R.drawable.wic_moon_newmoon
            WeatherIcons.MOON_WANING_GIBBOUS_3 -> if (isColored) R.drawable.wic_color_moon_waning_gibbous else R.drawable.wic_moon_waning_gibbous
            WeatherIcons.MOON_THIRD_QUARTER -> if (isColored) R.drawable.wic_color_moon_last_quarter else R.drawable.wic_moon_last_quarter
            WeatherIcons.MOON_WANING_CRESCENT_3 -> if (isColored) R.drawable.wic_color_moon_waning_crescent else R.drawable.wic_moon_waning_crescent

            WeatherIcons.MOON_ALT_NEW -> if (isColored) R.drawable.wic_color_moon_fullmoon else R.drawable.wic_moon_fullmoon
            WeatherIcons.MOON_ALT_WAXING_CRESCENT_3 -> if (isColored) R.drawable.wic_color_moon_waxing_crescent else R.drawable.wic_moon_waxing_crescent
            WeatherIcons.MOON_ALT_FIRST_QUARTER -> if (isColored) R.drawable.wic_color_moon_first_quarter else R.drawable.wic_moon_first_quarter
            WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3 -> if (isColored) R.drawable.wic_color_moon_waxing_gibbous else R.drawable.wic_moon_waxing_gibbous
            WeatherIcons.MOON_ALT_FULL -> if (isColored) R.drawable.wic_color_moon_newmoon else R.drawable.wic_moon_newmoon
            WeatherIcons.MOON_ALT_WANING_GIBBOUS_3 -> if (isColored) R.drawable.wic_color_moon_waning_gibbous else R.drawable.wic_moon_waning_gibbous
            WeatherIcons.MOON_ALT_THIRD_QUARTER -> if (isColored) R.drawable.wic_color_moon_last_quarter else R.drawable.wic_moon_last_quarter
            WeatherIcons.MOON_ALT_WANING_CRESCENT_3 -> if (isColored) R.drawable.wic_color_moon_waning_crescent else R.drawable.wic_moon_waning_crescent

            WeatherIcons.FAHRENHEIT -> if (isColored) R.drawable.wic_color_fahrenheit else R.drawable.wic_fahrenheit
            WeatherIcons.CELSIUS -> if (isColored) R.drawable.wic_color_celsius else R.drawable.wic_celsius

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
            WeatherIcons.UV_INDEX_11 -> if (isColored) R.drawable.wic_color_sun else R.drawable.wic_sun

            WeatherIcons.TREE_POLLEN -> R.drawable.ic_outline_tree
            WeatherIcons.GRASS_POLLEN -> R.drawable.ic_baseline_grass
            WeatherIcons.RAGWEED_POLLEN -> if (isColored) R.drawable.wic_color_pollen else R.drawable.wic_pollen

            WeatherIcons.NA -> if (isColored) R.drawable.wic_color_na else R.drawable.wic_na

            else -> 0
        }

        if (weatherIcon == 0) {
            // Not Available
            weatherIcon = if (isColored) R.drawable.wic_color_na else R.drawable.wic_na
        }

        return weatherIcon
    }
}