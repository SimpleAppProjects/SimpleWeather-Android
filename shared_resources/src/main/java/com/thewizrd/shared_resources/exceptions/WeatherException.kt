package com.thewizrd.shared_resources.exceptions

import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps

enum class ErrorStatus {
    UNKNOWN, SUCCESS, NOWEATHER, NETWORKERROR, INVALIDAPIKEY, QUERYNOTFOUND
}

class WeatherException(val errorStatus: ErrorStatus) : Exception() {
    override val message: String
        get() {
            return when (errorStatus) {
                ErrorStatus.NOWEATHER -> {
                    sharedDeps.context.getString(R.string.werror_noweather)
                }
                ErrorStatus.NETWORKERROR -> {
                    sharedDeps.context.getString(R.string.werror_networkerror)
                }
                ErrorStatus.INVALIDAPIKEY -> {
                    sharedDeps.context.getString(R.string.werror_invalidkey)
                }
                ErrorStatus.QUERYNOTFOUND -> {
                    sharedDeps.context.getString(R.string.werror_querynotfound)
                }
                else -> {
                    // ErrorStatus.UNKNOWN
                    sharedDeps.context.getString(R.string.werror_unknown)
                }
            }
        }
}