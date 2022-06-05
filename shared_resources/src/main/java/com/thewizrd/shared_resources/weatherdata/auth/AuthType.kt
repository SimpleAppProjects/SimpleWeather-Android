package com.thewizrd.shared_resources.weatherdata.auth

enum class AuthType {
    NONE,
    APIKEY,
    APPID_APPCODE,
    BASIC, // Username, Password
    INTERNAL // handled internally
}