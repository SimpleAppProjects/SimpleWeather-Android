package com.thewizrd.shared_resources.weatherdata.images;

import android.net.Uri;

import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

public abstract class ImageDataHelperImpl {
    public abstract ImageData getCachedImageData(String backgroundCode);

    public ImageData getRemoteImageData(String backgroundCode) {
        ImageData imageData = ImageDatabase.getRandomImageForCondition(backgroundCode);

        if (imageData != null && imageData.isValid()) {
            ImageData cachedImage = cacheImage(imageData);
            return cachedImage;
        }

        return null;
    }

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

    protected abstract ImageData storeImage(Uri imageUri, ImageData imageData);

    public abstract void clearCachedImageData();

    public abstract ImageData getDefaultImageData(String backgroundCode, Weather weather);

    public abstract boolean isEmpty();
}
