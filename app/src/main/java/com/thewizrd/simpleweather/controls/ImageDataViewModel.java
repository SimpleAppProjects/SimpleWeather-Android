package com.thewizrd.simpleweather.controls;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;

import com.thewizrd.simpleweather.images.model.ImageData;

public class ImageDataViewModel {
    private String artistName;
    private @ColorInt
    int color;
    private String imageURI;
    private String siteName;
    private String originalLink;

    @RestrictTo({RestrictTo.Scope.LIBRARY, RestrictTo.Scope.TESTS})
    public ImageDataViewModel() {
        // Needed for deserialization
    }

    public ImageDataViewModel(ImageData imageData) {
        this.artistName = imageData.getArtistName();
        this.color = Color.parseColor(imageData.getColor());
        this.imageURI = imageData.getImageURL();
        this.siteName = imageData.getSiteName();
        this.originalLink = imageData.getOriginalLink();
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    @ColorInt
    public int getColor() {
        return color;
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
    }

    public String getImageURI() {
        return imageURI;
    }

    public void setImageURI(String imageURI) {
        this.imageURI = imageURI;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getOriginalLink() {
        return originalLink;
    }

    public void setOriginalLink(String originalLink) {
        this.originalLink = originalLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageDataViewModel that = (ImageDataViewModel) o;

        if (color != that.color) return false;
        if (artistName != null ? !artistName.equals(that.artistName) : that.artistName != null)
            return false;
        if (imageURI != null ? !imageURI.equals(that.imageURI) : that.imageURI != null)
            return false;
        if (siteName != null ? !siteName.equals(that.siteName) : that.siteName != null)
            return false;
        return originalLink != null ? originalLink.equals(that.originalLink) : that.originalLink == null;
    }

    @Override
    public int hashCode() {
        int result = artistName != null ? artistName.hashCode() : 0;
        result = 31 * result + color;
        result = 31 * result + (imageURI != null ? imageURI.hashCode() : 0);
        result = 31 * result + (siteName != null ? siteName.hashCode() : 0);
        result = 31 * result + (originalLink != null ? originalLink.hashCode() : 0);
        return result;
    }
}
