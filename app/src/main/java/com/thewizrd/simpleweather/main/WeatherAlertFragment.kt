package com.thewizrd.simpleweather.main

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.thewizrd.common.adapters.WeatherAlertPanelAdapter
import com.thewizrd.common.controls.ForecastsListViewModel
import com.thewizrd.common.controls.WeatherAlertsViewModel
import com.thewizrd.common.helpers.SimpleRecyclerViewAdapterObserver
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.databinding.LayoutLocationHeaderBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.viewmodels.WeatherNowViewModel
import kotlinx.coroutines.launch

class WeatherAlertFragment : ToolbarFragment() {
    private val wNowViewModel: WeatherNowViewModel by activityViewModels()
    private val forecastsView: ForecastsListViewModel by viewModels()
    private val alertsView: WeatherAlertsViewModel by activityViewModels()

    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var headerBinding: LayoutLocationHeaderBinding
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var alertAdapter: WeatherAlertPanelAdapter

    private val args: WeatherAlertFragmentArgs by navArgs()

    init {
        arguments = Bundle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherListFragment: onCreate")

        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()

        if (savedInstanceState?.containsKey(Constants.KEY_DATA) == true) {
            locationData = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA))
        } else if (args.data != null) {
            locationData = JSONParser.deserializer(args.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true)
        headerBinding = LayoutLocationHeaderBinding.inflate(inflater, appBarLayout, true)

        binding.lifecycleOwner = viewLifecycleOwner
        headerBinding.lifecycleOwner = viewLifecycleOwner
        headerBinding.viewModel = wNowViewModel

        // Setup Actionbar
        toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(R.attr.homeAsUpIndicator))
        toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true)
        // use a linear layout manager
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).also { layoutManager = it }
        binding.recyclerView.adapter = WeatherAlertPanelAdapter().also { alertAdapter = it }

        return root
    }

    override val scrollTargetViewId: Int
        get() = binding.recyclerView.id

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progressBar.show()

        if (args.data.isNullOrBlank() && savedInstanceState?.containsKey(Constants.KEY_DATA) != true) {
            viewLifecycleOwner.lifecycleScope.launch {
                wNowViewModel.uiState.collect {
                    locationData = it.locationData
                    initialize()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                initialize()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherListFragment: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherListFragment: onPause")
        super.onPause()
    }

    override val titleResId: Int
        get() = R.string.title_fragment_alerts

    private suspend fun initialize() {
        if (locationData == null) {
            locationData = wNowViewModel.uiState.value.locationData
        }

        locationData?.let {
            forecastsView.updateForecasts(it)
            alertsView.updateAlerts(it)
        }

        alertAdapter.registerAdapterDataObserver(object : SimpleRecyclerViewAdapterObserver() {
            override fun onChanged() {
                alertAdapter.unregisterAdapterDataObserver(this)
                binding.progressBar.hide()
            }
        })

        alertsView.getAlerts().collect {
            alertAdapter.submitList(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save data
        outState.putString(
            Constants.KEY_DATA,
            JSONParser.serializer(locationData, LocationData::class.java)
        )
        super.onSaveInstanceState(outState)
    }

    override fun updateWindowColors() {
        super.updateWindowColors()

        context?.let { ctx ->
            var backgroundColor = ctx.getAttrColor(android.R.attr.colorBackground)
            if (settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                backgroundColor = Colors.BLACK
            }

            binding.recyclerView.setBackgroundColor(backgroundColor)
        }
    }

    override fun createSnackManager(activity: Activity): SnackbarManager {
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }
}