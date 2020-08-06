package com.thewizrd.shared_resources.weatherdata.images;

import android.net.Uri;

import androidx.annotation.WorkerThread;

import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

public abstract class ImageDataHelperImpl {
    @WorkerThread
    public abstract ImageData getCachedImageData(String backgroundCode);

    @WorkerThread
    public ImageData getRemoteImageData(String backgroundCode) {
        ImageData imageData = ImageDatabase.getRandomImageForCondition(backgroundCode);

        if (imageData != null && imageData.isValid()) {
            ImageData cachedImage = cacheImage(imageData);
            return cachedImage;
        }

        return null;
    }

    @WorkerThread
    public ImageData cacheImage(ImageData imageData) {
        Uri imageUri = Uri.parse(imageData.getImageURL());
        if ("gs".equals(imageUri.getScheme()) || "https".equals(imageUri.getScheme()) || "http".equals(imageUri.getScheme())) {
            // Download image to storage
            // and image metadata to settings
            ImageData cachedImage = storeImage(imageUri, imageData);
            return cachedImage;
        }

        // Invalid image uri
        return null;
    }

    @WorkerThread
    protected abstract ImageData storeImage(Uri imageUri, ImageData imageData);

    @WorkerThread
    public abstract void clearCachedImageData();

    @WorkerThread
    public abstract ImageData getDefaultImageData(String backgroundCode, Weather weather);

    @WorkerThread
    public abstract boolean isEmpty();
}
