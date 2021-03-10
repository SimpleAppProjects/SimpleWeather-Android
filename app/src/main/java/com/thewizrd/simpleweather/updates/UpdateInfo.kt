package com.thewizrd.simpleweather.updates

import com.google.gson.annotations.SerializedName

class UpdateInfo {
    @SerializedName("version")
    var versionCode = 0

    @SerializedName("updatePriority")
    var updatePriority = 0
}