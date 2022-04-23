package com.thewizrd.simpleweather.images

import androidx.annotation.WorkerThread
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.images.model.ImageData

val imageDataService: ImageDataService by lazy { ImageDataServiceImpl() }

interface ImageDataService {
    @WorkerThread
    suspend fun getCachedImageData(backgroundCode: String?): ImageData?

    @WorkerThread
    suspend fun getRemoteImageData(backgroundCode: String?): ImageData?

    @WorkerThread
    suspend fun cacheImage(imageData: ImageData): ImageData?

    @WorkerThread
    suspend fun clearCachedImageData()

    @WorkerThread
    suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData?

    @get:WorkerThread
    val isEmpty: Boolean

    fun getImageDBUpdateTime(): Long
    fun setImageDBUpdateTime(value: Long)
    fun shouldInvalidateCache(): Boolean
    fun invalidateCache(value: Boolean)
}