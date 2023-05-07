package com.thewizrd.weather_api.weatherkit.auth

val weatherKitJwtService: WeatherKitJwtService by lazy { WeatherKitJwtServiceImpl() }

interface WeatherKitJwtService {
    suspend fun getBearerToken(forceRefresh: Boolean): String?
}