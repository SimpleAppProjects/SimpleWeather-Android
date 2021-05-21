package com.thewizrd.shared_resources.tzdb

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.firebase.FirebaseHelper
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.util.*
import java.util.concurrent.TimeUnit

class TimeZoneProvider : TimeZoneProviderInterface {
    private inner class TimeZoneData {
        @SerializedName("tz_long")
        var tzLong: String? = null
    }

    override suspend fun getTimeZone(latitude: Double, longitude: Double): String? =
            withContext(Dispatchers.IO) {
                // Get Firebase token
                val userToken = FirebaseHelper.getAccessToken()

                val tzAPI = Keys.getTimeZoneAPI()
                if (tzAPI.isNullOrBlank() || userToken.isNullOrBlank())
                    return@withContext null

                var tzLong: String? = null
                val client = SimpleLibrary.instance.httpClient
                var response: Response? = null

                try {
                    val request = Request.Builder()
                            .cacheControl(CacheControl.Builder()
                                    .maxAge(1, TimeUnit.DAYS)
                                    .build())
                            .url(String.format(Locale.ROOT, "%s?lat=%s&lon=%s", tzAPI, latitude, longitude))
                            .addHeader("Authorization", String.format(Locale.ROOT, "Bearer %s", userToken))
                            .build()

                    // Connect to webstream
                    response = client.newCall(request).await()
                    val stream = response.getStream()

                    // Load data
                    val root = JSONParser.deserializer<TimeZoneData>(stream, TimeZoneData::class.java)
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