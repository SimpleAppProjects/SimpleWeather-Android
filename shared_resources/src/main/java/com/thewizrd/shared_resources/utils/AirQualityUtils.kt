package com.thewizrd.shared_resources.utils

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * AQI Calculations based on AirNow AQI Calculator
 * https://www.airnow.gov/aqi/aqi-calculator-concentration/
 */
object AirQualityUtils {
    fun CO_ugm3_TO_ppm(value: Double): Double {
        // 1ppm = 1.15mg/m3 = 1150ug/m3 (CO)
        return value / 1150
    }

    fun SO2_ugm3_to_ppb(value: Double): Double {
        // 1ppb = 2.62ug/m3 (SO2)
        return value / 2.62
    }

    fun NO2_ugm3_to_ppb(value: Double): Double {
        // 1ppb = 1.88ug/m3 (NO2)
        return value / 1.88
    }

    fun NO_ugm3_to_ppb(value: Double): Double {
        // 1ppb = 1.25ug/m3 (NO)
        return value / 1.88
    }

    fun O3_ugm3_to_ppb(value: Double): Double {
        // 1ppb = 2.00ug/m3 (O3)
        return value / 2.00
    }

    private fun AQICalc(aqiHi: Double, aqiLo: Double, concHi: Double, concLo: Double, concVal: Double): Int {
        val aqiValue = ((concVal - concLo) / (concHi - concLo)) * (aqiHi - aqiLo) + aqiLo
        return aqiValue.roundToInt()
    }

    fun AQIO3(value: Double): Int {
        return if (floor(value) <= 200)
            AQIO3_8hr(value)
        else
            AQIO3_1hr(value)
    }

    fun AQIO3_8hr(value: Double): Int {
        val conc = floor(value) / 1000;

        when {
            conc >= 0 && conc < .055 -> {
                return AQICalc(50.0, 0.0, 0.054, 0.0, conc);
            }
            conc >= .055 && conc < .071 -> {
                return AQICalc(100.0, 51.0, .070, .055, conc);
            }
            conc >= .071 && conc < .086 -> {
                return AQICalc(150.0, 101.0, .085, .071, conc);
            }
            conc >= .086 && conc < .106 -> {
                return AQICalc(200.0, 151.0, .105, .086, conc);
            }
            conc >= .106 && conc < .201 -> {
                return AQICalc(300.0, 201.0, .200, .106, conc);
            }
            conc >= .201 && conc < .605 -> {
                throw IllegalArgumentException("8-hour ozone values do not define higher AQI values (>=301); calculate using 1-hour O3 conc")
            }
            else -> {
                throw IllegalArgumentException("value out of range")
            }
        }
    }

    fun AQIO3_1hr(value: Double): Int {
        val conc = floor(value) / 1000;

        when {
            conc in 0.0..124.0 -> {
                throw IllegalArgumentException("1-hour ozone values do not define lower AQI values (<= 100); AQI values of 100 or lower are calculated with 8-hour ozone concentrations.")
            }
            conc >= .125 && conc < .165 -> {
                return AQICalc(150.0, 101.0, .164, .125, conc);
            }
            conc >= .165 && conc < .205 -> {
                return AQICalc(200.0, 151.0, .204, .165, conc);
            }
            conc >= .205 && conc < .405 -> {
                return AQICalc(300.0, 201.0, .404, .205, conc);
            }
            conc >= .405 && conc < .505 -> {
                return AQICalc(400.0, 301.0, .504, .405, conc);
            }
            conc >= .505 && conc < .605 -> {
                return AQICalc(500.0, 401.0, .604, .505, conc);
            }
            else -> {
                throw IllegalArgumentException("value out of range")
            }
        }
    }

    fun AQIPM2_5(value: Double): Int {
        val conc = floor(value * 10) / 10

        when {
            conc >= 0 && conc < 12.1 -> {
                return AQICalc(50.0, 0.0, 12.0, 0.0, conc)
            }
            conc >= 12.1 && conc < 35.5 -> {
                return AQICalc(100.0, 51.0, 35.4, 12.1, conc)
            }
            conc >= 35.5 && conc < 55.5 -> {
                return AQICalc(150.0, 101.0, 55.4, 35.5, conc)
            }
            conc >= 55.5 && conc < 150.5 -> {
                return AQICalc(200.0, 151.0, 150.4, 55.5, conc)
            }
            conc >= 150.5 && conc < 250.5 -> {
                return AQICalc(300.0, 201.0, 250.4, 150.5, conc)
            }
            conc >= 250.5 && conc < 350.5 -> {
                return AQICalc(400.0, 301.0, 350.4, 250.5, conc)
            }
            conc >= 350.5 && conc < 500.5 -> {
                return AQICalc(500.0, 401.0, 500.4, 350.5, conc)
            }
            else -> throw IllegalArgumentException("value out of range")
        }
    }

    fun AQIPM10(value: Double): Int {
        val conc = floor(value)

        when {
            conc >= 0 && conc < 55 -> {
                return AQICalc(50.0, 0.0, 54.0, 0.0, conc)
            }
            conc >= 55 && conc < 155 -> {
                return AQICalc(100.0, 51.0, 154.0, 55.0, conc)
            }
            conc >= 155 && conc < 255 -> {
                return AQICalc(150.0, 101.0, 254.0, 155.0, conc)
            }
            conc >= 255 && conc < 355 -> {
                return AQICalc(200.0, 151.0, 354.0, 255.0, conc)
            }
            conc >= 355 && conc < 425 -> {
                return AQICalc(300.0, 201.0, 424.0, 355.0, conc)
            }
            conc >= 425 && conc < 505 -> {
                return AQICalc(400.0, 301.0, 504.0, 425.0, conc)
            }
            conc >= 505 && conc < 605 -> {
                return AQICalc(500.0, 401.0, 604.0, 505.0, conc)
            }
            else -> throw IllegalArgumentException("value out of range")
        }
    }

    fun AQICO(value: Double): Int {
        val conc = floor(10 * value) / 10;

        when {
            conc >= 0 && conc < 4.5 -> {
                return AQICalc(50.0, 0.0, 4.4, 0.0, conc);
            }
            conc >= 4.5 && conc < 9.5 -> {
                return AQICalc(100.0, 51.0, 9.4, 4.5, conc);
            }
            conc >= 9.5 && conc < 12.5 -> {
                return AQICalc(150.0, 101.0, 12.4, 9.5, conc);
            }
            conc >= 12.5 && conc < 15.5 -> {
                return AQICalc(200.0, 151.0, 15.4, 12.5, conc);
            }
            conc >= 15.5 && conc < 30.5 -> {
                return AQICalc(300.0, 201.0, 30.4, 15.5, conc);
            }
            conc >= 30.5 && conc < 40.5 -> {
                return AQICalc(400.0, 301.0, 40.4, 30.5, conc);
            }
            conc >= 40.5 && conc < 50.5 -> {
                return AQICalc(500.0, 401.0, 50.4, 40.5, conc);
            }
            else -> {
                throw IllegalArgumentException("value out of range")
            }
        }
    }

    fun AQISO2(value: Double): Int {
        return if (floor(value) >= 305)
            AQISO2_24hr(value)
        else
            AQISO2_1hr(value)
    }

    fun AQISO2_1hr(value: Double): Int {
        val conc = floor(value);

        when {
            conc >= 0 && conc < 36 -> {
                return AQICalc(50.0, 0.0, 35.0, 0.0, conc);
            }
            conc >= 36 && conc < 76 -> {
                return AQICalc(100.0, 51.0, 75.0, 36.0, conc);
            }
            conc >= 76 && conc < 186 -> {
                return AQICalc(150.0, 101.0, 185.0, 76.0, conc);
            }
            conc >= 186 && conc < 305 -> {
                return AQICalc(200.0, 151.0, 304.0, 186.0, conc);
            }
            conc in 305.0..604.0 -> {
                throw IllegalArgumentException("AQI values of 201 or greater are calculated with 24-hour SO2 concentrations")
            }
            else -> {
                throw IllegalArgumentException("value out of range")
            }
        }
    }

    fun AQISO2_24hr(value: Double): Int {
        val conc = floor(value);

        when {
            conc >= 0 && conc <= 304 -> {
                throw IllegalArgumentException("AQI values less than 201 are calculated with 1-hour SO2 concentrations")
            }
            conc >= 305 && conc < 605 -> {
                return AQICalc(300.0, 201.0, 604.0, 305.0, conc);
            }
            conc >= 605 && conc < 804 -> {
                return AQICalc(400.0, 301.0, 804.0, 605.0, conc);
            }
            conc >= 805 && conc < 1005 -> {
                return AQICalc(500.0, 401.0, 1004.0, 805.0, conc);
            }
            else -> {
                throw IllegalArgumentException("value out of range")
            }
        }
    }

    fun AQINO2(value: Double): Int {
        val conc = floor(value) / 1000;

        when {
            conc >= 0 && conc < .054 -> {
                return AQICalc(50.0, 0.0, .053, 0.0, conc);
            }
            conc >= .054 && conc < .101 -> {
                return AQICalc(100.0, 51.0, .100, .054, conc);
            }
            conc >= .101 && conc < .361 -> {
                return AQICalc(150.0, 101.0, .360, .101, conc);
            }
            conc >= .361 && conc < .650 -> {
                return AQICalc(200.0, 151.0, .649, .361, conc);
            }
            conc >= .650 && conc < 1.250 -> {
                return AQICalc(300.0, 201.0, 1.249, .650, conc);
            }
            conc >= 1.250 && conc < 1.650 -> {
                return AQICalc(400.0, 301.0, 1.649, 1.250, conc);
            }
            conc >= 1.650 && conc <= 2.049 -> {
                return AQICalc(500.0, 401.0, 2.049, 1.650, conc);
            }
            else -> {
                throw IllegalArgumentException("value out of range")
            }
        }
    }
}