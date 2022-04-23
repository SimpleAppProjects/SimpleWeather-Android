package com.thewizrd.simpleweather.images

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import com.thewizrd.shared_resources.appLib
import com.thewizrd.simpleweather.images.model.ImageData

abstract class BaseImageDataService : ImageDataService {
    private val preferences = appLib.preferences

    @WorkerThread
    protected abstract suspend fun storeImage(imageUri: Uri, imageData: ImageData): ImageData?

    override fun getImageDBUpdateTime(): Long {
        return preferences.getString("ImageDB_LastUpdated", "0")!!.toLong()
    }

    override fun setImageDBUpdateTime(value: Long) {
        preferences.edit {
            putString("ImageDB_LastUpdated", value.toString())
        }
    }

    override fun shouldInvalidateCache(): Boolean {
        return preferences.getBoolean("ImageDB_Invalidate", false)
    }

    override fun invalidateCache(value: Boolean) {
        preferences.edit {
            putBoolean("ImageDB_Invalidate", value)
        }
    }
}