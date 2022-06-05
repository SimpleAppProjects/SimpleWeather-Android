package com.thewizrd.shared_resources.weatherdata.auth

import com.google.gson.annotations.SerializedName

class ProviderApiKey : ProviderKey {
    @SerializedName("key")
    var key: String = ""

    constructor() : super()
    constructor(apikey: String) : super() {
        this.key = apikey
    }

    override fun fromString(input: String) {
        key = input
    }

    override fun toString(): String {
        return key
    }
}