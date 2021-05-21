package com.thewizrd.shared_resources.weatherdata.aqicn

import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.AirQualityProviderInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AQICNProvider : AirQualityProviderInterface {
    companion object {
        private const val QUERY_URL = "https://api.waqi.info/feed/geo:%s;%s/?token=%s"
        private const val MAX_ATTEMPTS = 2
    }

    override suspend fun getAirQualityData(location: LocationData): AQICNData? =
            withContext(Dispatchers.IO) {
                var aqiData: AQICNData? = null

                val key = Keys.getAQICNKey()

                if (key.isNullOrBlank()) return@withContext null

                val client = SimpleLibrary.instance.httpClient
                var response: Response? = null

                try {
                    val context = SimpleLibrary.instance.appContext
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val version = String.format("v%s", packageInfo.versionName)

                    val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                    df.applyPattern("0.####")

                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.HOURS)
                                    .build())
                            .url(String.format(Locale.ROOT, QUERY_URL, df.format(location.latitude), df.format(location.longitude), key))
                            .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                            .build()

                    for (i in 0 until MAX_ATTEMPTS) {
                        try {
                            // Connect to webstream
                            response = client.newCall(request).await()

                            if (response.code == HttpURLConnection.HTTP_BAD_REQUEST) {
                                break
                            } else {
                                val stream = response.getStream()

                                // Load data
                                val root = JSONParser.deserializer<Rootobject>(stream, Rootobject::class.java)

                                aqiData = AQICNData(root)

                                // End Stream
                                stream.closeQuietly()
                            }
                        } catch (_: Exception) {
                        }

                        if (i < MAX_ATTEMPTS - 1 && response == null) {
                            delay(1000)
                        }
                    }
                } catch (ex: Exception) {
                    aqiData = null
                    Logger.writeLine(Log.ERROR, ex, "AQICNProvider: error getting air quality data")
                } finally {
                    response?.closeQuietly()
                }

                return@withContext aqiData
            }
}