package com.thewizrd.simpleweather.main

import android.graphics.Outline
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.ChartsItemAdapter
import com.thewizrd.simpleweather.controls.viewmodels.ChartsViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.*

class WeatherChartsFragment : ToolbarFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private val chartsView: ChartsViewModel by viewModels()
    private var locationData: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var adapter: ChartsItemAdapter

    private lateinit var args: WeatherChartsFragmentArgs

    private val wm = WeatherManager.instance

    init {
        arguments = Bundle()
    }

    companion object {
        fun newInstance(locData: LocationData): WeatherChartsFragment {
            val fragment = WeatherChartsFragment()
            fragment.locationData = locData
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherChartsFragment: onCreate")

        args = WeatherChartsFragmentArgs.fromBundle(requireArguments())

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                locationData = JSONParser.deserializer(
                    savedInstanceState.getString(Constants.KEY_DATA),
                    LocationData::class.java
                )
            }
        } else {
            if (args.data != null) {
                locationData = JSONParser.deserializer(args.data, LocationData::class.java)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true)
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup Actionbar
        val context = binding.root.context
        val navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp)!!).mutate()
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText))
        toolbar.navigationIcon = navIcon

        toolbar.setNavigationOnClickListener { v ->
            Navigation.findNavController(v).navigateUp()
        }

        binding.locationHeader.clipToOutline = false
        binding.locationHeader.outlineProvider = object : ViewOutlineProvider() {
            val elevation = context.resources.getDimensionPixelSize(R.dimen.appbar_elevation)
            override fun getOutline(view: View, outline: Outline) {
                outline.setRect(0, view.height - elevation, view.width, view.height)
            }
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true)
        // use a linear layout manager
        binding.recyclerView.layoutManager = LinearLayoutManager(appCompatActivity)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateHeaderElevation()
            }
        })

        binding.recyclerView.adapter = ChartsItemAdapter().also {
            adapter = it
        }
        return root
    }

    private fun updateHeaderElevation() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (binding.recyclerView.computeVerticalScrollOffset() > 0) {
                binding.locationHeader.elevation = ContextUtils.dpToPx(requireContext(), 4f)
            } else {
                binding.locationHeader.elevation = 0f
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args = WeatherChartsFragmentArgs.fromBundle(requireArguments())

        binding.locationHeader.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.locationHeader.viewTreeObserver.removeOnPreDrawListener(this)
                runWithView(Dispatchers.Main.immediate) {
                    val layoutParams = binding.recyclerView.layoutParams as MarginLayoutParams
                    layoutParams.topMargin = binding.locationHeader.height
                    binding.recyclerView.layoutParams = layoutParams
                }
                return true
            }
        })

        binding.progressBar.visibility = View.VISIBLE

        chartsView.getGraphModelData().observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            AnalyticsLogger.logEvent("WeatherChartsFragment: onResume")
            initialize()
        }
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherChartsFragment: onPause")
        super.onPause()
    }

    override fun getTitle(): Int {
        return R.string.label_forecast
    }

    private fun initialize() {
        runWithView {
            if (locationData == null) locationData = getSettingsManager().getHomeData()

            if (!weatherView.isValid || locationData != null && locationData!!.query != weatherView.query) {
                runWithView(Dispatchers.Default) {
                    supervisorScope {
                        val weather = WeatherDataLoader(locationData!!).loadWeatherData(
                            WeatherRequest.Builder()
                                .forceLoadSavedData()
                                .setErrorListener { wEx ->
                                    when (wEx.errorStatus) {
                                        ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                                            // Show error message and prompt to refresh
                                            showSnackbar(
                                                Snackbar.make(
                                                    wEx.message,
                                                    Snackbar.Duration.LONG
                                                ), null
                                            )
                                        }
                                        ErrorStatus.QUERYNOTFOUND -> {
                                            if (!wm.isRegionSupported(locationData!!.countryCode)) {
                                                showSnackbar(
                                                    Snackbar.make(
                                                        R.string.error_message_weather_region_unsupported,
                                                        Snackbar.Duration.LONG
                                                    ), null
                                                )
                                                return@setErrorListener
                                            }
                                            // Show error message
                                            showSnackbar(
                                                Snackbar.make(
                                                    wEx.message,
                                                    Snackbar.Duration.LONG
                                                ), null
                                            )
                                        }
                                        else -> {
                                            showSnackbar(
                                                Snackbar.make(
                                                    wEx.message,
                                                    Snackbar.Duration.LONG
                                                ), null
                                            )
                                        }
                                    }
                                }.build()
                        )

                        ensureActive()

                        launch(Dispatchers.Main) {
                            weatherView.updateView(weather)
                            chartsView.updateForecasts(locationData!!)
                            binding.locationName.text = weatherView.location
                        }
                    }
                }
            } else {
                chartsView.updateForecasts(locationData!!)
                binding.locationName.text = weatherView.location
            }

            binding.progressBar.visibility = View.GONE
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

        var color = ContextUtils.getColor(appCompatActivity!!, android.R.attr.colorBackground)
        if (getSettingsManager().getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }
        binding.locationHeader.setCardBackgroundColor(color)
        binding.recyclerView.setBackgroundColor(color)
    }

    override fun createSnackManager(): SnackbarManager {
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }
}