package com.thewizrd.shared_resources

import android.content.Context
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.okhttp3.CacheInterceptor
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.Logger
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

private lateinit var _sharedDeps: SharedModule

var sharedDeps: SharedModule
    get() = _sharedDeps
    set(value) {
        _sharedDeps = value
        value.init()
    }

abstract class SharedModule {
    abstract val context: Context

    internal fun init() {
        // For added network security
        GMSSecurityProvider.installAsync(context)

        // Initialize logger
        Logger.init(context)
    }

    open val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(SettingsManager.READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .connectTimeout(SettingsManager.CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .cache(Cache(File(context.cacheDir, "okhttp3"), 50L * 1024 * 1024))
            .addNetworkInterceptor(CacheInterceptor())
            .build()
    }

    /**
     * Manages WeatherIcon providers
     */
    public val weatherIconsManager by lazy { WeatherIconsManager() }
}