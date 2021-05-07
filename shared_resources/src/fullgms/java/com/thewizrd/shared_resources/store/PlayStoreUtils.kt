package com.thewizrd.shared_resources.store

import android.net.Uri

object PlayStoreUtils {
    // Link to Play Store listing
    const val PLAY_STORE_APP_URI = "market://details?id=com.thewizrd.simpleweather"
    const val PLAY_STORE_APP_WEBURI = "https://play.google.com/store/apps/details?id=com.thewizrd.simpleweather"

    fun getPlayStoreURI(): Uri {
        return Uri.parse(PLAY_STORE_APP_URI)
    }

    fun getPlayStoreWebURI(): Uri {
        return Uri.parse(PLAY_STORE_APP_WEBURI)
    }
}