package com.thewizrd.shared_resources.weatherdata.images

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.firebase.FirebaseHelper
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherBackground
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class ImageDataHelperImplApp : ImageDataHelperImpl() {
    // Shared Preferences
    private val imageDataPrefs = SimpleLibrary.getInstance()
            .appContext.getSharedPreferences("images", Context.MODE_PRIVATE)

    private val imageDataFolder: File

    init {
        // App data files
        val cacheDataFolder = SimpleLibrary.getInstance().appContext.cacheDir
        imageDataFolder = File(cacheDataFolder, "images")
        imageDataFolder.mkdir()
    }

    override suspend fun getCachedImageData(backgroundCode: String?): ImageData? {
        return if (imageDataPrefs.contains(backgroundCode)) {
            withContext(Dispatchers.Default) {
                JSONParser.deserializer(
                        imageDataPrefs.getString(backgroundCode, null), ImageData::class.java)
            }
        } else {
            null
        }
    }

    override suspend fun storeImage(imageUri: Uri, imageData: ImageData): ImageData? =
            withContext(Dispatchers.IO) {
                if (!imageDataFolder.exists())
                    imageDataFolder.mkdir()

                val storage = FirebaseHelper.getFirebaseStorage()
                val storageRef = storage.getReferenceFromUrl(imageUri.toString())
                val imageFile = File(imageDataFolder, String.format("%s-%s", imageData.condition, UUID.randomUUID().toString()))

                try {
                    val args = Bundle()
                    args.putString("imageData", imageData.toString())
                    AnalyticsLogger.logEvent("ImageDataHelperImplApp: storeImage", args)

                    storageRef.getFile(imageFile).await()
                } catch (e: Exception) {
                    Logger.writeLine(Log.ERROR, e, "ImageDataHelper: Error retrieving download url")
                    imageFile.delete()
                    return@withContext null
                }

                val newImageData = ImageData.copyWithNewImageUrl(imageData, Uri.fromFile(imageFile).toString())

                imageDataPrefs.edit().putString(imageData.condition,
                        JSONParser.serializer(newImageData, ImageData::class.java)).apply()

                return@withContext newImageData
            }

    override suspend fun clearCachedImageData() {
        if (imageDataFolder.exists()) imageDataFolder.delete()
        imageDataPrefs.edit().clear().apply()
    }

    override suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData {
        val wm = WeatherManager.instance

        // Fallback to assets
        // day, night, rain, snow
        val imageData = ImageData()
        when (backgroundCode) {
            WeatherBackground.SNOW, WeatherBackground.SNOW_WINDY -> {
                imageData.imageURL = "file:///android_asset/backgrounds/snow.jpg"
                imageData.color = "#ffb8d0f0"
            }
            WeatherBackground.RAIN, WeatherBackground.RAIN_NIGHT -> {
                imageData.imageURL = "file:///android_asset/backgrounds/rain.jpg"
                imageData.color = "#ff102030"
            }
            WeatherBackground.TSTORMS_DAY, WeatherBackground.TSTORMS_NIGHT, WeatherBackground.STORMS -> {
                imageData.imageURL = "file:///android_asset/backgrounds/storms.jpg"
                imageData.color = "#ff182830"
            }
            else -> if (wm.isNight(weather)) {
                imageData.imageURL = "file:///android_asset/backgrounds/night.jpg"
                imageData.color = "#ff182020"
            } else {
                imageData.imageURL = "file:///android_asset/backgrounds/day.jpg"
                imageData.color = "#ff88b0c8"
            }
        }

        return imageData
    }

    override val isEmpty: Boolean
        get() = imageDataPrefs.all.isEmpty()
}