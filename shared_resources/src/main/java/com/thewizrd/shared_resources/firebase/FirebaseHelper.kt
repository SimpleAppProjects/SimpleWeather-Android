package com.thewizrd.shared_resources.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.Duration

object FirebaseHelper {
    private var sHasSignInFailed = false

    @JvmStatic
    fun getFirestoreDB(): FirebaseFirestore {
        runBlocking {
            checkSignIn()
        }
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .setPersistenceEnabled(true)
                .setSslEnabled(true)
                .build()
        return db
    }

    @JvmStatic
    fun getFirebaseStorage(): FirebaseStorage {
        runBlocking {
            checkSignIn()
        }
        val stor = FirebaseStorage.getInstance()
        stor.maxDownloadRetryTimeMillis = Duration.ofHours(1).toMillis()
        return stor
    }

    @JvmStatic
    fun getFirebaseDB(): FirebaseDatabase {
        runBlocking {
            checkSignIn()
        }
        val db = FirebaseDatabase.getInstance()
        db.setPersistenceEnabled(true)
        db.setPersistenceCacheSizeBytes((2 * 1024 * 1024).toLong()) // 2 MB
        return FirebaseDatabase.getInstance()
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        checkSignIn()

        val auth = FirebaseAuth.getInstance()

        var token: String? = null
        try {
            token = auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Logger.writeLine(Log.DEBUG, "Error getting user token")
        }

        return@withContext token
    }

    private suspend fun checkSignIn() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null && !sHasSignInFailed) {
            supervisorScope {
                try {
                    withTimeout(15000 /* 15s */) {
                        auth.signInAnonymously().await()
                    }
                } catch (e: Exception) {
                    if (e is TimeoutCancellationException) {
                        sHasSignInFailed = true
                    }
                    null
                }
            }
        }
    }
}