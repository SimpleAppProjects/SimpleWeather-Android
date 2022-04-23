package com.thewizrd.weather_api.here.auth

val hereOAuthService: HEREOAuthService by lazy { HEREOAuthServiceImpl() }

interface HEREOAuthService {
    companion object {
        const val HERE_OAUTH_URL = "https://account.api.here.com/oauth2/token"
    }

    suspend fun getBearerToken(forceRefresh: Boolean): String?
}