package com.thewizrd.shared_resources.weatherdata.images;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.FirebaseHelper;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherBackground;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ImageDataHelperImplApp extends ImageDataHelperImpl {
    // Shared Preferences
    private SharedPreferences imageDataPrefs;

    // App data files
    private File cacheDataFolder;
    private File imageDataFolder;

    public ImageDataHelperImplApp() {
        super();

        imageDataPrefs = SimpleLibrary.getInstance()
                .getAppContext().getSharedPreferences("images", Context.MODE_PRIVATE);

        cacheDataFolder = SimpleLibrary.getInstance().getAppContext().getCacheDir();
        imageDataFolder = new File(cacheDataFolder, "images");
        imageDataFolder.mkdir();
    }

    @Override
    public ImageData getCachedImageData(String backgroundCode) {
        if (imageDataPrefs.contains(backgroundCode)) {
            return JSONParser.deserializer(
                    imageDataPrefs.getString(backgroundCode, null), ImageData.class);
        }

        return null;
    }

    @Override
    protected ImageData storeImage(final Uri imageUri, final ImageData imageData) {
        return new AsyncTask<ImageData>().await(new Callable<ImageData>() {
            @Override
            public ImageData call() {
                if (!imageDataFolder.exists())
                    imageDataFolder.mkdir();

                FirebaseStorage storage = FirebaseHelper.getFirebaseStorage();
                StorageReference storageRef = storage.getReferenceFromUrl(imageUri.toString());
                File imageFile = new File(imageDataFolder,
                        String.format("%s-%s", imageData.getCondition(), UUID.randomUUID().toString()));

                try {
                    Tasks.await(storageRef.getFile(imageFile));
                } catch (ExecutionException | InterruptedException e) {
                    Logger.writeLine(Log.ERROR, e, "ImageDataHelper: Error retrieving download url");
                    imageFile.delete();
                    return null;
                }

                ImageData newImageData = ImageData.copyWithNewImageUrl(imageData, Uri.fromFile(imageFile).toString());

                imageDataPrefs.edit().putString(imageData.getCondition(),
                        JSONParser.serializer(newImageData, ImageData.class)).apply();

                return newImageData;
            }
        });
    }

    @Override
    public void clearCachedImageData() {
        if (imageDataFolder.exists())
            imageDataFolder.delete();

        imageDataPrefs.edit().clear().apply();
    }

    @Override
    public ImageData getDefaultImageData(String backgroundCode, Weather weather) {
        WeatherManager wm = WeatherManager.getInstance();

        // Fallback to assets
        // day, night, rain, snow
        ImageData imageData = new ImageData();
        switch (backgroundCode) {
            case WeatherBackground.SNOW:
            case WeatherBackground.SNOW_WINDY:
                imageData.setImageURL("file:///android_asset/backgrounds/snow.jpg");
                imageData.setColor("#ffb8d0f0");
                break;
            case WeatherBackground.RAIN:
            case WeatherBackground.RAIN_NIGHT:
                imageData.setImageURL("file:///android_asset/backgrounds/rain.jpg");
                imageData.setColor("#ff102030");
                break;
            case WeatherBackground.TSTORMS_DAY:
            case WeatherBackground.TSTORMS_NIGHT:
            case WeatherBackground.STORMS:
                imageData.setImageURL("file:///android_asset/backgrounds/storms.jpg");
                imageData.setColor("#ff182830");
                break;
            default:
                if (wm.isNight(weather)) {
                    imageData.setImageURL("file:///android_asset/backgrounds/night.jpg");
                    imageData.setColor("#ff182020");
                } else {
                    imageData.setImageURL("file:///android_asset/backgrounds/day.jpg");
                    imageData.setColor("#ff88b0c8");
                }
                break;
        }

        return imageData;
    }
}
