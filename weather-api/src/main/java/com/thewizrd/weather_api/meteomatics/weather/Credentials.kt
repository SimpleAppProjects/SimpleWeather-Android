package com.thewizrd.weather_api.meteomatics.weather

import com.google.gson.annotations.SerializedName

data class Credentials(
    @SerializedName("username")
    var username: String,
    @SerializedName("password")
    var password: String
)
