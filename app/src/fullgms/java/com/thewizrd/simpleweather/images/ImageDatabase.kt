package com.thewizrd.simpleweather.images

import android.content.Intent
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.*
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.firebase.FirebaseHelper
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.images.model.ImageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

object ImageDatabase {
    private const val TAG = "ImageDatabase"

    @WorkerThread
    suspend fun getAllImageDataForCondition(backgroundCode: String?): List<ImageData> =
            withContext(Dispatchers.IO) {
                val db = FirebaseHelper.getFirestoreDB()
                val query = db.collection("background_images")
                        .whereEqualTo("condition", backgroundCode)

                var querySnapshot: QuerySnapshot? = null
                try {
                    // Try to retrieve from cache first
                    if (!ImageDataHelper.shouldInvalidateCache()) {
                        querySnapshot = query[Source.CACHE].await()
                    }
                } catch (e: Exception) {
                    querySnapshot = null
                }

                // If data is missing from cache, get data from server
                if (querySnapshot == null) {
                    try {
                        querySnapshot = query[Source.SERVER].await()

                        // Run query to cache data
                        saveSnapshot(db)
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e)
                    }
                }

                val list = ArrayList<ImageData>()
                try {
                    if (querySnapshot != null) {
                        list.ensureCapacity(querySnapshot.documents.size)
                        for (docSnapshot in querySnapshot.documents) {
                            if (docSnapshot.exists()) {
                                docSnapshot.toObject(ImageData::class.java)?.let {
                                    list.add(it)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.writeLine(Log.ERROR, e)
                }

                return@withContext list
            }

    @WorkerThread
    suspend fun getRandomImageForCondition(backgroundCode: String?): ImageData? =
            withContext(Dispatchers.IO) {
                val db = FirebaseHelper.getFirestoreDB()
                val query = db.collection("background_images")
                        .whereEqualTo("condition", backgroundCode)

                var querySnapshot: QuerySnapshot? = null
                try {
                    // Try to retrieve from cache first
                    if (!ImageDataHelper.shouldInvalidateCache()) {
                        querySnapshot = query[Source.CACHE].await()
                    }
                } catch (e: Exception) {
                    querySnapshot = null
                }

                // If data is missing from cache, get data from server
                if (querySnapshot == null || !querySnapshot.iterator().hasNext()) {
                    try {
                        querySnapshot = query[Source.SERVER].await()

                        // Run query to cache data
                        saveSnapshot(db)
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e)
                    }
                }

                if (querySnapshot != null) {
                    val randomDocSnapshot = querySnapshot.documents.randomOrNull()

                    if (randomDocSnapshot != null) {
                        var imageData: ImageData? = null
                        try {
                            imageData = randomDocSnapshot.toObject(ImageData::class.java)
                        } catch (e: Exception) {
                            Logger.writeLine(Log.ERROR, e)
                        }
                        return@withContext imageData
                    }
                }

                return@withContext null
            }

    @WorkerThread
    private suspend fun saveSnapshot(db: FirebaseFirestore) = withContext(Dispatchers.IO) {
        AnalyticsLogger.logEvent("$TAG: saveSnapshot")

        // Get all data from server to cache locally
        val query = db.collection("background_images")
        try {
            query[Source.SERVER].await()
            ImageDataHelper.invalidateCache(false)
            if (ImageDataHelper.getImageDBUpdateTime() == 0L) {
                ImageDataHelper.setImageDBUpdateTime(getLastUpdateTime())
            }
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
        }

        // Register worker
        val app = SimpleLibrary.instance.app
        if (app.isPhone) {
            LocalBroadcastManager.getInstance(app.appContext)
                    .sendBroadcast(Intent(CommonActions.ACTION_IMAGES_UPDATEWORKER))
        }
    }

    @WorkerThread
    suspend fun getLastUpdateTime(): Long = withContext(Dispatchers.IO) {
        val db = FirebaseHelper.getFirebaseDB()

        return@withContext suspendCancellableCoroutine { continuation ->
            val ref = db.reference.child("background_images_info")
                .child("collection_info").child("last_updated")

            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lastUpdated = snapshot.getValue(Long::class.java)
                    if (continuation.isActive)
                        continuation.resume(lastUpdated ?: 0L)
                }

                override fun onCancelled(error: DatabaseError) {
                    if (continuation.isActive)
                        continuation.resume(0L)
                }
            }

            ref.addListenerForSingleValueEvent(valueEventListener)

            continuation.invokeOnCancellation {
                ref.removeEventListener(valueEventListener)
            }
        }
    }
}