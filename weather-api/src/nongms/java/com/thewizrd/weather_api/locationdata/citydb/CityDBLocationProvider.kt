package com.thewizrd.weather_api.locationdata.citydb

import android.util.Log
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.database.City
import com.thewizrd.weather_api.database.CityDatabase
import com.thewizrd.weather_api.locationdata.WeatherLocationProviderImpl
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Location provider using city id database provided by OpenWeatherMap
 * https://openweathermap.org/faq
 */
class CityDBLocationProvider : WeatherLocationProviderImpl() {
    private val cityDBDao = CityDatabase.getCityDAO(appLib.context)

    override fun getLocationAPI(): String {
        return WeatherAPI.OPENWEATHERMAP
    }

    override fun isKeyRequired(): Boolean {
        return false
    }

    override fun supportsLocale(): Boolean {
        return false
    }

    override suspend fun getLocations(
        ac_query: String?,
        weatherAPI: String?
    ): Collection<LocationQuery> {
        val locations = HashSet<LocationQuery>()

        try {
            val cities = cityDBDao.findLocationsByQuery(ac_query ?: "")

            for (city in cities) {
                locations.add(createLocationModel(city, weatherAPI!!))
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "CityDBLocationProvider: error getting location")
        }

        if (locations.isEmpty()) {
            locations.add(LocationQuery())
        }

        return locations
    }

    override suspend fun getLocation(
        coordinate: Coordinate,
        weatherAPI: String?
    ): LocationQuery {
        val location: LocationQuery
        var result: City?

        try {
            val df = DecimalFormat.getInstance() as DecimalFormat
            df.applyPattern("0.0")
            df.roundingMode = RoundingMode.DOWN // Truncate value

            val lat = df.format(coordinate.latitude)
            val lon = df.format(coordinate.longitude)
            val cities = cityDBDao.findLocationsByCoordinate(lat, lon)
            var closestCity: Pair<City, Double>? = null

            for (city in cities) {
                if (closestCity == null) {
                    closestCity = Pair(
                        city,
                        ConversionMethods.calculateHaversine(
                            coordinate.latitude, coordinate.longitude,
                            city.lat, city.lon
                        )
                    )
                } else {
                    val distance = ConversionMethods.calculateHaversine(
                        coordinate.latitude, coordinate.longitude,
                        city.lat, city.lon
                    )

                    if (distance < closestCity.second) {
                        closestCity = Pair(city, distance)
                    }
                }
            }

            result = closestCity?.first ?: cities.firstOrNull()
        } catch (ex: Exception) {
            result = null
            Logger.writeLine(Log.ERROR, ex, "CityDBLocationProvider: error getting location")
        }

        location = result?.let { createLocationModel(it, weatherAPI!!) } ?: LocationQuery()

        return location
    }

    override suspend fun getLocationFromID(model: LocationQuery): LocationQuery? {
        return null
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }
}