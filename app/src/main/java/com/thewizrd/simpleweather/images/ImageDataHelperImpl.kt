package com.thewizrd.simpleweather.images

import android.net.Uri
import androidx.annotation.WorkerThread
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.simpleweather.images.model.ImageData

abstract class ImageDataHelperImpl {
    @WorkerThread
    abstract suspend fun getCachedImageData(backgroundCode: String?): ImageData?

    @WorkerThread
    abstract suspend fun getRemoteImageData(backgroundCode: String?): ImageData?

    @WorkerThread
    abstract suspend fun cacheImage(imageData: ImageData): ImageData?

    @WorkerThread
    protected abstract suspend fun storeImage(imageUri: Uri, imageData: ImageData): ImageData?

    @WorkerThread
    abstract suspend fun clearCachedImageData()

    @WorkerThread
    abstract suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData?

    @get:WorkerThread
    abstract val isEmpty: Boolean
}