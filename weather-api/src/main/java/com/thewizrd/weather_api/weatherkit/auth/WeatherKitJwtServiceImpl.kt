package com.thewizrd.weather_api.weatherkit.auth

import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.StringUtils.isNullOrWhitespace
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.BuildConfig
import com.thewizrd.weather_api.keys.WeatherKitConfig
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

class WeatherKitJwtServiceImpl : WeatherKitJwtService {
    companion object {
        private const val KEY_TOKEN = "token"
    }

    private val context
        get() = sharedDeps.context

    override suspend fun getBearerToken(forceRefresh: Boolean): String? =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val token = getTokenFromStorage()
                if (!token.isNullOrBlank())
                    return@withContext token
            }

            return@withContext try {
                val token = generateBearerToken()
                storeToken(token)
                token
            } catch (e: Exception) {
                Logger.writeLine(Log.ERROR, e, "WeatherKitJwtService: Error retrieving token")
                null
            }
        }

    private fun getTokenFromStorage(): String? {
        val prefs = context.getSharedPreferences(WeatherAPI.APPLE, Context.MODE_PRIVATE)

        val jwts = Jwts.parserBuilder()
            .setClock {
                // Time travel to check expiration to avoid any auth issues
                Date(Instant.now().plusSeconds(90).toEpochMilli())
            }.build()

        if (prefs.contains(KEY_TOKEN)) {
            val token = prefs.getString(KEY_TOKEN, null)

            val jwtToken = runCatching {
                // Workaround: Read unsigned token
                val split = token!!.split('.')
                jwts.parseClaimsJws("${split[0]}.${split[1]}.")
            }.onFailure {
                if (BuildConfig.DEBUG) {
                    Log.d("WeatherKitJwtService", "Error reading token", it)
                }
            }.getOrNull()

            if (!token.isNullOrWhitespace() && jwtToken != null) {
                return token
            }
        }

        return null
    }

    private fun storeToken(token: String) {
        // Shared Settings
        val prefs = context.getSharedPreferences(WeatherAPI.APPLE, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_TOKEN, token)
        }
    }

    private fun generateBearerToken(): String {
        val iat = Date()
        val exp = Date(iat.time + TimeUnit.MINUTES.toMillis(60))

        val privKeyBytes = Base64.decode(WeatherKitConfig.getPrivateKey(), Base64.DEFAULT)
        val privKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        } else {
            KeyFactory.getInstance("EC")
        }.generatePrivate(PKCS8EncodedKeySpec(privKeyBytes))

        return Jwts.builder()
            .signWith(privKey, SignatureAlgorithm.ES256)
            .setHeaderParam(JwsHeader.KEY_ID, WeatherKitConfig.getKeyID())
            .setHeaderParam(
                "id",
                "${WeatherKitConfig.getTeamID()}.${WeatherKitConfig.getServiceID()}"
            )
            .setIssuer(WeatherKitConfig.getTeamID())
            .setIssuedAt(iat)
            .setExpiration(exp)
            .setSubject(WeatherKitConfig.getServiceID())
            .compact()
    }
}