package com.thewizrd.shared_resources

import android.annotation.SuppressLint
import android.content.Context
import com.thewizrd.shared_resources.icons.WeatherIconProvider
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.okhttp3.CacheInterceptor
import com.thewizrd.shared_resources.utils.SettingsManager
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class SimpleLibrary private constructor() {
    private var mApp: ApplicationLib? = null
    private var mAppContext: Context? = null
    private var client: OkHttpClient? = null

    private val mIconProviders: LinkedHashMap<String, WeatherIconProvider> = LinkedHashMap()

    init {
        // Register default icon providers
        resetIconProviders()
    }

    private constructor(app: ApplicationLib) : this() {
        this.mApp = app
        this.mAppContext = app.appContext
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        private var sSimpleLib: SimpleLibrary? = null

        @JvmStatic
        val instance: SimpleLibrary
            get() {
                if (sSimpleLib == null) {
                    sSimpleLib = SimpleLibrary()
                }

                return sSimpleLib!!
            }

        @JvmStatic
        fun initialize(app: ApplicationLib) {
            if (sSimpleLib == null) {
                sSimpleLib = SimpleLibrary(app)
            } else {
                sSimpleLib!!.mApp = app
                sSimpleLib!!.mAppContext = app.appContext
            }

            // For added network security
            GMSSecurityProvider.installAsync(app.appContext)
        }

        fun unregister() {
            sSimpleLib = null
        }
    }

    val app: ApplicationLib
        get() {
            return mApp!!
        }

    val appContext: Context
        get() {
            return mAppContext!!
        }

    val httpClient: OkHttpClient
        get() {
            if (client == null) {
                client = OkHttpClient.Builder()
                        .readTimeout(SettingsManager.READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                        .connectTimeout(SettingsManager.CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                        .followRedirects(true)
                        .retryOnConnectionFailure(true)
                        .cache(Cache(File(mAppContext!!.cacheDir, "okhttp3"), 50L * 1024 * 1024))
                        .addNetworkInterceptor(CacheInterceptor())
                        .build()
            }
            return client!!
        }

    fun registerIconProvider(provider: WeatherIconProvider) {
        if (!mIconProviders.containsKey(provider.key)) {
            mIconProviders[provider.key] = provider
        }
    }

    fun getIconProvider(key: String): WeatherIconProvider {
        var provider = mIconProviders[key]
        if (provider == null) {
            // Can't find the provider for this key; fallback to default/first available
            if (mIconProviders.size > 0) {
                provider = mIconProviders.values.firstOrNull()
            } else {
                registerIconProvider(WeatherIconsProvider().also { provider = it })
            }
        }
        return provider!!
    }

    val iconProviders: Map<String, WeatherIconProvider>
        get() = Collections.unmodifiableMap(mIconProviders)

    fun resetIconProviders() {
        mIconProviders.clear()
        mIconProviders.putAll(WeatherIconsManager.DEFAULT_ICONS)
    }
}