package com.thewizrd.shared_resources.utils.here

import android.content.Context
import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.keys.Keys
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.await
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.oauth.OAuthRequest
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object HEREOAuthUtils {
    const val HERE_OAUTH_URL = "https://account.api.here.com/oauth2/token"
    private const val KEY_TOKEN = "token"

    suspend fun getBearerToken(forceRefresh: Boolean): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val token = getTokenFromStorage()
            if (!token.isNullOrBlank())
                return@withContext token
        }

        val oAuthRequest = OAuthRequest(Keys.getHERECliID(), Keys.getHERECliSecr(),
                OAuthRequest.SignatureMethod.HMAC_SHA256, OAuthRequest.HTTPRequestType.POST)

        val client = SimpleLibrary.instance.httpClient
        var response: Response? = null

        try {
            val authorization = oAuthRequest.getAuthorizationHeader(HERE_OAUTH_URL, true)

            val request = Request.Builder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .url(HERE_OAUTH_URL)
                    .addHeader("Authorization", authorization)
                    .post(FormBody.Builder().addEncoded("grant_type", "client_credentials").build())
                    .build()

            response = client.newCall(request).await()

            val stream = response.getStream()
            val dateField = response.header("Date", null)
            val date = ZonedDateTime.parse(dateField, DateTimeFormatter.RFC_1123_DATE_TIME)

            val tokenRoot = JSONParser.deserializer<TokenRootobject>(stream, TokenRootobject::class.java)

            if (tokenRoot != null) {
                val tokenStr = String.format(Locale.ROOT, "Bearer %s", tokenRoot.accessToken)

                // Store token for future operations
                val token = Token().apply {
                    expirationDate = date.plusSeconds(tokenRoot.expiresIn.toLong())
                    access_token = tokenStr
                }

                storeToken(token)

                return@withContext tokenStr
            }
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e, "HEREOAuthUtils: Error retrieving token")
        } finally {
            response?.closeQuietly()
        }

        return@withContext null
    }

    private suspend fun getTokenFromStorage(): String? {
        val context = SimpleLibrary.instance.appContext
        val prefs = context.getSharedPreferences(WeatherAPI.HERE, Context.MODE_PRIVATE)

        if (prefs.contains(KEY_TOKEN)) {
            val tokenJSON = prefs.getString(KEY_TOKEN, null)
            if (tokenJSON != null) {
                val token = withContext(Dispatchers.Default) {
                    JSONParser.deserializer<Token>(tokenJSON, Token::class.java)
                }

                if (token != null && token.expirationDate.plusSeconds(-90).isAfter(ZonedDateTime.now(ZoneOffset.UTC))) {
                    return token.access_token
                }
            }
        }

        return null
    }

    private fun storeToken(token: Token) {
        // Shared Settings
        val context = SimpleLibrary.instance.appContext
        val prefs = context.getSharedPreferences(WeatherAPI.HERE, Context.MODE_PRIVATE)

        prefs.edit()
                .putString(KEY_TOKEN, JSONParser.serializer(token, Token::class.java))
                .apply()
    }
}