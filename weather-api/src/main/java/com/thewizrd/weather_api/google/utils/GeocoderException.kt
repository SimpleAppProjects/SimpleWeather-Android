package com.thewizrd.weather_api.google.utils

class GeocoderException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable) : super(message, cause)
}