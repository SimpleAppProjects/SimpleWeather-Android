package com.thewizrd.simpleweather.databinding

import androidx.databinding.BindingAdapter
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.*
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import com.thewizrd.simpleweather.controls.viewmodels.RangeBarGraphMapper.createGraphData

object GraphBindingAdapter {
    @JvmStatic
    @BindingAdapter("graphData")
    fun updateForecastGraph(view: ForecastGraphPanel, graphData: GraphData<*>?) {
        view.setGraphData(graphData as LineViewData?)
    }

    @JvmStatic
    @BindingAdapter("graphData")
    fun updateForecastGraph(view: RangeBarGraphPanel, graphData: RangeBarGraphData?) {
        view.setGraphData(graphData)
    }

    @JvmStatic
    @BindingAdapter("minForecastData")
    fun updateMinForecastGraph(view: ForecastGraphPanel, forecastData: List<MinutelyForecast>?) {
        if (!forecastData.isNullOrEmpty()) {
            val vm = ForecastGraphViewModel()
            vm.setMinutelyForecastData(forecastData)
            view.setGraphData(vm.graphData as LineViewData?)
        } else {
            view.setGraphData(null)
        }
    }

    @JvmStatic
    @BindingAdapter("forecastData")
    fun updateForecastGraph(view: ForecastGraphPanel, forecastData: List<HourlyForecast>?) {
        if (!forecastData.isNullOrEmpty()) {
            val vm = ForecastGraphViewModel().apply {
                setForecastData(
                    forecastData,
                    ForecastGraphViewModel.ForecastGraphType.PRECIPITATION
                )
            }
            view.setGraphData(vm.graphData as LineViewData?)
        } else {
            view.setGraphData(null)
        }
    }

    @JvmStatic
    @BindingAdapter("forecastData")
    fun updateForecastGraph(view: RangeBarGraphPanel, forecastData: List<Forecast>?) {
        val maxForecasts = view.context.resources.getInteger(R.integer.weathernow_max_forecasts)
        view.setGraphData(createGraphData(view.context, forecastData?.take(maxForecasts)))
    }

    @JvmStatic
    @BindingAdapter("graphData")
    fun updateBarGraph(view: BarGraphPanel, graphData: BarGraphData?) {
        view.setGraphData(graphData)
    }
}