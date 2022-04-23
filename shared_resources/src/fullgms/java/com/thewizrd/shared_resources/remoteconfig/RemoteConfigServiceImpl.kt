package com.thewizrd.shared_resources.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RemoteConfigServiceImpl : RemoteConfigService {
    companion object {
        private const val DEFAULT_WEATHERPROVIDER_KEY = "default_weather_provider"
    }

    override fun getLocationProvider(weatherAPI: String): String? {
        val configJson = FirebaseRemoteConfig.getInstance().getString(weatherAPI)

        val config = JSONParser.deserializer<WeatherProviderConfig>(
            configJson,
            WeatherProviderConfig::class.java
        )

        return config?.locSource
    }

    override fun isProviderEnabled(weatherAPI: String): Boolean {
        val configJson = FirebaseRemoteConfig.getInstance().getString(weatherAPI)

        val config = JSONParser.deserializer<WeatherProviderConfig>(
            configJson,
            WeatherProviderConfig::class.java
        )

        return config?.isEnabled ?: true
    }

    override fun updateWeatherProvider(): Boolean {
        val settingsMgr = appLib.settingsManager
        val API = settingsMgr.getAPI() ?: return false

        val configJson = FirebaseRemoteConfig.getInstance().getString(API)
        val config = JSONParser.deserializer<WeatherProviderConfig>(
            configJson,
            WeatherProviderConfig::class.java
        )

        if (config != null) {
            val isEnabled = config.isEnabled

            if (!isEnabled) {
                if (config.newWeatherSource?.isNotBlank() == true) {
                    settingsMgr.setAPI(config.newWeatherSource)
                } else {
                    settingsMgr.setAPI(getDefaultWeatherProvider())
                }
                return true
            }
        }
        return false
    }

    @WeatherAPI.WeatherProviders
    override fun getDefaultWeatherProvider(): String {
        return FirebaseRemoteConfig.getInstance().getString(DEFAULT_WEATHERPROVIDER_KEY)
    }

    @WeatherAPI.WeatherProviders
    override fun getDefaultWeatherProvider(countryCode: String?): String {
        return when {
            LocationUtils.isUS(countryCode) -> {
                WeatherAPI.NWS
            }
            LocationUtils.isFrance(countryCode) -> {
                WeatherAPI.METEOFRANCE
            }
            else -> {
                getDefaultWeatherProvider()
            }
        }
    }

    override fun checkConfig() {
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
            .addOnCompleteListener {
                // Update weather provider if needed
                updateWeatherProvider()
            }
    }

    override suspend fun checkConfigAsync() = withContext(Dispatchers.Default) {
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