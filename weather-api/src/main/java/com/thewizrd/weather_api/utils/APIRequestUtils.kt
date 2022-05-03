package com.thewizrd.weather_api.utils

import androidx.core.content.edit
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.exceptions.ErrorStatus
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.weatherdata.WeatherProvider
import com.thewizrd.weather_api.locationdata.WeatherLocationProviderImpl
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl
import okhttp3.Response
import java.net.HttpURLConnection
import kotlin.random.Random

object APIRequestUtils {
    /**
     * Checks if response was successful; if it was not, throw the appropriate WeatherException
     */
    @Throws(WeatherException::class)
    fun Response.checkForErrors(apiID: String, retryTimeInMs: Long = 60000) {
        checkForErrors(apiID, this, retryTimeInMs)
    }

    private fun checkForErrors(apiID: String, response: Response, retryTimeInMs: Long = 60000) {
        if (!response.isSuccessful) {
            // Check for errors
            when (response.code) {
                HttpURLConnection.HTTP_OK -> {
                }
                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    throw WeatherException(ErrorStatus.NOWEATHER)
                        .initCause(createThrowable(response))
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    throw WeatherException(ErrorStatus.QUERYNOTFOUND)
                        .initCause(createThrowable(response))
                }
                429 /* Too Many Requests */ -> {
                    throwIfRateLimited(apiID, response, retryTimeInMs)
                }
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    throw WeatherException(ErrorStatus.UNKNOWN)
                        .initCause(createThrowable(response))
                }
                else -> {
                    throw WeatherException(ErrorStatus.NOWEATHER)
                        .initCause(createThrowable(response))
                }
            }
        }
    }

    fun WeatherProviderImpl.checkForErrors(response: Response) {
        checkForErrors(this.getWeatherAPI(), response, this.getRetryTime())
    }

    fun Response.checkForErrors(provider: WeatherProviderImpl) {
        checkForErrors(provider.getWeatherAPI(), this, provider.getRetryTime())
    }

    fun WeatherLocationProviderImpl.checkForErrors(response: Response) {
        checkForErrors(this.getLocationAPI(), response, this.getRetryTime())
    }

    fun Response.checkForErrors(provider: WeatherLocationProviderImpl) {
        checkForErrors(provider.getLocationAPI(), this, provider.getRetryTime())
    }

    fun RateLimitedRequest.checkForErrors(apiID: String, response: Response) {
        checkForErrors(apiID, response, this.getRetryTime())
    }

    fun Response.checkForErrors(apiID: String, request: RateLimitedRequest) {
        checkForErrors(apiID, this, request.getRetryTime())
    }

    fun throwIfRateLimited(apiID: String, response: Response, retryTimeInMs: Long = 60000) {
        if (response.code == 429 /* Too Many Requests */) {
            setNextRetryTime(apiID, retryTimeInMs)
            throw WeatherException(ErrorStatus.NETWORKERROR)
                .initCause(createThrowable(response))
        }
    }

    fun WeatherProviderImpl.throwIfRateLimited(response: Response) {
        throwIfRateLimited(this.getWeatherAPI(), response, this.getRetryTime())
    }

    fun Response.throwIfRateLimited(provider: WeatherProviderImpl) {
        throwIfRateLimited(provider.getWeatherAPI(), this, provider.getRetryTime())
    }

    fun WeatherLocationProviderImpl.throwIfRateLimited(response: Response) {
        throwIfRateLimited(this.getLocationAPI(), response, this.getRetryTime())
    }

    fun Response.throwIfRateLimited(provider: WeatherLocationProviderImpl) {
        throwIfRateLimited(provider.getLocationAPI(), this, provider.getRetryTime())
    }

    fun RateLimitedRequest.throwIfRateLimited(apiID: String, response: Response) {
        throwIfRateLimited(apiID, response, this.getRetryTime())
    }

    fun Response.throwIfRateLimited(apiID: String, request: RateLimitedRequest) {
        throwIfRateLimited(apiID, this, request.getRetryTime())
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
    fun WeatherProvider.checkRateLimit() {
        checkRateLimit(this.getWeatherAPI())
    }

    /**
     * Check if API has been rate limited (HTTP Error 429 occurred recently)
     * If so, deny API request until time passes
     *
     * @throws WeatherException Exception if client is under rate limit
     */
    fun WeatherLocationProvider.checkRateLimit() {
        checkRateLimit(this.getLocationAPI())
    }

    private val KEY_NEXTRETRYTIME = "key_nextretrytime"

    private fun getRetryTimePrefKey(apiID: String): String {
        return "${apiID}:${KEY_NEXTRETRYTIME}"
    }

    private fun getNextRetryTime(apiID: String): Long {
        val preferences = appLib.preferences
        return preferences.getLong(getRetryTimePrefKey(apiID), -1)
    }

    private fun setNextRetryTime(apiID: String, retryTimeInMs: Long) {
        val preferences = appLib.preferences
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
                Random.nextInt(1, 60 + 1) * 60 * 1000L
            }
            /* 1 hrs */
            retryTimeInMs >= 3600000L -> {
                Random.nextInt(1, 30 + 1) * 60 * 1000L
            }
            /* 1 minute */
            retryTimeInMs >= 60000L -> {
                Random.nextInt(1, 5 + 1) * 60 * 1000L
            }
            /* 30 seconds */
            retryTimeInMs >= 30000L -> {
                Random.nextInt(5, 15 + 1) * 1000L
            }
            else -> {
                Random.nextLong(500, 5000 + 1)
            }
        }
    }

    private fun createThrowable(response: Response): Throwable {
        val errorBody = response.body?.string()
        val responseCode = response.code
        val responseMsg = response.message

        val throwableMsg = StringBuilder()
            .appendLine("HTTP Error $responseCode: $responseMsg")
            .appendLine("Request: ${response.request}")
            .appendLine("Response body: $errorBody")
            .toString()

        return Throwable(throwableMsg)
    }

    @JvmName("createThrowableExt")
    fun Response.createThrowable(): Throwable {
        return createThrowable(this)
    }
}