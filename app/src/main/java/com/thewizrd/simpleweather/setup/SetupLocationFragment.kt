package com.thewizrd.simpleweather.setup

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.launch
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchResult
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.activities.LocationSearch
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
    private val locationSearchViewModel: LocationSearchViewModel by viewModels()

    private var job: Job? = null

    private lateinit var locationPermissionLauncher: LocationPermissionLauncher
    private lateinit var locationSearchLauncher: ActivityResultLauncher<Void?>

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

        locationSearchLauncher = registerForActivityResult(LocationSearch()) { result ->
            when (result) {
                is LocationSearchResult.AlreadyExists,
                is LocationSearchResult.Success -> {
                    lifecycleScope.launch {
                        result.data?.takeIf { it.isValid }?.let {
                            onLocationReceived(it)
                        }
                    }
                }
                is LocationSearchResult.Failed -> {
                    // no-op
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSetupLocationBinding.inflate(inflater, container, false)

        /* Event Listeners */
        binding.searchBar.searchViewContainer.setOnClickListener {
            locationSearchLauncher.launch(
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    it,
                    Constants.SHARED_ELEMENT
                )
            )
        }
        ViewCompat.setTransitionName(
            binding.searchBar.searchViewContainer,
            Constants.SHARED_ELEMENT
        )

        binding.gpsFollow.setOnClickListener { fetchGeoLocation() }

        // Reset focus
        binding.root.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.isLoading.collect { loading ->
                if (loading)
                    binding.progressBar.show()
                else
                    binding.progressBar.hide()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.selectedSearchLocation.collectLatest { location ->
                location?.data?.takeIf { it.isValid }?.let {
                    onLocationReceived(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            locationSearchViewModel.currentLocation.collectLatest { location ->
                if (location?.isValid == true) {
                    onLocationReceived(location)
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

    private suspend fun onLocationReceived(location: LocationData) {
        settingsManager.deleteLocations()

        if (location.locationType == LocationType.GPS) {
            settingsManager.saveLastGPSLocData(location)
            settingsManager.addLocation(LocationQuery(location).toLocationData())
            settingsManager.setFollowGPS(true)
        } else {
            settingsManager.saveLastGPSLocData(null)
            settingsManager.addLocation(location)
            settingsManager.setFollowGPS(false)
        }

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
        view?.findNavController()?.safeNavigate(
            SetupLocationFragmentDirections.actionSetupLocationFragmentToSetupSettingsFragment()
        )
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