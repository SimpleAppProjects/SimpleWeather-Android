package com.thewizrd.shared_resources.weatherdata.auth

import com.google.gson.annotations.SerializedName
import okio.ByteString.Companion.decodeBase64
import java.nio.charset.StandardCharsets

class BasicAuthProviderKey : ProviderKey {
    @SerializedName("username")
    var username: String = ""

    @SerializedName("password")
    var password: String = ""

    constructor() : super()
    constructor(username: String, password: String) : super() {
        this.username = username
        this.password = password
    }

    override fun fromString(input: String) {
        input.replaceFirst("Basic ", "").decodeBase64()?.string(StandardCharsets.ISO_8859_1)?.let {
            it.split(":").let { split ->
                username = split.first()
                password = split.last()
            }
        }
    }

    override fun toString(): String {
        return okhttp3.Credentials.basic(username, password)
    }
}