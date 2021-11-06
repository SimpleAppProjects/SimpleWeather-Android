@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertSeverity
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType

@DrawableRes
fun WeatherAlertType?.getDrawableFromAlertType(): Int {
    return when (this) {
        WeatherAlertType.DENSEFOG -> R.drawable.wi_fog

        WeatherAlertType.FIRE -> R.drawable.wi_fire

        WeatherAlertType.FLOODWARNING,
        WeatherAlertType.FLOODWATCH -> R.drawable.wi_flood

        WeatherAlertType.HEAT -> R.drawable.wi_hot

        WeatherAlertType.HIGHWIND -> R.drawable.wi_strong_wind

        WeatherAlertType.HURRICANELOCALSTATEMENT,
        WeatherAlertType.HURRICANEWINDWARNING -> R.drawable.wi_hurricane

        WeatherAlertType.SEVERETHUNDERSTORMWARNING,
        WeatherAlertType.SEVERETHUNDERSTORMWATCH -> R.drawable.wi_thunderstorm

        WeatherAlertType.TORNADOWARNING, WeatherAlertType.TORNADOWATCH -> R.drawable.wi_tornado

        WeatherAlertType.VOLCANO -> R.drawable.wi_volcano

        WeatherAlertType.WINTERWEATHER -> R.drawable.wi_snowflake_cold

        WeatherAlertType.DENSESMOKE -> R.drawable.wi_smoke

        WeatherAlertType.DUSTADVISORY -> R.drawable.wi_dust

        WeatherAlertType.EARTHQUAKEWARNING -> R.drawable.wi_earthquake

        WeatherAlertType.GALEWARNING -> R.drawable.wi_gale_warning

        WeatherAlertType.SMALLCRAFT -> R.drawable.wi_small_craft_advisory

        WeatherAlertType.STORMWARNING -> R.drawable.wi_storm_warning

        WeatherAlertType.TSUNAMIWARNING, WeatherAlertType.TSUNAMIWATCH -> R.drawable.wi_tsunami

        WeatherAlertType.SEVEREWEATHER,
        WeatherAlertType.SPECIALWEATHERALERT -> R.drawable.ic_error

        else -> R.drawable.ic_error
    }
}

@ColorInt
fun WeatherAlertSeverity?.getColorFromAlertSeverity(): Int {
    return when (this) {
        WeatherAlertSeverity.SEVERE -> Colors.ORANGERED
        WeatherAlertSeverity.EXTREME -> Colors.RED
        WeatherAlertSeverity.MODERATE -> Colors.ORANGE
        else -> Colors.ORANGE
    }
}
