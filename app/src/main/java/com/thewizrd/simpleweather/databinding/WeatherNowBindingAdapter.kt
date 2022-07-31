package com.thewizrd.simpleweather.databinding

import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.widget.GridView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.ObjectsCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.SunPhaseViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.DetailsItemAdapter
import com.thewizrd.simpleweather.adapters.DetailsItemGridAdapter
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.controls.SunPhaseView
import com.thewizrd.simpleweather.controls.viewmodels.HourlyForecastNowViewModel

object WeatherNowBindingAdapter {
    @JvmStatic
    @BindingAdapter("details_data")
    fun updateDetailsContainer(
        view: GridView,
        map: Map<WeatherDetailsType, DetailItemViewModel>?
    ) {
        if (view.adapter is DetailsItemGridAdapter) {
            (view.adapter as DetailsItemGridAdapter).updateItems(map?.values)
        }
    }

    @JvmStatic
    @BindingAdapter("details_data")
    fun updateDetailsContainer(view: RecyclerView, models: List<DetailItemViewModel>?) {
        if (view.adapter is DetailsItemAdapter) {
            (view.adapter as DetailsItemAdapter).submitList(models)
        }
    }

    @JvmStatic
    @BindingAdapter("forecastData")
    fun updateHrForecastView(view: RecyclerView, forecasts: List<HourlyForecastNowViewModel>?) {
        if (view.adapter is HourlyForecastItemAdapter) {
            (view.adapter as HourlyForecastItemAdapter).submitList(forecasts)
        }
    }

    @JvmStatic
    @BindingAdapter("sunPhase")
    fun updateSunPhasePanel(view: SunPhaseView, sunPhase: SunPhaseViewModel?) {
        if (sunPhase != null) {
            view.setSunriseSetTimes(
                sunPhase.sunriseTime, sunPhase.sunsetTime,
                sunPhase.tzOffset
            )
        }
    }

    @JvmStatic
    @BindingAdapter("imageData")
    fun getBackgroundAttribution(view: TextView, imageData: ImageDataViewModel?) {
        if (imageData?.originalLink?.isNotBlank() == true) {
            val text = SpannableString(
                String.format(
                    "%s %s (%s)",
                    view.context.getString(R.string.attrib_prefix),
                    imageData.artistName,
                    imageData.siteName
                )
            )
            text.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            view.text = text
            view.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(imageData.originalLink))
                if (i.resolveActivity(view.context.packageManager) != null) {
                    runCatching {
                        view.context.startActivity(i)
                    }.onFailure {
                        // NOTE: possible exceptions: SecurityException, ActivityNotFoundException
                        Logger.writeLine(Log.ERROR, it, "Error opening attribution link")
                    }
                }
            }
        } else {
            view.text = ""
            view.setOnClickListener(null)
        }
    }

    @JvmStatic
    @BindingAdapter(value = ["tempTextColor", "tempUnit"], requireAll = false)
    fun tempTextColor(
        view: TextView,
        temp: CharSequence?,
        @Units.TemperatureUnits tempUnit: String?
    ) {
        val temp_str = temp?.removeNonDigitChars()?.toString()
        var temp_f = temp_str?.toFloatOrNull()
        if (temp_f != null) {
            if (ObjectsCompat.equals(
                    tempUnit,
                    Units.CELSIUS
                ) || temp?.endsWith(Units.CELSIUS) == true
            ) {
                temp_f = ConversionMethods.CtoF(temp_f)
            }

            view.setTextColor(getColorFromTempF(temp_f))
        } else {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.colorTextPrimary))
        }
    }
}