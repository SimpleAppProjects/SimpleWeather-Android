@file:Suppress("PropertyName")

package com.thewizrd.shared_resources.utils

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery

object LocationUtils {
    // xMin -> lon_min, xMax -> lon_max; yMin -> lat_min, yMax -> lat_max
    // xMax -> East, xMin -> West, yMax -> North, yMin -> South
    private val US_BOUNDING_BOX = BoundingBox(24.9493, 49.5904, -125.0011, -66.9326)

    // Canada
    private val CA_BOUNDING_BOX = BoundingBox(41.6765556, 83.3362128, -141.00275, -52.3231981)

    // France
    private val FR_BOUNDING_BOX = BoundingBox(41.2632185, 51.268318, -5.4534286, 9.8678344)

    // Puerto Rico
    private val PR_BOUNDING_BOX =
        BoundingBox(17.7306659963, 18.6663824908, -68.1108798087, -65.1100910828)

    // US Virgin Islands
    private val VI_BOUNDING_BOX =
        BoundingBox(17.6234681162, 18.4649841585, -65.1541175321, -64.512674287)

    // Guam & The Northern Mariana Islands
    private val GU_MP_BOUNDING_BOX =
        BoundingBox(13.019485113, 20.7560506513, 144.3987098565, 146.3240638604)

    // American Somoa
    private val AS_BOUNDING_BOX =
        BoundingBox(-14.6018129466, -10.9972026743, -171.141907163, -168.1016121805)

    // Germany
    private val DE_BOUNDING_BOX =
        BoundingBox(47.2701114, 55.099161, 5.8663153, 15.0419319)

    private val CAMS_OPENMETEO_EUROPE_BBOX =
        BoundingBox(30.0, 72.0, -25.0, 45.0)

    // United Kingdom and Ireland
    private val UK_IE_BOUNDING_BOX =
        BoundingBox(49.674, 61.061, -14.015517, 2.0919117)

    // Korea
    private val KOREA_BOUNDING_BOX =
        BoundingBox(32.9104556, 43.0078553939, 124.3017732603, 131.0789644224)

    // Japan
    private val JP_BOUNDING_BOX =
        BoundingBox(20.2145811, 45.7563343, 122.7141754, 154.205541)

    // Switzerland
    private val CH_BOUNDING_BOX =
        BoundingBox(45.8179579, 47.8084544, 5.9558318, 10.4922941)

    // Met Norway Area
    private val METNO_BOUNDING_BOX =
        BoundingBox(54.4504920972, 81.0280176, -9.6846279, 34.6889114)

    // Australia
    private val AU_BOUNDING_BOX =
        BoundingBox(-55.3228175, -9.0880125, 72.2461932, 168.2261259)

    // China
    private val CN_BOUNDING_BOX =
        BoundingBox(17.917694912, 53.5608154, 73.4997347, 134.7751959)

    // Netherlands
    private val NL_BOUNDING_BOX =
        BoundingBox(50.7218810189, 53.7801536938, 3.3388847969, 7.590283575)

    // Denmark
    private val DK_BOUNDING_BOX =
        BoundingBox(54.4516667, 57.9524297, 7.7153255, 15.5530641)

    // Italy
    private val IT_BOUNDING_BOX =
        BoundingBox(35.2889616, 47.0921485, 6.6272658, 18.7844746)

    // Austria
    private val AT_BOUNDING_BOX =
        BoundingBox(46.3722987, 49.0205239, 9.5307487, 17.1607728)

    private val NWS_SUPPORTED_COUNTRIES = setOf("US", "AS", "UM", "GU", "MP", "PR", "VI")
    private val NWS_SUPPORTED_LOCATIONS = listOf(
        US_BOUNDING_BOX,
        PR_BOUNDING_BOX,
        VI_BOUNDING_BOX,
        GU_MP_BOUNDING_BOX,
        AS_BOUNDING_BOX
    )

    private val OPENMETEO_SUPPORTED_COUNTRIES = setOf(
        "US", "AS", "UM", "GU", "MP", "PR", "VI", // NWS
        "UK", "IE", // UK and Ireland - UK Met Office
        "FR", // France - MeteoFrance
        "KR", "KP", // Korea - KMA Korea
        "JP", // JMA Japan
        "CH", // Switzerland - MeteoSwiss
        "NO", "DK", "SE", "FI", // Norway, Denmark, Sweden, Finland - Met Norway
        "CA", // Canada - GEM Canada
        "AU", // Australia - BOM Australia
        "CN", // China - CMA China
        "NL", // Netherlands - KNMI Netherlands
        "DK", // Denmark - DMI Denmark
        "IT", // Italy - ItaliaMeteo
        "AT", // Austria - GeoSphere Austria
    )
    private val OPENMETEO_SUPPORTED_LOCATIONS = listOf(
        // NWS
        US_BOUNDING_BOX,
        PR_BOUNDING_BOX,
        VI_BOUNDING_BOX,
        GU_MP_BOUNDING_BOX,
        AS_BOUNDING_BOX,
        // UK and Ireland
        UK_IE_BOUNDING_BOX,
        // France
        FR_BOUNDING_BOX,
        // Korea
        KOREA_BOUNDING_BOX,
        // Japan
        JP_BOUNDING_BOX,
        // Switzerland
        CH_BOUNDING_BOX,
        // MetNo
        METNO_BOUNDING_BOX,
        // Canada
        CA_BOUNDING_BOX,
        // Australia
        AU_BOUNDING_BOX,
        // China
        CN_BOUNDING_BOX,
        // Netherlands
        NL_BOUNDING_BOX,
        // Denmark
        DK_BOUNDING_BOX,
        // Italy
        IT_BOUNDING_BOX,
        // Austria,
        AT_BOUNDING_BOX,
    )

    fun isUS(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            countryCode.equals("us", ignoreCase = true) || countryCode.equals(
                "usa",
                ignoreCase = true
            ) || countryCode.lowercase().contains("united states")
        }
    }

    private fun inUSBounds(lat: Double, lon: Double) = US_BOUNDING_BOX.intersects(lat, lon)

    fun isUS(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isUS(location.countryCode) || inUSBounds(location.latitude, location.longitude)
        } else {
            inUSBounds(location.latitude, location.longitude)
        }
    }

    fun isUS(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isUS(location.locationCountry) || inUSBounds(
                location.locationLat,
                location.locationLong
            )
        } else {
            inUSBounds(location.locationLat, location.locationLong)
        }
    }

    fun isUSorCanada(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            isUS(countryCode) || isCanada(countryCode)
        }
    }

    private fun inUSorCanadaBounds(lat: Double, lon: Double) =
        inUSBounds(lat, lon) || inCanadaBounds(lat, lon)

    fun isUSorCanada(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isUSorCanada(location.countryCode) || inUSorCanadaBounds(
                location.latitude,
                location.longitude
            )
        } else {
            inUSorCanadaBounds(location.latitude, location.longitude)
        }
    }

    fun isUSorCanada(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isUSorCanada(location.locationCountry) || inUSorCanadaBounds(
                location.locationLat,
                location.locationLong
            )
        } else {
            inUSorCanadaBounds(location.locationLat, location.locationLong)
        }
    }

    fun isCanada(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            countryCode.equals("CA", ignoreCase = true) || countryCode.equals(
                "canada",
                ignoreCase = true
            )
        }
    }

    private fun inCanadaBounds(lat: Double, lon: Double) = CA_BOUNDING_BOX.intersects(lat, lon)

    fun isCanada(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isCanada(location.countryCode) || inCanadaBounds(location.latitude, location.longitude)
        } else {
            inCanadaBounds(location.latitude, location.longitude)
        }
    }

    fun isCanada(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isCanada(location.locationCountry) || inCanadaBounds(
                location.locationLat,
                location.locationLong
            )
        } else {
            inCanadaBounds(location.locationLat, location.locationLong)
        }
    }

    fun isFrance(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            countryCode.equals("fr", ignoreCase = true) || countryCode.equals(
                "france",
                ignoreCase = true
            )
        }
    }

    private fun inFranceBounds(lat: Double, lon: Double) = FR_BOUNDING_BOX.intersects(lat, lon)

    fun isFrance(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isFrance(location.countryCode) || inFranceBounds(location.latitude, location.longitude)
        } else {
            inFranceBounds(location.latitude, location.longitude)
        }
    }

    fun isFrance(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isFrance(location.locationCountry) || inFranceBounds(
                location.locationLat,
                location.locationLong
            )
        } else {
            inFranceBounds(location.locationLat, location.locationLong)
        }
    }

    fun isNWSSupported(countryCode: String?): Boolean {
        return NWS_SUPPORTED_COUNTRIES.contains(countryCode?.uppercase())
    }

    fun isNWSSupported(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isNWSSupported(location.countryCode)
        } else {
            NWS_SUPPORTED_LOCATIONS.any { it.intersects(location.latitude, location.longitude) }
        }
    }

    fun isNWSSupported(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isNWSSupported(location.locationCountry)
        } else {
            NWS_SUPPORTED_LOCATIONS.any {
                it.intersects(
                    location.locationLat,
                    location.locationLong
                )
            }
        }
    }

    fun isGermany(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            countryCode.equals("de", ignoreCase = true) || countryCode.equals(
                "germany",
                ignoreCase = true
            )
        }
    }

    private fun inGermanyBounds(lat: Double, lon: Double) = DE_BOUNDING_BOX.intersects(lat, lon)

    fun isGermany(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isGermany(location.countryCode) || inGermanyBounds(
                location.latitude,
                location.longitude
            )
        } else {
            inGermanyBounds(location.latitude, location.longitude)
        }
    }

    fun isGermany(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isGermany(location.locationCountry) || inGermanyBounds(
                location.locationLat,
                location.locationLong
            )
        } else {
            inGermanyBounds(location.locationLat, location.locationLong)
        }
    }

    fun isCAMSEuroCovered(location: LocationData): Boolean {
        return CAMS_OPENMETEO_EUROPE_BBOX.intersects(location.latitude, location.longitude)
    }

    fun isOpenMeteoSupported(countryCode: String?): Boolean {
        return OPENMETEO_SUPPORTED_COUNTRIES.contains(countryCode?.uppercase())
    }

    fun isOpenMeteoSupported(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isOpenMeteoSupported(location.countryCode)
        } else {
            OPENMETEO_SUPPORTED_LOCATIONS.any {
                it.intersects(
                    location.latitude,
                    location.longitude
                )
            }
        }
    }

    fun isOpenMeteoSupported(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isOpenMeteoSupported(location.locationCountry)
        } else {
            OPENMETEO_SUPPORTED_LOCATIONS.any {
                it.intersects(
                    location.locationLat,
                    location.locationLong
                )
            }
        }
    }

    private data class BoundingBox(
        val lat_min: Double,
        val lat_max: Double,
        val lon_min: Double,
        val lon_max: Double,
    ) {
        fun intersects(lat: Double, lon: Double): Boolean {
            return (lat in lat_min..lat_max) && (lon in lon_min..lon_max)
        }

        fun intersects(other: BoundingBox): Boolean {
            return lat_min <= other.lat_max && lat_max >= other.lat_min &&
                    lon_min <= other.lon_max && lon_max >= other.lon_min
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BoundingBox

            if (lat_min != other.lat_min) return false
            if (lat_max != other.lat_max) return false
            if (lon_min != other.lon_min) return false
            if (lon_max != other.lon_max) return false

            return true
        }

        override fun hashCode(): Int {
            var result = lat_min.hashCode()
            result = 31 * result + lat_max.hashCode()
            result = 31 * result + lon_min.hashCode()
            result = 31 * result + lon_max.hashCode()
            return result
        }
    }
}
