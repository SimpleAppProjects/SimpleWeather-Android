package com.thewizrd.weather_api.utils

interface RateLimitedRequest {
    /**
     * Time in milliseconds to wait until next request
     * (ex. 60 calls / per minute -> 60s wait time)
     *
     * @return retry time in milliseconds
     */
    fun getRetryTime(): Long
}