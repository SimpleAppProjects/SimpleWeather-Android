package com.thewizrd.shared_resources.tzdb

import android.net.Uri
import android.util.Log
import com.google.gson.annotations.SerializedName
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.firebase.FirebaseHelper
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkForErrors
import com.thewizrd.shared_resources.utils.APIRequestUtils.checkRateLimit
import com.thewizrd.shared_resources.utils.IRateLimitedRequest
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TimeZoneProvider : TimeZoneProviderInterface, IRateLimitedRequest {
    companion object {
        private const val API_ID = "tzdb"
    }

    private inner class TimeZoneData {
        @SerializedName("tz_long")
        var tzLong: String? = null
    }

    override fun getRetryTime(): Long {
        return 60000
    }

    override suspend fun getTimeZone(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            // Get Firebase token
            val userToken = FirebaseHelper.getAccessToken()

            val tzAPI = Keys.getTimeZoneAPI()
            if (tzAPI.isNullOrBlank() || userToken.isNullOrBlank())
                return@withContext null

            var tzLong: String?
            val client = SimpleLibrary.instance.httpClient
                var response: Response? = null

                try {
                    // If were under rate limit, deny request
                    checkRateLimit(API_ID)

                    val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
                    df.applyPattern("0.####")

                    val requestUri = Uri.parse(tzAPI).buildUpon()
                        .appendQueryParameter("lat", df.format(latitude))
                        .appendQueryParameter("lon", df.format(longitude))
                        .build()

                    val request = Request.Builder()
                        .cacheControl(
                            CacheControl.Builder()
                                .maxAge(1, TimeUnit.DAYS)
                                .build()
                        )
                        .url(requestUri.toString())
                        .header("Authorization", "Bearer $userToken")
                        .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    checkForErrors(API_ID, response)

                    val stream = response.getStream()

                    // Load data
                    val root =
                        JSONParser.deserializer<TimeZoneData>(stream, TimeZoneData::class.java)
                    tzLong = root.tzLong

                    // End Stream
                    stream.closeQuietly()
                } catch (ex: Exception) {
                    tzLong = null
                    Logger.writeLine(Log.ERROR, ex, "TimeZoneProvider: error time zone")
                } finally {
                    response?.closeQuietly()
                }

                return@withContext tzLong
            }
}