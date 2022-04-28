package com.thewizrd.simpleweather.main

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrResourceId
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentWeatherRadarBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.radar.RadarProvider.getRadarViewProvider
import com.thewizrd.simpleweather.radar.RadarViewProvider
import com.thewizrd.simpleweather.snackbar.SnackbarManager

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class WeatherRadarFragment : ToolbarFragment() {
    private val weatherView: WeatherNowViewModel by activityViewModels()
    private lateinit var binding: FragmentWeatherRadarBinding

    private var radarViewProvider: RadarViewProvider? = null

    override fun createSnackManager(activity: Activity): SnackbarManager {
        val mSnackMgr = SnackbarManager(binding.root)
        mSnackMgr.setSwipeDismissEnabled(true)
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        return mSnackMgr
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherRadarFragment: onCreate")
        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
        sharedElementEnterTransition = MaterialContainerTransform()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherRadarBinding.inflate(inflater, root, true)

        ViewCompat.setTransitionName(binding.radarWebviewContainer, "radar")

        // Setup Actionbar
        toolbar.setNavigationIcon(toolbar.context.getAttrResourceId(R.attr.homeAsUpIndicator))
        toolbar.setNavigationOnClickListener { v: View? -> v?.findNavController()?.navigateUp() }

        toolbar.inflateMenu(R.menu.radar)
        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> {
                    if (radarViewProvider != null && weatherView.locationCoord != null) {
                        radarViewProvider!!.updateCoordinates(weatherView.locationCoord, true)
                    }
                    return@OnMenuItemClickListener true
                }
            }
            false
        })

        radarViewProvider = getRadarViewProvider(requireContext(), binding.radarWebviewContainer)
        radarViewProvider!!.enableInteractions(true)
        radarViewProvider!!.onCreateView(savedInstanceState)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (weatherView.locationCoord != null) {
            radarViewProvider?.onViewCreated(weatherView.locationCoord)
        }
    }

    override fun onStart() {
        super.onStart()
        radarViewProvider?.onStart()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("WeatherRadarFragment: onResume")

        radarViewProvider?.onResume()
        initialize()
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherRadarFragment: onPause")

        radarViewProvider?.onPause()
        super.onPause()
    }

    override fun onStop() {
        radarViewProvider?.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        radarViewProvider?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        radarViewProvider?.onDestroyView()
        radarViewProvider = null

        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        radarViewProvider?.onLowMemory()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        radarViewProvider?.onConfigurationChanged()
    }

    override val titleResId: Int
        get() = R.string.label_radar

    // Initialize views here
    @CallSuper
    protected fun initialize() {
        radarViewProvider?.updateRadarView()
    }
}