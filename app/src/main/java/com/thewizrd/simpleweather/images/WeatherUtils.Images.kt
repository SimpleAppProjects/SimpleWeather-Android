@file:JvmMultifileClass
@file:JvmName("WeatherUtils")

package com.thewizrd.simpleweather.images

import android.net.Uri
import android.util.Log
import androidx.annotation.ColorInt
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.FileUtils
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherBackground
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.images.model.ImageData
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
    val wm = WeatherManager.instance

    // Apply background based on weather condition
    backgroundCode = when (icon) {
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
        WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
        WeatherIcons.NIGHT_ALT_SLEET_STORM,
        WeatherIcons.HAIL,
        WeatherIcons.HURRICANE,
        WeatherIcons.TORNADO -> WeatherBackground.STORMS

        WeatherIcons.DUST, WeatherIcons.SANDSTORM -> WeatherBackground.DUST

        WeatherIcons.DAY_FOG,
        WeatherIcons.DAY_HAZE,
        WeatherIcons.FOG,
        WeatherIcons.NIGHT_FOG,
        WeatherIcons.SMOG,
        WeatherIcons.SMOKE -> WeatherBackground.FOG

        WeatherIcons.DAY_SNOW,
        WeatherIcons.DAY_SNOW_THUNDERSTORM,
        WeatherIcons.NIGHT_ALT_SNOW,
        WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
        WeatherIcons.SNOW,
        WeatherIcons.SNOW_WIND,
        WeatherIcons.DAY_SNOW_WIND,
        WeatherIcons.NIGHT_ALT_SNOW_WIND -> WeatherBackground.SNOW

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
        WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> {
            if (wm.isNight(this))
                WeatherBackground.MOSTLYCLOUDY_NIGHT
            else
                WeatherBackground.MOSTLYCLOUDY_DAY
        }

        WeatherIcons.DAY_SUNNY_OVERCAST, WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> {
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
        WeatherIcons.WINDY,
        WeatherIcons.STRONG_WIND -> {
            // Set background based using sunset/rise times
            if (wm.isNight(this)) WeatherBackground.NIGHT else WeatherBackground.DAY
        }
        else -> {
            if (wm.isNight(this)) WeatherBackground.NIGHT else WeatherBackground.DAY
        }
    }

    // Check cache for image data
    val imageHelper = ImageDataHelper.getImageDataHelper()
    var imageData = imageHelper.getCachedImageData(backgroundCode)
    // Check if cache is available and valid
    val imgDataValid = imageData != null && imageData.isValid
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

        if (!FeatureSettings.isUpdateAvailable()) {
            imageData = withTimeoutOrNull(15000) {
                imageHelper.getRemoteImageData(backgroundCode)
            }
            if (imageData?.isValid == true) {
                return ImageDataViewModel(imageData)
            } else {
                imageData = imageHelper.getDefaultImageData(backgroundCode, this)
                if (imageData?.isValid == true)
                    return ImageDataViewModel(imageData)
            }
        } else {
            imageData = imageHelper.getDefaultImageData(backgroundCode, this)
            if (imageData?.isValid == true)
                return ImageDataViewModel(imageData)
        }
    }

    return null
}

@ColorInt
fun Weather.getBackgroundColor(): Int {
    var rgb = -1
    val icon: String = this.condition.icon
    val wm = WeatherManager.instance

    // Apply background based on weather condition
    rgb = when (icon) {
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

        WeatherIcons.DAY_LIGHTNING, WeatherIcons.DAY_THUNDERSTORM -> 0xFF283848.toInt()

        WeatherIcons.NIGHT_ALT_LIGHTNING,
        WeatherIcons.NIGHT_ALT_THUNDERSTORM,
        WeatherIcons.LIGHTNING,
        WeatherIcons.THUNDERSTORM -> 0xFF181830.toInt()

        WeatherIcons.DAY_STORM_SHOWERS,
        WeatherIcons.DAY_SLEET_STORM,
        WeatherIcons.STORM_SHOWERS,
        WeatherIcons.NIGHT_ALT_STORM_SHOWERS,
        WeatherIcons.NIGHT_ALT_SLEET_STORM,
        WeatherIcons.HAIL,
        WeatherIcons.HURRICANE,
        WeatherIcons.TORNADO -> 0xFF182830.toInt()

        WeatherIcons.DUST, WeatherIcons.SANDSTORM -> 0xFFB06810.toInt()

        WeatherIcons.DAY_FOG,
        WeatherIcons.DAY_HAZE,
        WeatherIcons.FOG,
        WeatherIcons.NIGHT_FOG,
        WeatherIcons.SMOG,
        WeatherIcons.SMOKE -> 0xFF252524.toInt()

        WeatherIcons.DAY_SNOW,
        WeatherIcons.DAY_SNOW_THUNDERSTORM,
        WeatherIcons.NIGHT_ALT_SNOW,
        WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM,
        WeatherIcons.SNOW -> 0xFF646464.toInt()

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
        WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> {
            if (wm.isNight(this))
                0xFF182020.toInt()
            else
                0xFF5080A8.toInt()
        }

        WeatherIcons.DAY_SUNNY_OVERCAST, WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> {
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
        WeatherIcons.WINDY,
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
        if (imgData.isValid) {
            val uri = Uri.parse(imgData.imageURL)
            if ("file" == uri.scheme) {
                val stream = uri.path?.let {
                    try {
                        if (it.startsWith("/android_asset")) {
                            val startAsset = it.indexOf("/android_asset/")
                            val path = it.substring(startAsset + 15)
                            val ctx = SimpleLibrary.instance.appContext
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