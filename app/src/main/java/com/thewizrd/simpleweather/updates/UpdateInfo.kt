package com.thewizrd.simpleweather.updates

import com.squareup.moshi.Json

class UpdateInfo {
    @Json(name = "version")
    var versionCode = 0

    @Json(name = "updatePriority")
    var updatePriority = 0
}