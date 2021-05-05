package com.thewizrd.shared_resources.weatherdata.images

import android.net.Uri
import androidx.annotation.WorkerThread
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData

abstract class ImageDataHelperImpl {
    @WorkerThread
    abstract suspend fun getCachedImageData(backgroundCode: String?): ImageData?

    @WorkerThread
    suspend fun getRemoteImageData(backgroundCode: String?): ImageData? {
        val imageData = ImageDatabase.getRandomImageForCondition(backgroundCode)

        return if (imageData?.isValid == true) {
            cacheImage(imageData)
        } else {
            null
        }
    }

    @WorkerThread
    suspend fun cacheImage(imageData: ImageData): ImageData? {
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

    @WorkerThread
    protected abstract suspend fun storeImage(imageUri: Uri, imageData: ImageData): ImageData?

    @WorkerThread
    abstract suspend fun clearCachedImageData()

    @WorkerThread
    abstract suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData?

    @get:WorkerThread
    abstract val isEmpty: Boolean
}