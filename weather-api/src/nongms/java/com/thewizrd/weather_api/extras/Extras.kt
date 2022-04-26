package com.thewizrd.weather_api.extras

import com.thewizrd.extras.extrasModule
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.appLib
import okhttp3.CacheControl
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun Request.Builder.cacheRequestIfNeeded(keyRequired: Boolean, maxAge: Int, timeUnit: TimeUnit) =
    apply {
        if (keyRequired && appLib.settingsManager.usePersonalKey()) {
            // relax cache rules for the following users
        } else {
            cacheControl(
                CacheControl.Builder()
                    .maxAge(maxAge, timeUnit)
                    .build()
            )
        }
    }