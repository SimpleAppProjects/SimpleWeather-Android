package com.thewizrd.shared_resources.weatherdata.images;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.FirebaseHelper;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ImageDatabase {
    @WorkerThread
    public static List<ImageData> getAllImageDataForCondition(final String backgroundCode) {
        return new AsyncTask<List<ImageData>>().await(new Callable<List<ImageData>>() {
            @Override
            public List<ImageData> call() {
                FirebaseFirestore db = FirebaseHelper.getFirestoreDB();
                Query query = db.collection("background_images")
                        .whereEqualTo("condition", backgroundCode);

                QuerySnapshot querySnapshot = null;
                try {
                    // Try to retrieve from cache first
                    if (!ImageDataHelper.shouldInvalidateCache()) {
                        querySnapshot = Tasks.await(query.get(Source.CACHE));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    querySnapshot = null;
                }

                // If data is missing from cache, get data from server
                if (querySnapshot == null) {
                    try {
                        querySnapshot = Tasks.await(query.get(Source.SERVER));

                        // Run query to cache data
                        saveSnapshot(db);
                    } catch (ExecutionException | InterruptedException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                }

                List<ImageData> list = new ArrayList<>();
                try {
                    if (querySnapshot != null) {
                        for (DocumentSnapshot docSnapshot : querySnapshot.getDocuments()) {
                            if (docSnapshot.exists()) {
                                list.add(docSnapshot.toObject(ImageData.class));
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.writeLine(Log.ERROR, e);
                }

                return list;
            }
        });
    }

    @WorkerThread
    public static ImageData getRandomImageForCondition(final String backgroundCode) {
        return new AsyncTask<ImageData>().await(new Callable<ImageData>() {
            @Override
            public ImageData call() {
                FirebaseFirestore db = FirebaseHelper.getFirestoreDB();
                Query query = db.collection("background_images")
                        .whereEqualTo("condition", backgroundCode);

                QuerySnapshot querySnapshot = null;
                try {
                    // Try to retrieve from cache first
                    if (!ImageDataHelper.shouldInvalidateCache()) {
                        querySnapshot = Tasks.await(query.get(Source.CACHE));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    querySnapshot = null;
                }

                // If data is missing from cache, get data from server
                if (querySnapshot == null || !querySnapshot.iterator().hasNext()) {
                    try {
                        querySnapshot = Tasks.await(query.get(Source.SERVER));

                        // Run query to cache data
                        saveSnapshot(db);
                    } catch (ExecutionException | InterruptedException e) {
                        Logger.writeLine(Log.ERROR, e);
                    }
                }

                final Random rand = new Random();
                if (querySnapshot != null) {
                    List<DocumentSnapshot> collection = querySnapshot.getDocuments();
                    Collections.sort(collection, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot o1, DocumentSnapshot o2) {
                            return rand.nextInt(1 - (-1)) + -1;
                        }
                    });

                    if (collection.size() > 0) {
                        DocumentSnapshot docSnapshot = collection.get(0);
                        ImageData imageData = null;
                        try {
                            imageData = docSnapshot.toObject(ImageData.class);
                        } catch (Exception e) {
                            Logger.writeLine(Log.ERROR, e);
                        }
                        return imageData;
                    }
                }

                return null;
            }
        });
    }

    @WorkerThread
    private static void saveSnapshot(@NonNull final FirebaseFirestore db) {
        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                // Get all data from server to cache locally
                Query query = db.collection("background_images");
                try {
                    Tasks.await(query.get(Source.SERVER));
                } catch (ExecutionException | InterruptedException e) {
                    Logger.writeLine(Log.ERROR, e);
                }

                // Register worker
                ApplicationLib app = SimpleLibrary.getInstance().getApp();
                if (app.isPhone()) {
                    LocalBroadcastManager.getInstance(app.getAppContext())
                            .sendBroadcast(new Intent(CommonActions.ACTION_IMAGES_UPDATEWORKER));
                }
                return null;
            }
        });
    }

    @WorkerThread
    public static long getLastUpdateTime() {
        return new AsyncTask<Long>().await(new Callable<Long>() {
            @Override
            public Long call() {
                FirebaseFirestore db = FirebaseHelper.getFirestoreDB();
                DocumentReference docRef = db.collection("background_images_info")
                        .document("collection_info");
                try {
                    DocumentSnapshot snapshot = Tasks.await(docRef.get(Source.DEFAULT));
                    Long updateTime = snapshot.getLong("last_updated");
                    if (updateTime != null) {
                        return updateTime;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Logger.writeLine(Log.ERROR, e);
                }

                return 0L;
            }
        });
    }
}
