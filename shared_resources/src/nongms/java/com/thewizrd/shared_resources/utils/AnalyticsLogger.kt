package com.thewizrd.shared_resources.utils

import android.os.Bundle
import android.util.Log
import androidx.annotation.Size

object AnalyticsLogger {
    @JvmOverloads
    @JvmStatic
    fun logEvent(@Size(min = 1L, max = 40L) eventName: String, properties: Bundle? = null) {
        val append =
            if (properties == null) "" else StringUtils.lineSeparator() + properties.toString()
        Logger.writeLine(Log.INFO, "EVENT | $eventName$append")
    }
}