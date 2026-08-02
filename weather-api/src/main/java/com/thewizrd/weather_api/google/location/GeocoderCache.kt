package com.thewizrd.weather_api.google.location

import android.location.Address
import android.os.Parcel
import com.thewizrd.shared_resources.sharedDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.concurrent.TaskRunner
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal object GeocoderCache {
    private val cacheDir = File(sharedDeps.context.cacheDir, "geocoder_cache")
    private val diskCache: DiskLruCache by lazy {
        DiskLruCache(
            fileSystem = FileSystem.SYSTEM,
            directory = cacheDir.absolutePath.toPath(),
            appVersion = 1,
            valueCount = 1,
            maxSize = 1 * 1024 * 1024, // 1MB
            taskRunner = TaskRunner.INSTANCE
        )
    }

    private fun hashKey(key: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(key.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun get(key: String): List<Address>? = withContext(Dispatchers.IO) {
        val hashedKey = hashKey(key)
        try {
            diskCache[hashedKey]?.use { snapshot ->
                snapshot.getSource(0).buffer().use { source ->
                    val bytes = source.readByteArray()
                    val parcel = Parcel.obtain()
                    try {
                        parcel.unmarshall(bytes, 0, bytes.size)
                        parcel.setDataPosition(0)
                        val addresses = mutableListOf<Address>()
                        parcel.readTypedList(addresses, Address.CREATOR)
                        addresses
                    } finally {
                        parcel.recycle()
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun put(key: String, addresses: List<Address>) = withContext(Dispatchers.IO) {
        val hashedKey = hashKey(key)
        try {
            diskCache.edit(hashedKey)?.let { editor ->
                try {
                    editor.newSink(0).buffer().use { sink ->
                        val parcel = Parcel.obtain()
                        try {
                            parcel.writeTypedList(addresses)
                            val bytes = parcel.marshall()
                            sink.write(bytes)
                            editor.commit()
                        } finally {
                            parcel.recycle()
                        }
                    }
                } catch (_: Exception) {
                    editor.abort()
                }
            }
        } catch (_: IOException) {
            // ignore
        }
    }
}
