package com.thewizrd.shared_resources.locationdata.openweather

import android.net.Uri
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.preferences.DevSettingsEnabler
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkForErrors
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.shared_resources.utils.APIRequestUtils.throwIfRateLimited
import com.thewizrd.shared_resources.utils.ExceptionUtils.copyStackTrace
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet

class OpenWeatherMapLocationProvider : LocationProviderImpl() {
    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        private const val KEYCHECK_QUERY_URL = BASE_URL + "forecast?appid=%s"
        private const val DIRECT_BASE_URL = "https://api.openweathermap.org/geo/1.0/direct"
        private const val GEOCODER_BASE_URL = "https://api.openweathermap.org/geo/1.0/reverse"
    }

    override fun getLocationAPI(): String {
        return WeatherAPI.OPENWEATHERMAP
    }

    override fun isKeyRequired(): Boolean {
        return true
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override suspend fun getLocations(
        ac_query: String?,
        weatherAPI: String?
    ): Collection<LocationQueryViewModel> = withContext(Dispatchers.IO) {
        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null
        var locations: Collection<LocationQueryViewModel>? = null
        var wEx: WeatherException? = null

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val settingsMgr = SimpleLibrary.instance.app.settingsManager
            val key =
                (if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKEY() else getAPIKey())
                    ?: DevSettingsEnabler.getAPIKey(
                        SimpleLibrary.instance.appContext,
                        WeatherAPI.OPENWEATHERMAP
                    )

            if (key.isNullOrBlank()) {
                throw WeatherException(ErrorStatus.INVALIDAPIKEY)
            }

            val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
            df.applyPattern("0.##")

            // http://api.openweathermap.org/geo/1.0/direct?q={city name},{state code},{country code}&limit={limit}&appid={API key}
            val requestUri = Uri.parse(DIRECT_BASE_URL).buildUpon()
                .appendQueryParameter("q", "$ac_query")
                .appendQueryParameter("limit", "10")
                .appendQueryParameter("appid", key)
                .build()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(14, TimeUnit.DAYS)
                        .build()
                )
                .url(requestUri.toString())
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            checkForErrors(response)

            val stream = response.getStream()

            // Load data
            val arrListType = object : TypeToken<ArrayList<ResponseItem>>() {}.type
            val root = JSONParser.deserializer<List<ResponseItem>>(stream, arrListType)

            locations = HashSet()

            for (result in root) {
                locations.add(createLocationModel(result, uLocale.language, weatherAPI!!))
            }

            // End Stream
            stream.closeQuietly()
        } catch (ex: Exception) {
            locations = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(
                Log.ERROR,
                ex,
                "OpenWeatherMapLocationProvider: error getting location"
            )
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null) throw wEx

        if (locations.isNullOrEmpty()) {
            locations = listOf(LocationQueryViewModel())
        }

        return@withContext locations
    }

    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel? {
        return null
    }

    override suspend fun getLocation(
        coordinate: Coordinate,
        weatherAPI: String?
    ): LocationQueryViewModel? = withContext(Dispatchers.IO) {
        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null
        var result: ResponseItem? = null
        var wEx: WeatherException? = null

        var location: LocationQueryViewModel?

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val settingsMgr = SimpleLibrary.instance.app.settingsManager
            val key =
                (if (settingsMgr.usePersonalKey()) settingsMgr.getAPIKEY() else getAPIKey())
                    ?: DevSettingsEnabler.getAPIKey(
                        SimpleLibrary.instance.appContext,
                        WeatherAPI.OPENWEATHERMAP
                    )

            if (key.isNullOrBlank()) {
                throw WeatherException(ErrorStatus.INVALIDAPIKEY)
            }

            val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
            df.applyPattern("0.##")

            // http://api.openweathermap.org/geo/1.0/reverse?lat={lat}&lon={lon}&limit={limit}&appid={API key}
            val requestUri = Uri.parse(GEOCODER_BASE_URL).buildUpon()
                .appendQueryParameter("lat", df.format(coordinate.latitude))
                .appendQueryParameter("lon", df.format(coordinate.longitude))
                .appendQueryParameter("limit", "1")
                .appendQueryParameter("appid", key)
                .build()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(14, TimeUnit.DAYS)
                        .build()
                )
                .url(requestUri.toString())
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            checkForErrors(response)

            val stream = response.getStream()

            // Load data
            val arrListType = object : TypeToken<ArrayList<ResponseItem>>() {}.type
            val root = JSONParser.deserializer<List<ResponseItem>>(stream, arrListType)

            result = root.first()

            // End Stream
            stream.closeQuietly()
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is IllegalArgumentException) {
                wEx = WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            Logger.writeLine(
                Log.ERROR,
                ex,
                "OpenWeatherMapLocationProvider: error getting location"
            )
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null) throw wEx

        location = if (result?.lat != null && result.lon != null) {
            createLocationModel(result, uLocale.language, weatherAPI!!)
        } else {
            LocationQueryViewModel()
        }

        return@withContext location
    }

    @Throws(WeatherException::class)
    override suspend fun isKeyValid(key: String?): Boolean = withContext(Dispatchers.IO) {
        if (key.isNullOrBlank()) {
            throw WeatherException(ErrorStatus.INVALIDAPIKEY)
        }

        var isValid = false
        var wEx: WeatherException? = null

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(1, TimeUnit.DAYS)
                        .build()
                )
                .url(String.format(KEYCHECK_QUERY_URL, key))
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            throwIfRateLimited(response)

            when (response.code) {
                HttpURLConnection.HTTP_BAD_REQUEST -> isValid = true
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    wEx = WeatherException(ErrorStatus.INVALIDAPIKEY)
                    isValid = false
                }
            }
        } catch (ex: Exception) {
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR).copyStackTrace(ex)
            } else if (ex is WeatherException) {
                wEx = ex
            }

            isValid = false
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null && wEx.errorStatus != ErrorStatus.INVALIDAPIKEY) {
            throw wEx
        }

        return@withContext isValid
    }

    override fun getAPIKey(): String? {
        return Keys.getOWMKey()
    }
}