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
import java.util.concurrent.TimeUnit

object FirebaseHelper {
    private var sHasSignInFailed = false
    private var sSetupFirebaseDB = false

    suspend fun getFirestoreDB(): FirebaseFirestore {
        checkSignIn()

        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .setPersistenceEnabled(true)
            .setSslEnabled(true)
            .build()
        return db
    }

    suspend fun getFirebaseStorage(): FirebaseStorage {
        checkSignIn()

        val stor = FirebaseStorage.getInstance()
        stor.maxDownloadRetryTimeMillis = TimeUnit.HOURS.toMillis(1)
        return stor
    }

    suspend fun getFirebaseDB(): FirebaseDatabase {
        checkSignIn()

        val db = FirebaseDatabase.getInstance()
        if (!sSetupFirebaseDB) {
            db.setPersistenceEnabled(true)
            db.setPersistenceCacheSizeBytes((2 * 1024 * 1024).toLong()) // 2 MB
            sSetupFirebaseDB = true
        }
        return db
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