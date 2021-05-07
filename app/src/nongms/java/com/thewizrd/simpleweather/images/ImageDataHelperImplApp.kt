package com.thewizrd.simpleweather.images

import android.net.Uri
import androidx.annotation.WorkerThread
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.simpleweather.images.model.ImageData

class ImageDataHelperImplApp : ImageDataHelperImpl() {
    override suspend fun getCachedImageData(backgroundCode: String?): ImageData? {
        return null
    }

    @WorkerThread
    override suspend fun getRemoteImageData(backgroundCode: String?): ImageData? {
        return null
    }

    override suspend fun cacheImage(imageData: ImageData): ImageData {
        return imageData
    }

    override suspend fun storeImage(imageUri: Uri, imageData: ImageData): ImageData = imageData

    override suspend fun clearCachedImageData() {
        // no-op
    }

    override suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData {
        return ImageDataUtils.getDefaultImageData(backgroundCode, weather)
    }

    override val isEmpty: Boolean
        get() = false
}