@file:JvmMultifileClass
@file:JvmName("WeatherUtils")

package com.thewizrd.simpleweather.images

import android.net.Uri
import android.util.Log
import androidx.annotation.ColorInt
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.preferences.UpdateSettings
import com.thewizrd.shared_resources.utils.FileUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.images.model.ImageData
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI

suspend fun Weather.getImageData(): ImageDataViewModel? {
    val icon: String = this.condition.icon
    val backgroundCode: String
    val wm = weatherModule.weatherManager

    // Apply background based on weather condition
    backgroundCode = when (icon) {
        WeatherIcons.DAY_HAIL,
        WeatherIcons.DAY_RAIN,
        WeatherIcons.DAY_RAIN_MIX,
        WeatherIcons.DAY_RAIN_WIND,
        WeatherIcons.DAY_SHOWERS,
        WeatherIcons.DAY_SLEET,
        WeatherIcons.DAY_SPRINKLE -> WeatherBackground.RAIN

        WeatherIcons.NIGHT_ALT_HAIL,
        WeatherIcons.NIGHT_ALT_RAIN,
        WeatherIcons.NIGHT_ALT_RAIN_MIX,
        WeatherIcons.NIGHT_ALT_RAIN_WIND,
        WeatherIcons.NIGHT_ALT_SHOWERS,
        WeatherIcons.NIGHT_ALT_SLEET,
        WeatherIcons.NIGHT_ALT_SPRINKLE,
        WeatherIcons.RAIN,
        WeatherIcons.RAIN_MIX,
        WeatherIcons.RAIN_WIND,
        WeatherIcons.SHOWERS,
        WeatherIcons.SLEET,
        WeatherIcons.SPRINKLE -> WeatherBackground.RAIN_NIGHT

        WeatherIcons.DAY_LIGHTNING,
        WeatherIcons.DAY_THUNDERSTORM,
        WeatherIcons.NIGHT_ALT_LIGHTNING,
        WeatherIcons.NIGHT_ALT_THUNDERSTORM,
        WeatherIcons.LIGHTNING,
        WeatherIcons.THUNDERSTORM -> WeatherBackground.TSTORMS_NIGHT

        WeatherIcons.DAY_STORM_SHOWERS,
        WeatherIcons.DAY_SLEET_STORM,
        WeatherIcons.STORM_SHOWERS,
        WeatherIcons.SLEET_STORM,
        WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
        WeatherIcons.NIGHT_ALT_SLEET_STORM,
        WeatherIcons.HAIL,
        WeatherIcons.HURRICANE,
        WeatherIcons.TORNADO -> WeatherBackground.STORMS

        WeatherIcons.DUST, WeatherIcons.SANDSTORM -> WeatherBackground.DUST

        WeatherIcons.DAY_FOG,
        WeatherIcons.DAY_HAZE,
        WeatherIcons.FOG,
        WeatherIcons.HAZE,
        WeatherIcons.NIGHT_FOG,
        WeatherIcons.NIGHT_HAZE,
        WeatherIcons.SMOG,
        WeatherIcons.SMOKE -> WeatherBackground.FOG

        WeatherIcons.DAY_SNOW,
        WeatherIcons.DAY_SNOW_THUNDERSTORM,
        WeatherIcons.DAY_SNOW_WIND,
        WeatherIcons.NIGHT_ALT_SNOW,
        WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
        WeatherIcons.NIGHT_ALT_SNOW_WIND,
        WeatherIcons.SNOW,
        WeatherIcons.SNOW_THUNDERSTORM,
        WeatherIcons.SNOW_WIND -> WeatherBackground.SNOW

        WeatherIcons.CLOUD,
        WeatherIcons.CLOUDY,
        WeatherIcons.CLOUDY_GUSTS,
        WeatherIcons.CLOUDY_WINDY,
        WeatherIcons.DAY_CLOUDY,
        WeatherIcons.DAY_CLOUDY_GUSTS,
        WeatherIcons.DAY_CLOUDY_HIGH,
        WeatherIcons.DAY_CLOUDY_WINDY,
        WeatherIcons.NIGHT_ALT_CLOUDY,
        WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
        WeatherIcons.NIGHT_ALT_CLOUDY_HIGH,
        WeatherIcons.NIGHT_ALT_CLOUDY_WINDY,
        WeatherIcons.DAY_SUNNY_OVERCAST,
        WeatherIcons.NIGHT_OVERCAST,
        WeatherIcons.OVERCAST -> {
            if (wm.isNight(this))
                WeatherBackground.MOSTLYCLOUDY_NIGHT
            else
                WeatherBackground.MOSTLYCLOUDY_DAY
        }

        WeatherIcons.DAY_PARTLY_CLOUDY,
        WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> {
            if (wm.isNight(this))
                WeatherBackground.PARTLYCLOUDY_NIGHT
            else
                WeatherBackground.PARTLYCLOUDY_DAY
        }

        WeatherIcons.DAY_SUNNY,
        WeatherIcons.NA,
        WeatherIcons.NIGHT_CLEAR,
        WeatherIcons.SNOWFLAKE_COLD,
        WeatherIcons.DAY_HOT,
        WeatherIcons.NIGHT_HOT,
        WeatherIcons.HOT,
        WeatherIcons.DAY_WINDY,
        WeatherIcons.NIGHT_WINDY,
        WeatherIcons.WINDY,
        WeatherIcons.DAY_LIGHT_WIND,
        WeatherIcons.NIGHT_LIGHT_WIND,
        WeatherIcons.LIGHT_WIND,
        WeatherIcons.STRONG_WIND -> {
            // Set background based using sunset/rise times
            if (wm.isNight(this)) WeatherBackground.NIGHT else WeatherBackground.DAY
        }
        else -> {
            if (wm.isNight(this)) WeatherBackground.NIGHT else WeatherBackground.DAY
        }
    }

    // Check cache for image data
    var imageData = imageDataService.getCachedImageData(backgroundCode)
    // Check if cache is available and valid
    val imgDataValid = imageData != null && imageData.isValid(appLib.context)
    // Validate image header/contents
    val imgValid = imgDataValid && imageData?.isImageValid() == true
    if (imgValid) {
        return ImageDataViewModel(imageData)
    } else {
        // Delete invalid file
        if (imgDataValid && !imgValid && imageData!!.imageURL.startsWith("file") && !imageData.imageURL.contains(
                "/android_asset/"
            )
        ) {
            val imageFile = File(URI.create(imageData.imageURL))
            if (imageFile.exists()) {
                imageFile.delete()
            }
        }

        if (!UpdateSettings.isUpdateAvailable) {
            imageData = withTimeoutOrNull(15000) {
                imageDataService.getRemoteImageData(backgroundCode)
            }
            if (imageData?.isValid(appLib.context) == true) {
                return ImageDataViewModel(imageData)
            } else {
                imageData = imageDataService.getDefaultImageData(backgroundCode, this)
                if (imageData?.isValid(appLib.context) == true)
                    return ImageDataViewModel(imageData)
            }
        } else {
            imageData = imageDataService.getDefaultImageData(backgroundCode, this)
            if (imageData?.isValid(appLib.context) == true)
                return ImageDataViewModel(imageData)
        }
    }

    return null
}

@ColorInt
fun Weather.getBackgroundColor(): Int {
    var rgb = -1
    val icon: String = this.condition.icon
    val wm = weatherModule.weatherManager

    // Apply background based on weather condition
    rgb = when (icon) {
        WeatherIcons.DAY_HAIL,
        WeatherIcons.DAY_RAIN,
        WeatherIcons.DAY_RAIN_MIX,
        WeatherIcons.DAY_RAIN_WIND,
        WeatherIcons.DAY_SHOWERS,
        WeatherIcons.DAY_SLEET,
        WeatherIcons.DAY_SPRINKLE -> 0xFF475374.toInt()

        WeatherIcons.NIGHT_ALT_HAIL,
        WeatherIcons.NIGHT_ALT_RAIN,
        WeatherIcons.NIGHT_ALT_RAIN_MIX,
        WeatherIcons.NIGHT_ALT_RAIN_WIND,
        WeatherIcons.NIGHT_ALT_SHOWERS,
        WeatherIcons.NIGHT_ALT_SLEET,
        WeatherIcons.NIGHT_ALT_SPRINKLE,
        WeatherIcons.RAIN,
        WeatherIcons.RAIN_MIX,
        WeatherIcons.RAIN_WIND,
        WeatherIcons.SHOWERS,
        WeatherIcons.SLEET,
        WeatherIcons.SPRINKLE -> 0xFF181010.toInt()

        WeatherIcons.DAY_LIGHTNING,
        WeatherIcons.DAY_THUNDERSTORM -> 0xFF283848.toInt()

        WeatherIcons.NIGHT_ALT_LIGHTNING,
        WeatherIcons.NIGHT_ALT_THUNDERSTORM,
        WeatherIcons.LIGHTNING,
        WeatherIcons.THUNDERSTORM -> 0xFF181830.toInt()

        WeatherIcons.DAY_STORM_SHOWERS,
        WeatherIcons.DAY_SLEET_STORM,
        WeatherIcons.STORM_SHOWERS,
        WeatherIcons.SLEET_STORM,
        WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
        WeatherIcons.NIGHT_ALT_SLEET_STORM,
        WeatherIcons.HAIL,
        WeatherIcons.HURRICANE,
        WeatherIcons.TORNADO -> 0xFF182830.toInt()

        WeatherIcons.DUST, WeatherIcons.SANDSTORM -> 0xFFB06810.toInt()

        WeatherIcons.DAY_FOG,
        WeatherIcons.DAY_HAZE,
        WeatherIcons.FOG,
        WeatherIcons.HAZE,
        WeatherIcons.NIGHT_FOG,
        WeatherIcons.NIGHT_HAZE,
        WeatherIcons.SMOG,
        WeatherIcons.SMOKE -> 0xFF252524.toInt()

        WeatherIcons.DAY_SNOW,
        WeatherIcons.DAY_SNOW_THUNDERSTORM,
        WeatherIcons.NIGHT_ALT_SNOW,
        WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
        WeatherIcons.SNOW,
        WeatherIcons.SNOW_THUNDERSTORM -> 0xFF646464.toInt()

        WeatherIcons.SNOW_WIND,
        WeatherIcons.DAY_SNOW_WIND,
        WeatherIcons.NIGHT_ALT_SNOW_WIND -> 0xFF545454.toInt()

        /* Ambigious weather conditions */
        // (Mostly) Cloudy
        WeatherIcons.CLOUD,
        WeatherIcons.CLOUDY,
        WeatherIcons.CLOUDY_GUSTS,
        WeatherIcons.CLOUDY_WINDY,
        WeatherIcons.DAY_CLOUDY,
        WeatherIcons.DAY_CLOUDY_GUSTS,
        WeatherIcons.DAY_CLOUDY_HIGH,
        WeatherIcons.DAY_CLOUDY_WINDY,
        WeatherIcons.NIGHT_ALT_CLOUDY,
        WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS,
        WeatherIcons.NIGHT_ALT_CLOUDY_HIGH,
        WeatherIcons.NIGHT_ALT_CLOUDY_WINDY,
        WeatherIcons.DAY_SUNNY_OVERCAST,
        WeatherIcons.NIGHT_OVERCAST,
        WeatherIcons.OVERCAST -> {
            if (wm.isNight(this))
                0xFF182020.toInt()
            else
                0xFF5080A8.toInt()
        }


        WeatherIcons.DAY_PARTLY_CLOUDY,
        WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> {
            if (wm.isNight(this))
                0xFF181820.toInt()
            else
                0xFF256AAD.toInt()
        }

        WeatherIcons.DAY_SUNNY,
        WeatherIcons.NA,
        WeatherIcons.NIGHT_CLEAR,
        WeatherIcons.SNOWFLAKE_COLD,
        WeatherIcons.DAY_HOT,
        WeatherIcons.NIGHT_HOT,
        WeatherIcons.HOT,
        WeatherIcons.DAY_WINDY,
        WeatherIcons.NIGHT_WINDY,
        WeatherIcons.WINDY,
        WeatherIcons.DAY_LIGHT_WIND,
        WeatherIcons.NIGHT_LIGHT_WIND,
        WeatherIcons.LIGHT_WIND,
        WeatherIcons.STRONG_WIND -> {
            // Set background based using sunset/rise times
            if (wm.isNight(this))
                0xFF181018.toInt()
            else
                0xFF20A8D8.toInt()
        }
        else -> {
            // Set background based using sunset/rise times
            if (wm.isNight(this))
                0xFF181018.toInt()
            else
                0xFF20A8D8.toInt()
        }
    }

    return rgb
}

suspend fun ImageData.isImageValid(): Boolean {
    val imgData = this
    return withContext(Dispatchers.IO) {
        val ctx = appLib.context

        if (imgData.isValid(ctx)) {
            val uri = Uri.parse(imgData.imageURL)
            if ("file" == uri.scheme) {
                val stream = uri.path?.let {
                    try {
                        if (it.startsWith("/android_asset")) {
                            val startAsset = it.indexOf("/android_asset/")
                            val path = it.substring(startAsset + 15)
                            BufferedInputStream(ctx.resources.assets.open(path))
                        } else {
                            val file = File(it)

                            while (FileUtils.isFileLocked(file)) {
                                delay(250)
                            }

                            BufferedInputStream(FileInputStream(file))
                        }
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e, "ImageData: unable to open file")
                        null
                    }
                }
                return@withContext stream?.use {
                    ImageUtils.guessImageType(it) != ImageUtils.ImageType.UNKNOWN
                } ?: false
            } else {
                return@withContext true
            }
        } else {
            return@withContext false
        }
    }
}