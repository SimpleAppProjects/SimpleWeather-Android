package com.thewizrd.simpleweather.controls.viewmodels

import android.content.Context
import com.thewizrd.shared_resources.R

object GraphModelUtils {
    @JvmStatic
    fun getLabelForGraphType(context: Context, graphType: ForecastType): String {
        return when (graphType) {
            ForecastType.TEMPERATURE -> context.getString(R.string.label_temperature)
            ForecastType.MINUTELY, ForecastType.PRECIPITATION -> context.getString(R.string.label_precipitation)
            ForecastType.WIND -> context.getString(R.string.label_wind)
            ForecastType.RAIN -> context.getString(R.string.label_qpf_rain)
            ForecastType.SNOW -> context.getString(R.string.label_qpf_snow)
            ForecastType.UVINDEX -> context.getString(R.string.label_uv)
            ForecastType.HUMIDITY -> context.getString(R.string.label_humidity)
            ForecastType.AIRQUALITY -> context.getString(R.string.label_airquality)
        }
    }
}
