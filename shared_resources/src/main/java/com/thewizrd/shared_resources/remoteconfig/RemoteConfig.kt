package com.thewizrd.shared_resources.remoteconfig

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider
import com.thewizrd.shared_resources.locationdata.locationiq.LocationIQProvider
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherAPIs
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RemoteConfig {
    private const val DEFAULT_WEATHERPROVIDER_KEY = "default_weather_provider"

    @JvmStatic
    fun getLocationProvider(weatherAPI: String): LocationProviderImpl? {
        val configJson = FirebaseRemoteConfig.getInstance().getString(weatherAPI)

        val config = JSONParser.deserializer<WeatherProviderConfig>(configJson, WeatherProviderConfig::class.java)

        if (config != null) {
            when (config.locSource) {
                //WeatherAPI.HERE -> return HERELocationProvider()
                WeatherAPI.LOCATIONIQ -> return LocationIQProvider()
                WeatherAPI.GOOGLE -> return GoogleLocationProvider()
                WeatherAPI.WEATHERAPI -> return WeatherApiLocationProvider()
            }
        }
        return null
    }

    @JvmStatic
    fun isProviderEnabled(weatherAPI: String): Boolean {
        val configJson = FirebaseRemoteConfig.getInstance().getString(weatherAPI)

        val config = JSONParser.deserializer<WeatherProviderConfig>(configJson, WeatherProviderConfig::class.java)

        return config?.isEnabled ?: true
    }

    @JvmStatic
    fun updateWeatherProvider(): Boolean {
        val settingsMgr = SimpleLibrary.getInstance().app.settingsManager
        val API = settingsMgr.getAPI() ?: return false

        val configJson = FirebaseRemoteConfig.getInstance().getString(API)
        val config = JSONParser.deserializer<WeatherProviderConfig>(configJson, WeatherProviderConfig::class.java)

        if (config != null) {
            val isEnabled = config.isEnabled

            if (!isEnabled) {
                if (config.newWeatherSource?.isNotBlank() == true) {
                    settingsMgr.setAPI(config.newWeatherSource)
                    WeatherManager.instance.updateAPI()
                } else {
                    settingsMgr.setAPI(getDefaultWeatherProvider())
                    WeatherManager.instance.updateAPI()
                }
                return true
            }
        }
        return false
    }

    @JvmStatic
    @WeatherAPIs
    fun getDefaultWeatherProvider(): String {
        return FirebaseRemoteConfig.getInstance().getString(DEFAULT_WEATHERPROVIDER_KEY)
    }

    @JvmStatic
    fun checkConfig() {
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
                .addOnCompleteListener { task: Task<Boolean?>? ->
                    // Update weather provider if needed
                    updateWeatherProvider()
                }
    }

    suspend fun checkConfigAsync() = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine<Boolean> { continuation ->
            FirebaseRemoteConfig.getInstance().fetchAndActivate()
                    .addOnCompleteListener {
                        // Update weather provider if needed
                        updateWeatherProvider()
                        if (continuation.isActive) {
                            if (it.exception == null) {
                                continuation.resume(it.result)
                            } else {
                                continuation.resumeWithException(it.exception!!)
                            }
                        }
                    }
        }
    }
}