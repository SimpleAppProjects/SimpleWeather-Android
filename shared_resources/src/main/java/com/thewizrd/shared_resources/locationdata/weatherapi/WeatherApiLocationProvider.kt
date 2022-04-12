package com.thewizrd.shared_resources.locationdata.weatherapi

import android.util.Log
import com.google.gson.reflect.TypeToken
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkForErrors
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class WeatherApiLocationProvider : LocationProviderImpl() {
    companion object {
        private const val QUERY_URL = "https://api.weatherapi.com/v1/search.json?key=%s&q=%s&lang=%s"
    }

    @LocationProviders
    override fun getLocationAPI(): String {
        return WeatherAPI.WEATHERAPI
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override fun needsLocationFromName(): Boolean {
        return true
    }

    @Throws(WeatherException::class)
    override suspend fun getLocations(ac_query: String?, weatherAPI: String?
    ): Collection<LocationQueryViewModel> = withContext(Dispatchers.IO) {
        var locations: Collection<LocationQueryViewModel>? = null

        // Limit amount of results shown
        var maxResults = 10

        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

        val key = getAPIKey()

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null
        var wEx: WeatherException? = null

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(1, TimeUnit.DAYS)
                        .build()
                )
                .get()
                .url(String.format(QUERY_URL, key, URLEncoder.encode(ac_query, "UTF-8"), locale))
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            checkForErrors(response)

            val stream = response.getStream()

            // Load data
            locations = HashSet() // Use HashSet to avoid duplicate location (names)
            val arrListType = object : TypeToken<ArrayList<LocationItem>>() {}.type
            val root = JSONParser.deserializer<List<LocationItem>>(stream, arrListType)

            for (result in root) {
                val added = locations.add(createLocationModel(result, weatherAPI!!))

                // Limit amount of results
                if (added) {
                    maxResults--
                    if (maxResults <= 0) break
                }
            }

            // End Stream
            stream.closeQuietly()
        } catch (ex: Exception) {
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is WeatherException) {
                wEx = ex
            }
            Logger.writeLine(Log.ERROR, ex, "WeatherApiLocationProvider: error getting locations")
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null) throw wEx

        if (locations.isNullOrEmpty()) {
            locations = listOf(LocationQueryViewModel())
        }

        return@withContext locations
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(model: LocationQueryViewModel
    ): LocationQueryViewModel? {
        return super.getLocationFromName(model)
    }

    @Throws(WeatherException::class)
    override suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?
    ): LocationQueryViewModel = withContext(Dispatchers.IO) {
        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

        val key = getAPIKey()

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null
        var result: LocationItem? = null
        var wEx: WeatherException? = null

        try {
            // If were under rate limit, deny request
            checkRateLimit()

            val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
            df.applyPattern("0.##")

            val request = Request.Builder()
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(1, TimeUnit.DAYS)
                        .build()
                )
                .get()
                .url(
                    String.format(
                        Locale.ROOT,
                        QUERY_URL,
                        key,
                        String.format(
                            "%s,%s",
                            df.format(coordinate.latitude),
                            df.format(coordinate.longitude)
                        ),
                        locale
                    )
                )
                .build()

            // Connect to webstream
            response = client.newCall(request).await()
            checkForErrors(response)

            val stream = response.getStream()

            // Load data
            val arrListType = object : TypeToken<ArrayList<LocationItem>>() {}.type
            val locations = JSONParser.deserializer<List<LocationItem>>(stream, arrListType)

            for (item in locations) {
                if (abs(
                        ConversionMethods.calculateHaversine(
                            coordinate.latitude,
                            coordinate.longitude,
                            item.lat,
                            item.lon
                        )
                    ) <= 100
                ) {
                    result = item
                    break
                }
            }

            if (result == null) {
                result = locations[0]
            }

            // End Stream
            stream.closeQuietly()
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            } else if (ex is WeatherException) {
                wEx = ex
            }
            Logger.writeLine(Log.ERROR, ex, "LocationIQProvider: error getting location")
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null) throw wEx

        return@withContext result?.let { createLocationModel(it, weatherAPI!!) }
                ?: LocationQueryViewModel()
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return Keys.getWeatherApiKey()
    }

    override fun localeToLangCode(iso: String, name: String): String {
        var code = "en"

        code = when (iso) {
            // Chinese
            "zh" -> when (name) {
                // Chinese - Traditional
                "zh-Hant", "zh-HK", "zh-MO", "zh-TW" -> "zh_tw"
                // Mandarin
                "zh-cmn" -> "zh_cmn"
                // Wu
                "zh-wuu" -> "zh_wuu"
                // Xiang
                "zh-hsn" -> "zh_hsn"
                // Cantonese
                "zh-yue" -> "zh_yue"
                // Chinese - Simplified
                else -> "zh"
            }
            else -> iso
        }
        return code
    }
}