package com.thewizrd.shared_resources.firebase;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Logger;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FirebaseHelper {
    private static boolean sHasSignInFailed;

    public static FirebaseFirestore getFirestoreDB() {
        checkSignIn();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .setPersistenceEnabled(true)
                .setSslEnabled(true)
                .build());
        return db;
    }

    public static FirebaseStorage getFirebaseStorage() {
        checkSignIn();
        FirebaseStorage stor = FirebaseStorage.getInstance();
        stor.setMaxDownloadRetryTimeMillis(Duration.ofHours(1).toMillis());
        return stor;
    }

    public static FirebaseDatabase getFirebaseDB() {
        checkSignIn();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.setPersistenceEnabled(true);
        db.setPersistenceCacheSizeBytes(2 * 1024 * 1024); // 2 MB
        return FirebaseDatabase.getInstance();
    }

    public static String getAccessToken() {
        checkSignIn();
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        return AsyncTask.await(new Callable<String>() {
            @Override
            public String call() {
                if (auth.getCurrentUser() != null) {
                    String token = null;
                    try {
                        token = Tasks.await(auth.getCurrentUser().getIdToken(true)).getToken();
                    } catch (Exception e) {
                        Logger.writeLine(Log.DEBUG, "Error getting user token");
                    }

                    return token;
                }
                return null;
            }
        });
    }

    private static void checkSignIn() {
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null && !sHasSignInFailed) {
            AsyncTask.await(new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        Tasks.await(auth.signInAnonymously(), 15, TimeUnit.SECONDS);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        Logger.writeLine(Log.ERROR, e, "Firebase: failed to sign in");
                        if (e instanceof TimeoutException) {
                            sHasSignInFailed = true;
                        }
                    }
                    return null;
                }
            });
        }
    }
}
