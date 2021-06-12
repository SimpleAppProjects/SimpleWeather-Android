package com.thewizrd.simpleweather.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.SimpleRecyclerViewAdapterObserver
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter
import com.thewizrd.simpleweather.adapters.MinutelyItemAdapter
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment

class WeatherListFragment : SwipeDismissFragment() {
    private val forecastsView: ForecastsListViewModel by activityViewModels()
    private val forecastsPanelView: ForecastPanelsViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var itemDecoration: DividerItemDecoration

    private var weatherListType: WeatherListType? = null

    private var args: WeatherListFragmentArgs? = null

    init {
        arguments = Bundle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherList: onCreate")

        args = WeatherListFragmentArgs.fromBundle(requireArguments())

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherListType =
                    WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE))
            }
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                locationData = JSONParser.deserializer(
                    savedInstanceState.getString(Constants.KEY_DATA),
                    LocationData::class.java
                )
            }
        } else {
            weatherListType = args?.weatherListType
            if (args?.data != null) {
                locationData = JSONParser.deserializer(args?.data, LocationData::class.java)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use this to return your custom view for this Fragment
        val outerView = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        binding = FragmentWeatherListBinding.inflate(inflater, outerView)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.isEdgeItemsCenteringEnabled = true
        binding.recyclerView.layoutManager = LinearLayoutManager(fragmentActivity)
        binding.recyclerView.requestFocus()

        itemDecoration = DividerItemDecoration(fragmentActivity, DividerItemDecoration.VERTICAL)

        return outerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherList: onResume")
        initialize()
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherList: onPause")
        super.onPause()
    }

    private fun initialize() {
        runWithView {
            binding.recyclerView.requestFocus()

            if (locationData == null) locationData = settingsManager.getHomeData()
            forecastsView.updateForecasts(locationData!!)
            alertsView.updateAlerts(locationData!!)
            forecastsPanelView.updateForecasts(locationData!!)

            // specify an adapter (see also next example)
            when (weatherListType) {
                WeatherListType.FORECAST, WeatherListType.HOURLYFORECAST -> {
                    if (binding.recyclerView.itemDecorationCount == 0) {
                        binding.recyclerView.addItemDecoration(itemDecoration)
                    }

                    if (weatherListType == WeatherListType.FORECAST) {
                        forecastsView.getForecasts()?.removeObservers(this@WeatherListFragment)
                        forecastsView.getForecasts()
                            ?.observe(this@WeatherListFragment, Observer { forecasts ->
                                val detailsAdapter =
                                    getForecastAdapter<ForecastItemViewModel>(binding.recyclerView)
                                detailsAdapter.submitList(forecasts)
                            })
                    } else {
                        forecastsView.getHourlyForecasts()
                            ?.removeObservers(this@WeatherListFragment)
                        forecastsView.getHourlyForecasts()
                            ?.observe(this@WeatherListFragment, Observer { hrforecasts ->
                                val detailsAdapter =
                                    getForecastAdapter<HourlyForecastItemViewModel>(binding.recyclerView)
                                detailsAdapter.submitList(hrforecasts)
                            })
                    }
                }
                WeatherListType.ALERTS -> {
                    binding.recyclerView.removeItemDecoration(itemDecoration)
                    val alertAdapter = binding.recyclerView.adapter as? WeatherAlertPanelAdapter?
                        ?: WeatherAlertPanelAdapter()
                    if (binding.recyclerView.adapter !== alertAdapter) {
                        binding.recyclerView.adapter = alertAdapter
                    }

                    alertAdapter.registerAdapterDataObserver(object :
                        SimpleRecyclerViewAdapterObserver() {
                        override fun onChanged() {
                            alertAdapter.unregisterAdapterDataObserver(this)
                            binding.progressBar.visibility = View.GONE
                        }
                    })

                    alertsView.getAlerts()?.removeObservers(this@WeatherListFragment)
                    alertsView.getAlerts()?.observe(this@WeatherListFragment, Observer { alerts ->
                        alertAdapter.submitList(alerts)
                    })
                }
                WeatherListType.PRECIPITATION -> {
                    if (binding.recyclerView.itemDecorationCount == 0) {
                        binding.recyclerView.addItemDecoration(itemDecoration)
                    }

                    val minForecastAdapter = binding.recyclerView.adapter as? MinutelyItemAdapter?
                        ?: MinutelyItemAdapter()
                    if (binding.recyclerView.adapter !== minForecastAdapter) {
                        binding.recyclerView.adapter = minForecastAdapter
                    }

                    minForecastAdapter.registerAdapterDataObserver(object :
                        SimpleRecyclerViewAdapterObserver() {
                        override fun onChanged() {
                            minForecastAdapter.unregisterAdapterDataObserver(this)
                            binding.progressBar.visibility = View.GONE
                        }
                    })

                    forecastsPanelView.getMinutelyForecasts()
                        ?.removeObservers(this@WeatherListFragment)
                    forecastsPanelView.getMinutelyForecasts()
                        ?.observe(this@WeatherListFragment, Observer {
                            minForecastAdapter.submitList(it)
                        })
                }
                else -> {
                    binding.recyclerView.adapter = null
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun <T : BaseForecastItemViewModel> getForecastAdapter(
        recyclerView: RecyclerView
    ): ForecastItemAdapter<T> {
        @Suppress("UNCHECKED_CAST")
        val detailsAdapter: ForecastItemAdapter<T> =
            recyclerView.adapter as? ForecastItemAdapter<T>?
                ?: ForecastItemAdapter<T>()
        if (recyclerView.adapter !== detailsAdapter) {
            recyclerView.adapter = detailsAdapter
        }

        detailsAdapter.registerAdapterDataObserver(object : SimpleRecyclerViewAdapterObserver() {
            override fun onChanged() {
                if (detailsAdapter.currentList != null) {
                    detailsAdapter.unregisterAdapterDataObserver(this)
                    binding.progressBar.visibility = View.GONE
                }
            }
        })

        return detailsAdapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save data
        outState.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherListType!!.value)
        outState.putString(
            Constants.KEY_DATA,
            JSONParser.serializer(locationData, LocationData::class.java)
        )
        super.onSaveInstanceState(outState)
    }
}