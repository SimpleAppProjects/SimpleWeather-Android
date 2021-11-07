package com.thewizrd.shared_resources.locationdata.citydb

import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.database.City
import com.thewizrd.shared_resources.database.CityDatabase
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Location provider using city id database provided by OpenWeatherMap
 * https://openweathermap.org/faq
 */
class CityDBLocationProvider : LocationProviderImpl() {
    private val cityDatabase = CityDatabase.getInstance(SimpleLibrary.instance.appContext)

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
    ): Collection<LocationQueryViewModel> {
        val locations = HashSet<LocationQueryViewModel>()

        try {
            val cities = cityDatabase.cityDAO().findLocationsByQuery(ac_query ?: "")

            for (city in cities) {
                locations.add(createLocationModel(city, weatherAPI!!))
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "CityDBLocationProvider: error getting location")
        }

        if (locations.isEmpty()) {
            locations.add(LocationQueryViewModel())
        }

        return locations
    }

    override suspend fun getLocation(
        coordinate: Coordinate,
        weatherAPI: String?
    ): LocationQueryViewModel {
        val location: LocationQueryViewModel
        var result: City?

        try {
            val df = DecimalFormat.getInstance() as DecimalFormat
            df.applyPattern("0.0")
            df.roundingMode = RoundingMode.DOWN // Truncate value

            val lat = df.format(coordinate.latitude)
            val lon = df.format(coordinate.longitude)
            val cities = cityDatabase.cityDAO().findLocationsByCoordinate(lat, lon)
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

        location = result?.let { createLocationModel(it, weatherAPI!!) } ?: LocationQueryViewModel()

        return location
    }

    override suspend fun getLocationFromID(model: LocationQueryViewModel): LocationQueryViewModel? {
        return null
    }

    override suspend fun isKeyValid(key: String?): Boolean {
        return false
    }

    override fun getAPIKey(): String? {
        return null
    }
}