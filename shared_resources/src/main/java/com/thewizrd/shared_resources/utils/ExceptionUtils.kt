package com.thewizrd.shared_resources.utils

object ExceptionUtils {
    @JvmStatic
    fun <T : Throwable> T.copyStackTrace(e: Exception): T {
        this.stackTrace = e.stackTrace
        return this
    }
}