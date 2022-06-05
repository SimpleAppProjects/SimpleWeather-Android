package com.thewizrd.shared_resources.weatherdata.auth

import com.google.gson.annotations.SerializedName

class ProviderAppKey : ProviderKey {
    @SerializedName("app_id")
    var appId: String = ""

    @SerializedName("app_code")
    var appCode: String = ""

    constructor() : super()
    constructor(appId: String, appCode: String) : super() {
        this.appId = appId
        this.appCode = appCode
    }

    override fun fromString(input: String) {
        input.split(":").let { split ->
            appId = split.first()
            appCode = split.last()
        }
    }

    override fun toString(): String {
        return "$appId:$appCode"
    }
}