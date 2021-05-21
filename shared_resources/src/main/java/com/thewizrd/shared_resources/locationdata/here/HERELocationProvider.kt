package com.thewizrd.shared_resources.locationdata.here

import android.util.Log
import androidx.annotation.RestrictTo
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils
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

class HERELocationProvider  // Keep hidden for now
@RestrictTo(RestrictTo.Scope.TESTS) private constructor() : LocationProviderImpl() {
    companion object {
        private const val AUTOCOMPLETE_QUERY_URL = "https://autocomplete.geocoder.ls.hereapi.com/6.2/suggest.json?query=%s&language=%s&maxresults=10"
        private const val GEOLOCATION_QUERY_URL = "https://reverse.geocoder.ls.hereapi.com/6.2/reversegeocode.json" +
                                                  "?prox=%s,150&mode=retrieveAreas&maxresults=1&additionaldata=Country2,true&gen=9&jsonattributes=1" +
                                                  "&locationattributes=adminInfo,timeZone,-mapView,-mapReference&language=%s"
        private const val GEOCODER_QUERY_URL = "https://geocoder.ls.hereapi.com/6.2/geocode.json" +
                                               "?locationid=%s&mode=retrieveAreas&maxresults=1&additionaldata=Country2,true&gen=9&jsonattributes=1" +
                                               "&locationattributes=adminInfo,timeZone,-mapView,-mapReference&language=%s"
    }

    @LocationProviders
    override fun getLocationAPI(): String {
        return WeatherAPI.HERE
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun needsLocationFromID(): Boolean {
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

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null
        var wEx: WeatherException? = null

        try {
            val authorization = HEREOAuthUtils.getBearerToken(false)

            if (authorization.isNullOrBlank()) {
                throw WeatherException(ErrorStatus.NETWORKERROR)
            }

            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .url(String.format(AUTOCOMPLETE_QUERY_URL, URLEncoder.encode(ac_query, "UTF-8"), locale))
                    .addHeader("Authorization", authorization)
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()
            val stream = response.getStream()

            // Load data
            locations = HashSet() // Use HashSet to avoid duplicate location (names)
            val root = JSONParser.deserializer<AutoCompleteQuery>(stream, AutoCompleteQuery::class.java)

            for (result in root.suggestions) {
                var added = false
                // Filter: only store city results
                added = if ("city" == result.matchLevel
                            || "district" == result.matchLevel
                            || "postalCode" == result.matchLevel) {
                    locations.add(createLocationModel(result!!, weatherAPI!!))
                } else {
                    continue
                }

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
            Logger.writeLine(Log.ERROR, ex, "HERELocationProvider: error getting locations")
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

        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")

        val location_query = String.format(Locale.ROOT, "%s,%s", df.format(coordinate.latitude), df.format(coordinate.longitude))

        val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null
        var result: ResultItem? = null
        var wEx: WeatherException? = null

        try {
            val authorization = HEREOAuthUtils.getBearerToken(false)

            if (authorization.isNullOrBlank()) {
                throw WeatherException(ErrorStatus.NETWORKERROR)
            }

            val request = Request.Builder()
                    .cacheControl(CacheControl.Builder()
                            .maxAge(1, TimeUnit.DAYS)
                            .build())
                    .url(String.format(GEOLOCATION_QUERY_URL, location_query, locale))
                    .addHeader("Authorization", authorization)
                    .build()

            // Connect to webstream
            response = client.newCall(request).await()
            val stream = response.getStream()

            // Load data
            val root = JSONParser.deserializer<Geo_Rootobject>(stream, Geo_Rootobject::class.java)

            if (!root.response.view.isNullOrEmpty() && !root.response.view[0]?.result.isNullOrEmpty())
                result = root.response.view[0].result[0]

            // End Stream
            stream.closeQuietly()
        } catch (ex: Exception) {
            result = null
            if (ex is IOException) {
                wEx = WeatherException(ErrorStatus.NETWORKERROR)
            }
            Logger.writeLine(Log.ERROR, ex, "HERELocationProvider: error getting location")
        } finally {
            response?.closeQuietly()
        }

        if (wEx != null) throw wEx

        location = if (!result?.location?.locationId.isNullOrBlank())
            createLocationModel(result!!, weatherAPI!!)
        else
            LocationQueryViewModel()

        return@withContext location
    }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel =
            withContext(Dispatchers.IO) {
                val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
                val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

                val client = SimpleLibrary.instance.httpClient
                var response: Response? = null
                var result: ResultItem? = null
                var wEx: WeatherException? = null

                try {
                    val authorization = HEREOAuthUtils.getBearerToken(false)

                    if (authorization.isNullOrBlank()) {
                        throw WeatherException(ErrorStatus.NETWORKERROR)
                    }

                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.DAYS)
                                    .build())
                            .url(String.format(GEOCODER_QUERY_URL, model.locationQuery, locale))
                            .addHeader("Authorization", authorization)
                            .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    val stream = response.getStream()

                    // Load data
                    val root = JSONParser.deserializer<Geo_Rootobject>(stream, Geo_Rootobject::class.java)

                    if (!root.response.view.isNullOrEmpty() && !root.response.view[0]?.result.isNullOrEmpty())
                        result = root.response.view[0].result[0]

                    // End Stream
                    stream.closeQuietly()
                } catch (ex: Exception) {
                    result = null
                    if (ex is IOException) {
                        wEx = WeatherException(ErrorStatus.NETWORKERROR)
                    }
                    Logger.writeLine(Log.ERROR, ex, "HERELocationProvider: error getting location")
                } finally {
                    response?.closeQuietly()
                }

                if (wEx != null) throw wEx

                return@withContext if (!result?.location?.locationId.isNullOrBlank())
                    createLocationModel(result!!, model.weatherSource!!)
                else
                    LocationQueryViewModel()
            }

    @Throws(WeatherException::class)
    override suspend fun getLocationFromName(model: LocationQueryViewModel
    ): LocationQueryViewModel? {
        return null
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }

    override fun localeToLangCode(iso: String, name: String): String {
        return name
    }
}