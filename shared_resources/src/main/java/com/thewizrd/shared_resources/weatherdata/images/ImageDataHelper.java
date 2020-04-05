package com.thewizrd.shared_resources.weatherdata.images;

public class ImageDataHelper {
    private static ImageDataHelperImpl sImageDataHelper;

    public static ImageDataHelperImpl getImageDataHelper() {
        if (sImageDataHelper == null)
            sImageDataHelper = new ImageDataHelperImplApp();

        return sImageDataHelper;
    }
}