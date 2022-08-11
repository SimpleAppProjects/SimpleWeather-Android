package com.thewizrd.simpleweather.setup

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ActivityUtils.showToast
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.viewmodels.LocationSearchViewModel
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.di.localBroadcastManager
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.FragmentSetupBinding
import com.thewizrd.simpleweather.fragments.LocationSearchFragment
import com.thewizrd.simpleweather.helpers.AcceptDenyDialog
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SetupActivity : UserLocaleActivity() {
    companion object {
        private const val TAG = "SetupActivity"
        private const val REQUEST_CODE_SYNC_ACTIVITY = 10
    }

    // Views
    private lateinit var binding: FragmentSetupBinding
    private val locationSearchViewModel: LocationSearchViewModel by viewModels()

    private lateinit var locationPermissionLauncher: LocationPermissionLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("SetupActivity: onCreate")

        binding = FragmentSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Controls
        binding.searchButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, LocationSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.locationButton.setOnClickListener { fetchGeoLocation() }

        binding.setupPhoneButton.setOnClickListener {
            AcceptDenyDialog.Builder(this) { _, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    startActivityForResult(
                        Intent(this, SetupSyncActivity::class.java),
                        REQUEST_CODE_SYNC_ACTIVITY
                    )
                }
            }.setMessage(R.string.prompt_confirmsetup)
                .show()
        }

        binding.progressBarContainer.visibility = View.GONE

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
                    settingsManager.setFollowGPS(false)
                    showToast(R.string.error_location_denied, Toast.LENGTH_SHORT)
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            locationSearchViewModel.currentLocation.collectLatest { location ->
                if (location?.isValid == true) {
                    settingsManager.updateLocation(location)

                    settingsManager.setFollowGPS(true)
                    settingsManager.setWeatherLoaded(true)

                    // If we're changing locations, trigger an update
                    if (settingsManager.isWeatherLoaded()) {
                        localBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                    }

                    settingsManager.setFollowGPS(true)
                    settingsManager.setWeatherLoaded(true)
                    settingsManager.setDataSync(WearableDataSync.OFF)

                    // Start WeatherNow Activity with weather data
                    val intent = Intent(this@SetupActivity, MainActivity::class.java).apply {
                        putExtra(Constants.KEY_DATA, JSONParser.serializer(location))
                    }
                    startActivity(intent)
                    finishAffinity()
                } else {
                    settingsManager.setFollowGPS(false)
                }
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.selectedSearchLocation.collectLatest { location ->
                location?.data?.takeIf { it.isValid }?.let {
                    settingsManager.updateLocation(it)

                    settingsManager.setFollowGPS(false)
                    settingsManager.setWeatherLoaded(true)
                    settingsManager.setDataSync(WearableDataSync.OFF)

                    // Start WeatherNow Activity with weather data
                    val intent = Intent(this@SetupActivity, MainActivity::class.java).apply {
                        putExtra(Constants.KEY_DATA, JSONParser.serializer(it))
                    }
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.errorMessages.collect {
                val error = it.firstOrNull()

                if (error != null) {
                    onErrorMessage(error)
                }
            }
        }

        lifecycleScope.launch {
            locationSearchViewModel.isLoading.collect { loading ->
                enableControls(!loading)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_SYNC_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    lifecycleScope.launch {
                        if (settingsManager.getHomeData() != null) {
                            settingsManager.setDataSync(WearableDataSync.DEVICEONLY)
                            settingsManager.setWeatherLoaded(true)
                            // Start WeatherNow Activity
                            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                            finishAffinity()
                        }
                    }
                }
            }
        }
    }

    private fun enableControls(enable: Boolean) {
        binding.searchButton.isEnabled = enable
        binding.locationButton.isEnabled = enable
        binding.progressBarContainer.isVisible = !enable
    }

    private fun fetchGeoLocation() {
        if (!locationPermissionEnabled()) {
            locationPermissionLauncher.requestLocationPermission()
            return
        }

        locationSearchViewModel.fetchGeoLocation()
    }

    private fun onErrorMessage(error: ErrorMessage) {
        when (error) {
            is ErrorMessage.Resource -> {
                showToast(error.stringId, Toast.LENGTH_SHORT)
            }
            is ErrorMessage.String -> {
                showToast(error.message, Toast.LENGTH_SHORT)
            }
            is ErrorMessage.WeatherError -> {
                showToast(error.exception.message, Toast.LENGTH_SHORT)
            }
        }

        locationSearchViewModel.setErrorMessageShown(error)
    }
}