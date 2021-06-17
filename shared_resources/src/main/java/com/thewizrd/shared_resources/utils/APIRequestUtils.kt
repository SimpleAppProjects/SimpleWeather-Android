package com.thewizrd.shared_resources.utils

import androidx.core.content.edit
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl
import java.net.HttpURLConnection
import kotlin.random.Random

object APIRequestUtils {
    /**
     * Checks if response was successful; if it was not, throw the appropriate WeatherException
     */
    @Throws(WeatherException::class)
    fun checkForErrors(apiID: String, responseCode: Int, retryTimeInMs: Long = 60000) {
        // Check for errors
        when (responseCode) {
            HttpURLConnection.HTTP_OK -> {
            }
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                throw WeatherException(ErrorStatus.NOWEATHER)
            }
            HttpURLConnection.HTTP_NOT_FOUND -> {
                throw WeatherException(ErrorStatus.QUERYNOTFOUND)
            }
            429 /* Too Many Requests */ -> {
                throwIfRateLimited(apiID, responseCode, retryTimeInMs)
            }
            HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                throw WeatherException(ErrorStatus.UNKNOWN)
            }
            else -> {
                throw WeatherException(ErrorStatus.NOWEATHER)
            }
        }
    }

    fun WeatherProviderImpl.checkForErrors(responseCode: Int) {
        checkForErrors(this.getWeatherAPI(), responseCode, this.getRetryTime())
    }

    fun LocationProviderImpl.checkForErrors(responseCode: Int) {
        checkForErrors(this.getLocationAPI(), responseCode, this.getRetryTime())
    }

    fun IRateLimitedRequest.checkForErrors(apiID: String, responseCode: Int) {
        checkForErrors(apiID, responseCode, this.getRetryTime())
    }

    fun throwIfRateLimited(apiID: String, responseCode: Int, retryTimeInMs: Long = 60000) {
        if (responseCode == 429 /* Too Many Requests */) {
            setNextRetryTime(apiID, retryTimeInMs)
            throw WeatherException(ErrorStatus.NETWORKERROR)
        }
    }

    fun WeatherProviderImpl.throwIfRateLimited(responseCode: Int) {
        throwIfRateLimited(this.getWeatherAPI(), responseCode, this.getRetryTime())
    }

    fun LocationProviderImpl.throwIfRateLimited(responseCode: Int) {
        throwIfRateLimited(this.getLocationAPI(), responseCode, this.getRetryTime())
    }

    fun IRateLimitedRequest.throwIfRateLimited(apiID: String, responseCode: Int) {
        throwIfRateLimited(apiID, responseCode, this.getRetryTime())
    }

    /**
     * Check if API has been rate limited (HTTP Error 429 occurred recently)
     * If so, deny API request until time passes
     *
     * @throws WeatherException Exception if client is under rate limit
     */
    @Throws(WeatherException::class)
    fun checkRateLimit(apiID: String) {
        val currentTime = System.currentTimeMillis()
        val nextRetryTime = getNextRetryTime(apiID)

        if (currentTime < nextRetryTime) {
            throw WeatherException(ErrorStatus.NETWORKERROR)
        }
    }

    /**
     * Check if API has been rate limited (HTTP Error 429 occurred recently)
     * If so, deny API request until time passes
     *
     * @throws WeatherException Exception if client is under rate limit
     */
    @Throws(WeatherException::class)
    fun WeatherProviderImpl.checkRateLimit() {
        checkRateLimit(this.getWeatherAPI())
    }

    /**
     * Check if API has been rate limited (HTTP Error 429 occurred recently)
     * If so, deny API request until time passes
     *
     * @throws WeatherException Exception if client is under rate limit
     */
    fun LocationProviderImpl.checkRateLimit() {
        checkRateLimit(this.getLocationAPI())
    }

    private val KEY_NEXTRETRYTIME = "key_nextretrytime"

    private fun getRetryTimePrefKey(apiID: String): String {
        return "${apiID}:${KEY_NEXTRETRYTIME}"
    }

    private fun getNextRetryTime(apiID: String): Long {
        val preferences = SimpleLibrary.instance.app.preferences
        return preferences.getLong(getRetryTimePrefKey(apiID), -1)
    }

    private fun setNextRetryTime(apiID: String, retryTimeInMs: Long) {
        val preferences = SimpleLibrary.instance.app.preferences
        preferences.edit(true) {
            putLong(
                getRetryTimePrefKey(apiID),
                System.currentTimeMillis() + (retryTimeInMs + getRandomOffset(retryTimeInMs))
            )
        }
    }

    /**
     * Returns random number of milliseconds as the delay offset based on given retry time
     *
     *
     * If time >= 12 hours returns random number of minutes between (1 - 60min)
     *
     * If time >= 1 hours returns random number of minutes between (1 - 30min)
     *
     * If time >= 1 minute returns random number of minutes between (1 - 5min)
     *
     * If time >= 30s returns random number of seconds between (5 - 15s)
     *
     * If time >= 1s returns random number of seconds between (500 - 5000ms)
     */
    private fun getRandomOffset(retryTimeInMs: Long): Long {
        return when {
            /* 12 hrs */
            retryTimeInMs >= 43200000L -> {
                Random.Default.nextInt(1, 60 + 1) * 60 * 1000L
            }
            /* 1 hrs */
            retryTimeInMs >= 3600000L -> {
                Random.Default.nextInt(1, 30 + 1) * 60 * 1000L
            }
            /* 1 minute */
            retryTimeInMs >= 60000L -> {
                Random.Default.nextInt(1, 5 + 1) * 60 * 1000L
            }
            /* 30 seconds */
            retryTimeInMs >= 30000L -> {
                Random.Default.nextInt(5, 15 + 1) * 1000L
            }
            else -> {
                Random.Default.nextLong(500, 5000 + 1)
            }
        }
    }
}

interface IRateLimitedRequest {
    /**
     * Time in milliseconds to wait until next request
     * (ex. 60 calls / per minute -> 60s wait time)
     *
     * @return retry time in milliseconds
     */
    fun getRetryTime(): Long
}