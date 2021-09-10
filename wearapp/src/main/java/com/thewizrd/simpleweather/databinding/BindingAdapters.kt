package com.thewizrd.simpleweather.databinding

import android.graphics.drawable.RotateDrawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.ObjectsCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Units.TemperatureUnits
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ForecastPanel
import com.thewizrd.simpleweather.controls.HourlyForecastPanel

object BindingAdapters {
    @BindingAdapter("forecasts")
    @JvmStatic
    fun updateForecasts(view: ForecastPanel, models: List<ForecastItemViewModel?>?) {
        view.bindModel(models)
    }

    @BindingAdapter("forecasts")
    @JvmStatic
    fun updateForecasts(view: HourlyForecastPanel, models: List<HourlyForecastItemViewModel?>?) {
        view.bindModel(models)
    }

    @BindingAdapter("popData")
    @JvmStatic
    fun updatePopLayout(view: ViewGroup, details: List<DetailItemViewModel>?) {
        val pop = view.findViewWithTag<TextView>("pop") ?: return

        if (details != null) {
            val chanceModel = details.find { input -> input.detailsType == WeatherDetailsType.POPCHANCE }

            if (chanceModel != null) {
                val wim = WeatherIconsManager.getInstance()

                if (pop is TextViewWeatherIconDrawableCompat) {
                    pop.weatherIconStart = chanceModel.icon
                    if (pop.iconProvider != null) {
                        pop.showAsMonochrome = wim.shouldUseMonochrome(pop.iconProvider)
                    } else {
                        pop.showAsMonochrome = wim.shouldUseMonochrome()
                    }
                } else {
                    val oldDrawables = TextViewCompat.getCompoundDrawablesRelative(pop)
                    if (oldDrawables[0] == null) {
                        pop.setCompoundDrawablesRelative(
                            ContextCompat.getDrawable(
                                pop.context,
                                wim.getWeatherIconResource(chanceModel.icon)
                            ), null, null, null
                        )
                    }
                }

                pop.text = chanceModel.value
                pop.visibility = View.VISIBLE
            } else {
                pop.visibility = View.GONE
            }
        } else {
            pop.visibility = View.GONE
        }
    }

    @BindingAdapter("windData")
    @JvmStatic
    fun updateWindLayout(view: ViewGroup, details: List<DetailItemViewModel>?) {
        val windSpeed = view.findViewWithTag<TextView>("windspeed") ?: return

        if (details != null) {
            val windModel = details.find { input -> input.detailsType == WeatherDetailsType.WINDSPEED }

            if (windModel != null) {
                val wim = WeatherIconsManager.getInstance()

                if (windSpeed is TextViewWeatherIconDrawableCompat) {
                    windSpeed.weatherIconStart = windModel.icon
                    if (windSpeed.iconProvider != null) {
                        windSpeed.showAsMonochrome = wim.shouldUseMonochrome(windSpeed.iconProvider)
                    } else {
                        windSpeed.showAsMonochrome = wim.shouldUseMonochrome()
                    }
                    windSpeed.iconRotation = windModel.iconRotation
                } else {
                    val oldDrawables = TextViewCompat.getCompoundDrawablesRelative(windSpeed)
                    if (oldDrawables[0] == null) {

                        val d = RotateDrawable()
                        d.fromDegrees = windModel.iconRotation.toFloat()
                        d.toDegrees = windModel.iconRotation.toFloat()
                        d.drawable = ContextCompat.getDrawable(
                            windSpeed.context,
                            wim.getWeatherIconResource(windModel.icon)
                        )
                        // Change level to trigger onLevelChange event
                        // which forces the drawable state to change
                        d.level = 10000
                        d.level = 0
                        TextViewCompat.setCompoundDrawablesRelative(windSpeed, d, null, null, null)
                    } else if (oldDrawables[0] is RotateDrawable) {
                        val d = oldDrawables[0] as RotateDrawable
                        if (d.fromDegrees != windModel.iconRotation.toFloat()) {
                            d.fromDegrees = windModel.iconRotation.toFloat()
                            d.toDegrees = windModel.iconRotation.toFloat()
                            // Change level to trigger onLevelChange event
                            // which forces the drawable state to change
                            d.level = 10000
                            d.level = 0
                        }
                    } else {
                        val d = RotateDrawable()
                        d.fromDegrees = windModel.iconRotation.toFloat()
                        d.toDegrees = windModel.iconRotation.toFloat()
                        d.drawable = oldDrawables[0]
                        // Change level to trigger onLevelChange event
                        // which forces the drawable state to change
                        d.level = 10000
                        d.level = 0
                        TextViewCompat.setCompoundDrawablesRelative(windSpeed, d, null, null, null)
                    }
                }

                var speed = if (TextUtils.isEmpty(windModel.value)) "" else windModel.value.toString()
                speed = speed.split(",").toTypedArray()[0]

                windSpeed.text = speed

                windSpeed.visibility = View.VISIBLE
            } else {
                windSpeed.visibility = View.GONE
            }
        } else {
            windSpeed.visibility = View.GONE
        }
    }

    @BindingAdapter(value = ["tempTextColor", "tempUnit"], requireAll = false)
    @JvmStatic
    fun tempTextColor(view: TextView, temp: CharSequence?, @TemperatureUnits tempUnit: String?) {
        val temp_str = StringUtils.removeNonDigitChars(temp)
        var temp_f = NumberUtils.tryParseFloat(temp_str)
        if (temp_f != null) {
            if (ObjectsCompat.equals(tempUnit, Units.CELSIUS) || temp.toString().endsWith(Units.CELSIUS)) {
                temp_f = ConversionMethods.CtoF(temp_f)
            }

            view.setTextColor(getColorFromTempF(temp_f, Colors.WHITE))
        } else {
            view.setTextColor(ContextCompat.getColor(view.context, R.color.colorTextPrimary))
        }
    }

    @BindingAdapter("watchHideIfEmpty")
    @JvmStatic
    fun <T : Any?> invisibleIfEmpty(view: View, c: Collection<T>?) {
        val isRound = view.context.resources.configuration.isScreenRound
        view.visibility = if (c.isNullOrEmpty()) (if (isRound) View.INVISIBLE else View.GONE) else View.VISIBLE
    }
}