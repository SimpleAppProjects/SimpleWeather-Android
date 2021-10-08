package com.thewizrd.simpleweather.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.updatePadding
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
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter
import com.thewizrd.simpleweather.adapters.MinutelyItemAdapter
import com.thewizrd.simpleweather.controls.ForecastPanelsViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment

class WeatherListFragment : SwipeDismissFragment() {
    companion object {
        fun newInstance(
            type: WeatherListType,
            data: String? = null,
            scrollToPosition: Int? = null
        ): WeatherListFragment {
            val b = Bundle(3)
            b.putSerializable(Constants.ARGS_WEATHERLISTTYPE, type)
            if (data != null) {
                b.putString(Constants.KEY_DATA, data)
            }
            if (scrollToPosition != null) {
                b.putInt(Constants.KEY_POSITION, scrollToPosition)
            }

            return WeatherListFragment().apply {
                arguments = b
            }
        }
    }

    private val forecastsView: ForecastsListViewModel by activityViewModels()
    private val forecastsPanelView: ForecastPanelsViewModel by activityViewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var itemDecoration: DividerItemDecoration

    private var weatherListType: WeatherListType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherList: onCreate")

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
            weatherListType =
                arguments?.getSerializable(Constants.ARGS_WEATHERLISTTYPE) as? WeatherListType
            if (arguments?.containsKey(Constants.KEY_DATA) == true) {
                locationData = arguments?.getString(Constants.KEY_DATA)?.let { data ->
                    JSONParser.deserializer(data, LocationData::class.java)
                }
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
        binding.recyclerView.layoutManager = LinearLayoutManager(fragmentActivity)
        binding.recyclerView.requestFocus()

        itemDecoration = DividerItemDecoration(fragmentActivity, DividerItemDecoration.VERTICAL)

        val verticalPadding = requireContext().dpToPx(48f).toInt()
        binding.recyclerView.updatePadding(top = verticalPadding, bottom = verticalPadding)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                binding.timeText.apply {
                    translationY = -recyclerView.computeVerticalScrollOffset().toFloat()
                }
            }
        })

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
                    binding.recyclerView.removeItemDecoration(itemDecoration)
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
            val scrollToPosition = arguments?.getInt(Constants.KEY_POSITION, 0) ?: 0

            override fun onChanged() {
                if (detailsAdapter.currentList != null && detailsAdapter.itemCount > scrollToPosition) {
                    detailsAdapter.unregisterAdapterDataObserver(this)
                    detailsAdapter.currentList!!.loadAround(scrollToPosition)
                    binding.recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            binding.recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            runWithView {
                                val layoutMgr =
                                    binding.recyclerView.layoutManager as? LinearLayoutManager
                                layoutMgr?.scrollToPositionWithOffset(scrollToPosition, 0)
                            }
                        }
                    })
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