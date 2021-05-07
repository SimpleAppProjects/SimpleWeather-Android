@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

enum class ErrorStatus {
    UNKNOWN, SUCCESS, NOWEATHER, NETWORKERROR, INVALIDAPIKEY, QUERYNOTFOUND
}