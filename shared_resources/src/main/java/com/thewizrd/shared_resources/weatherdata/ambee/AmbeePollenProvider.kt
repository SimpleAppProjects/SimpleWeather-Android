package com.thewizrd.shared_resources.weatherdata.ambee

import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkForErrors
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.shared_resources.utils.IRateLimitedRequest
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.PollenProviderInterface
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.Pollen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AmbeePollenProvider : PollenProviderInterface, IRateLimitedRequest {
    companion object {
        private const val QUERY_URL =
            "https://api.ambeedata.com/latest/pollen/by-lat-lng?lat=%s&lng=%s"
    }

    override fun getRetryTime(): Long {
        return 43200000L // 12 hrs
    }

    override suspend fun getPollenData(location: LocationData): Pollen? =
        withContext(Dispatchers.IO) {
            var pollenData: Pollen? = null

            val settingsMgr = SimpleLibrary.instance.app.settingsManager
            val key = settingsMgr.getAPIKey(WeatherAPI.AMBEE) ?: Keys.getAmbeeKey()

            if (key.isNullOrBlank()) return@withContext null

            val client = SimpleLibrary.instance.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit(WeatherAPI.AMBEE)

                    val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                    df.applyPattern("0.####")

                    val request = Request.Builder()
                        .cacheControl(
                            CacheControl.Builder()
                                .maxAge(6, TimeUnit.HOURS)
                                .build()
                        )
                        .url(
                            String.format(
                                Locale.ROOT,
                                QUERY_URL,
                                df.format(location.latitude),
                                df.format(location.longitude)
                            )
                        )
                        .addHeader("x-api-key", key)
                            .addHeader("Content-type", "application/json")
                            .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    response.checkForErrors(WeatherAPI.AMBEE)

                    val stream = response.getStream()

                    // Load data
                    val root = JSONParser.deserializer<Rootobject>(stream, Rootobject::class.java)

                    pollenData = Pollen().apply {
                        treePollenCount = when (root.data[0].risk.treePollen) {
                            "Low" -> Pollen.PollenCount.LOW
                            "Moderate" -> Pollen.PollenCount.MODERATE
                            "High" -> Pollen.PollenCount.HIGH
                            "Very High" -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }
                        grassPollenCount = when (root.data[0].risk.grassPollen) {
                            "Low" -> Pollen.PollenCount.LOW
                            "Moderate" -> Pollen.PollenCount.MODERATE
                            "High" -> Pollen.PollenCount.HIGH
                            "Very High" -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }
                        ragweedPollenCount = when (root.data[0].risk.weedPollen) {
                            "Low" -> Pollen.PollenCount.LOW
                            "Moderate" -> Pollen.PollenCount.MODERATE
                            "High" -> Pollen.PollenCount.HIGH
                            "Very High" -> Pollen.PollenCount.VERY_HIGH
                            else -> Pollen.PollenCount.UNKNOWN
                        }
                    }

                    // End Stream
                    stream.closeQuietly()
                } catch (ex: Exception) {
                    pollenData = null
                    Logger.writeLine(Log.ERROR, ex, "AmbeePollenProvider: error getting air quality data")
                } finally {
                    response?.closeQuietly()
                }

                return@withContext pollenData
            }
}