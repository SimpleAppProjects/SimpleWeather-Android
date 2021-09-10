package com.thewizrd.simpleweather.images

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.firebase.FirebaseHelper
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.images.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.*

class ImageDataHelperImplApp : ImageDataHelperImpl() {
    // Shared Preferences
    private val imageDataPrefs = SimpleLibrary.instance
            .appContext.getSharedPreferences("images", Context.MODE_PRIVATE)

    private val imageDataFolder: File

    init {
        // App data files
        val cacheDataFolder = SimpleLibrary.instance.appContext.cacheDir
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

    @WorkerThread
    override suspend fun getRemoteImageData(backgroundCode: String?): ImageData? {
        val imageData = if (ConnectivityUtils.isNetworkConnected()) {
            ImageDatabase.getRandomImageForCondition(backgroundCode)
        } else {
            Logger.writeLine(Log.INFO, "Network not connected!!")
            null
        }

        return if (imageData?.isValid == true) {
            cacheImage(imageData)
        } else {
            null
        }
    }

    @WorkerThread
    override suspend fun cacheImage(imageData: ImageData): ImageData? {
        val imageUri = Uri.parse(imageData.imageURL)
        return if ("gs" == imageUri.scheme || "https" == imageUri.scheme || "http" == imageUri.scheme) {
            // Download image to storage
            // and image metadata to settings
            storeImage(imageUri, imageData)
        } else {
            // Invalid image uri
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

                    withTimeoutOrNull(SettingsManager.CONNECTION_TIMEOUT.toLong()) {
                        storageRef.getFile(imageFile).await()
                    }
                } catch (e: Exception) {
                    Logger.writeLine(Log.ERROR, e, "ImageDataHelper: Error retrieving download url")
                    imageFile.delete()
                    return@withContext null
                }

                val newImageData =
                    ImageData.copyWithNewImageUrl(imageData, Uri.fromFile(imageFile).toString())

                // Check if stored image is a valid image
                if (!newImageData.isImageValid()) {
                    imageFile.delete()
                    return@withContext null
                }

                imageDataPrefs.edit {
                    putString(
                        imageData.condition,
                        JSONParser.serializer(newImageData, ImageData::class.java)
                    )
                }

                return@withContext newImageData
            }

    override suspend fun clearCachedImageData() {
        if (imageDataFolder.exists()) imageDataFolder.delete()
        imageDataPrefs.edit {
            clear()
        }
    }

    override suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData {
        return ImageDataUtils.getDefaultImageData(backgroundCode, weather)
    }

    override val isEmpty: Boolean
        get() = imageDataPrefs.all.isEmpty()
}