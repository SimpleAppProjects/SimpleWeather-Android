package com.thewizrd.weather_api.aqicn

import android.util.Log
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.AirQualityProvider
import com.thewizrd.weather_api.keys.Keys
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.RateLimitedRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AQICNProvider : AirQualityProvider, RateLimitedRequest {
    companion object {
        private const val QUERY_URL = "https://api.waqi.info/feed/geo:%s;%s/?token=%s"
        private const val API_ID = "waqi"
    }

    override fun getRetryTime(): Long {
        return 1000
    }

    override suspend fun getAirQualityData(location: LocationData): AQICNData? =
        withContext(Dispatchers.IO) {
            var aqiData: AQICNData? = null

            val key = Keys.getAQICNKey()

            if (key.isNullOrBlank()) return@withContext null

            val client = sharedDeps.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit(API_ID)

                val context = sharedDeps.context
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val version = String.format("v%s", packageInfo.versionName)

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.####")

                val request = Request.Builder()
                    .cacheControl(
                        CacheControl.Builder()
                            .maxAge(1, TimeUnit.HOURS)
                            .build()
                    )
                    .url(
                        String.format(
                            Locale.ROOT,
                            QUERY_URL,
                            df.format(location.latitude),
                            df.format(location.longitude),
                            key
                        )
                    )
                    .addHeader(
                        "User-Agent",
                        String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version)
                    )
                    .build()

                // Connect to webstream
                response = client.newCall(request).await()
                response.checkForErrors(API_ID)

                val stream = response.getStream()

                // Load data
                val root = JSONParser.deserializer<Rootobject>(stream, Rootobject::class.java)

                root?.let {
                    aqiData = AQICNData(it)
                }

                // End Stream
                stream.closeQuietly()
            } catch (ex: Exception) {
                aqiData = null
                Logger.writeLine(Log.ERROR, ex, "AQICNProvider: error getting air quality data")
            } finally {
                response?.closeQuietly()
            }

            return@withContext aqiData
        }
}