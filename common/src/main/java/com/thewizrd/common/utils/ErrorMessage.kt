package com.thewizrd.common.utils

import androidx.annotation.StringRes
import com.thewizrd.shared_resources.exceptions.WeatherException

interface ErrorMessage {
    data class Resource(
        @StringRes val stringId: Int
    ) : ErrorMessage

    data class String(
        val message: kotlin.String
    ) : ErrorMessage

    data class WeatherError(
        val exception: WeatherException
    ) : ErrorMessage
}