package com.thewizrd.weather_api.nws.alerts

import android.util.Log
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProvider
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.weather_api.utils.RateLimitedRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NWSAlertProvider : WeatherAlertProvider, RateLimitedRequest {
    companion object {
        private const val ALERT_QUERY_URL =
            "https://api.weather.gov/alerts/active?status=actual&message_type=alert&point=%s,%s"
    }

    override fun getRetryTime(): Long {
        return 30000
    }

    override suspend fun getAlerts(location: LocationData): Collection<WeatherAlert> =
        withContext(Dispatchers.IO) {
            var alerts: Collection<WeatherAlert>? = null

            val client = sharedDeps.httpClient
            var response: Response? = null

            try {
                // If were under rate limit, deny request
                checkRateLimit(WeatherAPI.NWS)

                val context = sharedDeps.context
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val version = String.format("v%s", packageInfo.versionName)

                val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                df.applyPattern("0.####")

                val request = Request.Builder()
                    .url(
                        String.format(
                            Locale.ROOT,
                            ALERT_QUERY_URL,
                            df.format(location.latitude),
                            df.format(location.longitude)
                        )
                    )
                    .addHeader("Accept", "application/ld+json")
                    .addHeader(
                        "User-Agent",
                        String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version)
                    )
                    .build()

                // Connect to webstream
                response = client.newBuilder()
                    // Extend timeout to 15s
                    .readTimeout(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
                    .newCall(request)
                    .await()

                response.checkForErrors(WeatherAPI.NWS)

                val stream = response.getStream()

                // Load data
                val root =
                    JSONParser.deserializer<AlertRootobject>(stream, AlertRootobject::class.java)

                requireNotNull(root)

                alerts = createWeatherAlerts(root)

                // End Stream
                stream.closeQuietly()
            } catch (ex: Exception) {
                Logger.writeLine(
                    Log.ERROR,
                    ex,
                    "NWSAlertProvider: error getting weather alert data"
                )
            } finally {
                response?.closeQuietly()
            }

            if (alerts == null) {
                alerts = emptyList()
            }

            return@withContext alerts
        }
}