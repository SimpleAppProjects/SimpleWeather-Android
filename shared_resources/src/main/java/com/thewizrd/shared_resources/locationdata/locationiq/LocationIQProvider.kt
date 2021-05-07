package com.thewizrd.shared_resources.locationdata.locationiq

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
import com.thewizrd.shared_resources.utils.ExceptionUtils.copyStackTrace
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.LocationProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LocationIQProvider : LocationProviderImpl() {
    companion object {
        private const val AUTOCOMPLETE_QUERY_URL = "https://api.locationiq.com/v1/autocomplete.php?key=%s&q=%s&limit=10&normalizecity=1&addressdetails=1&accept-language=%s"
        private const val GEOLOCATION_QUERY_URL = "https://api.locationiq.com/v1/reverse.php?key=%s&lat=%s&lon=%s&format=json&zoom=14&namedetails=0&addressdetails=1&accept-language=%s&normalizecity=1"
        private const val KEY_QUERY_URL = "https://us1.unwiredlabs.com/v2/timezone.php?token=%s"
    }

    @LocationProviders
    override fun getLocationAPI(): String {
        return WeatherAPI.LOCATIONIQ
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override fun isKeyRequired(): Boolean {
        return false
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

        val client = SimpleLibrary.getInstance().httpClient
        var response: Response? = null
        var wEx: WeatherException? = null

        try {
            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .get()
                    .url(String.format(AUTOCOMPLETE_QUERY_URL, key, URLEncoder.encode(ac_query, "UTF-8"), locale))
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()
            val stream = response.getStream()

            // Load data
            locations = HashSet() // Use HashSet to avoid duplicate location (names)
            val arrListType = object : TypeToken<ArrayList<AutoCompleteQuery>>() {}.type
            val root = JSONParser.deserializer<List<AutoCompleteQuery>>(stream, arrListType)

            for (result in root) {
                // Filter: only store city results
                val added = if ("place" == result.jsonMemberClass)
                    locations.add(LocationQueryViewModel(result, weatherAPI))
                else
                    continue

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
            }
            Logger.writeLine(Log.ERROR, ex, "LocationIQProvider: error getting locations")
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
    override suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?
    ): LocationQueryViewModel = withContext(Dispatchers.IO) {
        var location = super.getLocation(coordinate, weatherAPI)

        if (location != null) {
            return@withContext location
        }

        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

        val key = getAPIKey()

        val client = SimpleLibrary.getInstance().httpClient
        var response: Response? = null
        var result: GeoLocation? = null
        var wEx: WeatherException? = null

        try {
            val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
            df.applyPattern("0.####")

            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .get()
                    .url(String.format(Locale.ROOT, GEOLOCATION_QUERY_URL, key, df.format(coordinate.latitude), df.format(coordinate.longitude), locale))
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()
            val stream = response.getStream()

            // Load data
            result = JSONParser.deserializer(stream, GeoLocation::class.java)

            // End Stream
            stream.closeQuietly()
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            }
            Logger.writeLine(Log.ERROR, ex, "LocationIQProvider: error getting location")
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null) throw wEx

        location = if (!result?.osmId.isNullOrBlank())
            LocationQueryViewModel(result, weatherAPI)
        else
            LocationQueryViewModel()

        return@withContext location
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(model: LocationQueryViewModel
    ): LocationQueryViewModel? {
        return null
    }

    @Throws(WeatherException::class)
    override suspend fun isKeyValid(key: String?): Boolean {
        if (key.isNullOrBlank()) {
            throw WeatherException(ErrorStatus.INVALIDAPIKEY)
        }

        var isValid = false
        var wEx: WeatherException? = null

        val client = SimpleLibrary.getInstance().httpClient
        var response: Response? = null

        try {
            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .url(String.format(KEY_QUERY_URL, key))
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()

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
            }

            isValid = false
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null && wEx.errorStatus != ErrorStatus.INVALIDAPIKEY) {
            throw wEx
        }

        return isValid
    }

    override fun getAPIKey(): String? {
        return Keys.getLocIQKey()
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return iso
    }
}