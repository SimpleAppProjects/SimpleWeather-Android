package com.thewizrd.simpleweather.preferences

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.thewizrd.common.helpers.LocationPermissionLauncher
import com.thewizrd.common.helpers.backgroundLocationPermissionEnabled
import com.thewizrd.common.helpers.getBackgroundLocationRationale
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.UpdaterUtils
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.snackbar.Snackbar

abstract class BaseSettingsFragment : ToolbarPreferenceFragmentCompat() {
    // Intent queue
    private val intentQueue = mutableSetOf<Intent.FilterComparison>()

    protected lateinit var locationPermissionLauncher: LocationPermissionLauncher
    protected lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationPermissionLauncher =
            LocationPermissionLauncher(this, locationCallback = ::onLocationPermissionCallback)
        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    }

    protected open fun onLocationPermissionCallback(granted: Boolean) {
        if (granted) {
            // permission was granted, yay!
            // Do the task you need to do.
            settingsManager.setFollowGPS(true)
        } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            settingsManager.setFollowGPS(false)
            context?.let {
                showSnackbar(
                    Snackbar.make(
                        it,
                        R.string.error_location_denied,
                        Snackbar.Duration.SHORT
                    )
                )
            }
        }
    }

    protected fun checkBackgroundLocationAccess() {
        activity?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !settingsManager.requestedBGAccess() &&
                !it.backgroundLocationPermissionEnabled()
            ) {
                val snackbar = Snackbar.make(
                    it,
                    it.getBackgroundLocationRationale(),
                    Snackbar.Duration.VERY_LONG
                )
                snackbar.setAction(android.R.string.ok) {
                    locationPermissionLauncher.requestBackgroundLocationPermission()
                }
                showSnackbar(snackbar, null)
                settingsManager.setRequestBGAccess(true)
            }
        }
    }

    protected open fun enqueueIntent(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        } else {
            if (WeatherUpdaterWorker.ACTION_REQUEUEWORK == intent.action || (WeatherUpdaterWorker.ACTION_ENQUEUEWORK == intent.action)) {
                for (filter: Intent.FilterComparison in intentQueue) {
                    if (WeatherUpdaterWorker.ACTION_CANCELWORK == filter.intent.action) {
                        intentQueue.remove(filter)
                        break
                    }
                }
            } else if (WeatherUpdaterWorker.ACTION_CANCELWORK == intent.action) {
                for (filter: Intent.FilterComparison in intentQueue) {
                    if (WeatherUpdaterWorker.ACTION_REQUEUEWORK == filter.intent.action || WeatherUpdaterWorker.ACTION_ENQUEUEWORK == intent.action) {
                        intentQueue.remove(filter)
                        break
                    }
                }
            }

            return intentQueue.add(Intent.FilterComparison(intent))
        }
    }

    protected open fun processIntentQueue(intentQueue: Collection<Intent.FilterComparison>) {
        intentQueue.forEach { filter ->
            when (filter.intent.component!!.className) {
                WeatherUpdaterWorker::class.java.name -> {
                    when (filter.intent.action) {
                        WeatherUpdaterWorker.ACTION_REQUEUEWORK -> {
                            UpdaterUtils.updateAlarm(requireContext())
                        }
                        WeatherUpdaterWorker.ACTION_ENQUEUEWORK -> {
                            UpdaterUtils.startAlarm(requireContext())
                        }
                        WeatherUpdaterWorker.ACTION_CANCELWORK -> {
                            UpdaterUtils.cancelAlarm(requireContext())
                        }
                        else -> {
                            WeatherUpdaterWorker.enqueueAction(
                                requireContext(),
                                (filter.intent.action)!!
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        processIntentQueue(intentQueue)
        intentQueue.clear()

        super.onPause()
    }
}