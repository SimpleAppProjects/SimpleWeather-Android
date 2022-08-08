package com.thewizrd.simpleweather.setup

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.navGraphViewModels
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupLocationBinding
import com.thewizrd.simpleweather.fragments.CustomFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import com.thewizrd.simpleweather.utils.NavigationUtils.safeNavigate
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.wearable.WearableWorkerActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SetupLocationFragment : CustomFragment() {
    companion object {
        private const val TAG = "SetupLocationFragment"
    }

    // Views
    private lateinit var binding: FragmentSetupLocationBinding

    private val viewModel: SetupViewModel by activityViewModels()
    private val locationSearchViewModel: LocationSearchViewModel by navGraphViewModels("/locations")

    private var job: Job? = null

    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun createSnackManager(activity: Activity): SnackbarManager {
        val mStepperNavBar = activity.findViewById<View>(R.id.bottom_nav_bar)
        return SnackbarManager(binding.root).apply {
            setSwipeDismissEnabled(true)
            setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
            setAnchorView(mStepperNavBar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SetupLocation: onCreate")

        // Hold fragment in place for MaterialContainerTransform
        exitTransition = Hold().setDuration(Constants.ANIMATION_DURATION.toLong())

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)

        locationPermissionLauncher = LocationPermissionLauncher(
            this,
            locationCallback = { granted ->
                if (granted) {
                    // permission was granted, yay!
                    // Do the task you need to do.
                    fetchGeoLocation()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showSnackbar(
                        Snackbar.make(
                            requireContext(),
                            R.string.error_location_denied,
                            Snackbar.Duration.SHORT
                        )
                    )
                }
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false)

        binding.progressBar.visibility = View.GONE

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener { v ->
            v.isEnabled = false
            binding.gpsFollow.isEnabled = false

            // Setup search UI
            activity?.let {
                val bottomNavBar = it.findViewById<View>(R.id.bottom_nav_bar)
                bottomNavBar.visibility = View.GONE

                v.findNavController()
                    .safeNavigate(
                        SetupLocationFragmentDirections.actionSetupLocationFragmentToLocationSearchFragment3(),
                        FragmentNavigator.Extras.Builder()
                            .addSharedElement(v, Constants.SHARED_ELEMENT)
                            .build()
                    )
            }
        }
        ViewCompat.setTransitionName(binding.searchBar.searchViewContainer, Constants.SHARED_ELEMENT)

        binding.gpsFollow.setOnClickListener { fetchGeoLocation() }

        // Reset focus
        binding.root.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = view.findNavController()

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.selectedSearchLocation.collectLatest { location ->
                if (location?.isValid == true) {
                    // Setup complete
                    viewModel.locationData = location
                    navController.safeNavigate(
                        SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment()
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.currentLocation.collectLatest { location ->
                if (location?.isValid == true) {
                    // Save weather data
                    settingsManager.saveLastGPSLocData(location)
                    settingsManager.deleteLocations()
                    settingsManager.addLocation(location)

                    settingsManager.setFollowGPS(true)
                    settingsManager.setWeatherLoaded(true)

                    // Send data for wearables
                    context?.let {
                        WearableWorker.enqueueAction(
                            it,
                            WearableWorkerActions.ACTION_SENDUPDATE
                        )
                    }

                    // Setup complete
                    viewModel.locationData = location
                    navController.safeNavigate(
                        SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment()
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }
    }

    override fun onDestroyView() {
        job?.cancel()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsLogger.logEvent("SetupLocation: onResume")
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("SetupLocation: onPause")
        job?.cancel()
        super.onPause()
    }

    override fun onDestroy() {
        // Cancel pending actions
        job?.cancel()
        super.onDestroy()
    }

    private fun fetchGeoLocation() {
        val ctx = context ?: return

        if (!ctx.locationPermissionEnabled()) {
            locationPermissionLauncher.requestLocationPermission()
            return
        }

        locationSearchViewModel.fetchGeoLocation()
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                context?.let {
                    showSnackbar(Snackbar.make(it, error.stringId, Snackbar.Duration.SHORT))
                }
            }
            is ErrorMessage.String -> {
                context?.let {
                    showSnackbar(Snackbar.make(it, error.message, Snackbar.Duration.SHORT))
                }
            }
            is ErrorMessage.WeatherError -> {
                context?.let {
                    showSnackbar(
                        Snackbar.make(
                            it,
                            error.exception.message,
                            Snackbar.Duration.SHORT
                        )
                    )
                }
            }
        }

        locationSearchViewModel.setErrorMessageShown(error)
    }
}