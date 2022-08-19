package com.thewizrd.weather_api.accuweather.location

import android.net.Uri
import android.util.Log
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.google.location.AndroidLocationProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal class AccuWeatherLocationProvider : AndroidLocationProvider() {
    companion object {
        private const val BASE_URL =
            "https://dataservice.accuweather.com/locations/v1/cities/geoposition/search"
    }

    override fun getLocationAPI(): String {
        return WeatherAPI.ACCUWEATHER
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsLocale(): Boolean {
        return true
    }

    override fun needsLocationFromID(): Boolean {
        return true
    }

    override fun needsLocationFromName(): Boolean {
        return false
    }

    override fun needsLocationFromGeocoder(): Boolean {
        return false
    }

    override suspend fun getLocationFromID(model: LocationQuery): LocationQuery =
        withContext(Dispatchers.IO) {
            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val client = sharedDeps.httpClient
            var response: Response? = null
            var result: GeopositionResponse? = null
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val key =
                    if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(
                        getLocationAPI()
                    ) else getAPIKey()

                if (key.isNullOrBlank()) {
                    throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                }

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.##")

                val requestUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter("apikey", key)
                    .appendQueryParameter(
                        "q",
                        "${df.format(model.locationLat)},${df.format(model.locationLong)}"
                    )
                    .appendQueryParameter("language", locale)
                    .appendQueryParameter("details", "false")
                    .appendQueryParameter("toplevel", "false")
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
                result = JSONParser.deserializer<GeopositionResponse>(
                    stream,
                    GeopositionResponse::class.java
                )

                // End Stream
                stream.closeQuietly()
            } catch (ex: Exception) {
                result = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "AccuWeatherLocationProvider: error getting location"
                )
            } finally {
                response?.closeQuietly()
            }

            if (wEx != null) throw wEx

            return@withContext if (!result?.key.isNullOrBlank())
                createLocationModel(result!!, model)
            else
                LocationQuery()
        }

    override suspend fun getLocation(coordinate: Coordinate, weatherAPI: String?): LocationQuery =
        withContext(Dispatchers.IO) {
            val uLocale = ULocale.forLocale(LocaleUtils.getLocale())
            val locale = localeToLangCode(uLocale.language, uLocale.toLanguageTag())

            val client = sharedDeps.httpClient
            var response: Response? = null
            var result: GeopositionResponse?
            var wEx: WeatherException? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit()

                val key =
                    if (settingsManager.usePersonalKey()) settingsManager.getAPIKey(
                        getLocationAPI()
                    ) else getAPIKey()

                if (key.isNullOrBlank()) {
                    throw WeatherException(ErrorStatus.INVALIDAPIKEY)
                }

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.##")

                val requestUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter("apikey", key)
                    .appendQueryParameter(
                        "q",
                        "${df.format(coordinate.latitude)},${df.format(coordinate.longitude)}"
                    )
                    .appendQueryParameter("language", locale)
                    .appendQueryParameter("details", "false")
                    .appendQueryParameter("toplevel", "false")
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
                result = JSONParser.deserializer<GeopositionResponse>(
                    stream,
                    GeopositionResponse::class.java
                )

                // End Stream
                stream.closeQuietly()
            } catch (ex: Exception) {
                result = null
                if (ex is IOException) {
                    wEx = WeatherException(ErrorStatus.NETWORKERROR, ex)
                } else if (ex is WeatherException) {
                    wEx = ex
                }
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "AccuWeatherLocationProvider: error getting location"
                )
            } finally {
                response?.closeQuietly()
            }

            if (wEx != null) throw wEx

            return@withContext if (!result?.key.isNullOrBlank())
                createLocationModel(result!!)
            else
                LocationQuery()
        }

    override fun localeToLangCode(iso: String, name: String): String {
        return name
    }
}