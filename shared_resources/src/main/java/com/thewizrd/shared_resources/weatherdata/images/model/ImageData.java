package com.thewizrd.shared_resources.weatherdata.images.model;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.File;

@Entity(tableName = "imagedata")
public class ImageData {
    @PrimaryKey
    private String documentId;

    private String artistName;
    private String hexColor;
    private String condition;
    private String imageURL;
    private String location;
    private String originalLink;
    private String siteName;

    public boolean isValid() {
        if (imageURL != null && !StringUtils.isNullOrWhitespace(hexColor)) {
            Uri uri = Uri.parse(imageURL);

            if ("file".equals(uri.getScheme())) {
                return uri.getPath() != null && new File(uri.getPath()).exists();
            } else {
                return uri.isAbsolute();
            }
        }

        return false;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getColor() {
        return hexColor;
    }

    public void setColor(String color) {
        this.hexColor = color;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOriginalLink() {
        return originalLink;
    }

    public void setOriginalLink(String originalLink) {
        this.originalLink = originalLink;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public static ImageData copyWithNewImageUrl(@NonNull ImageData old, @NonNull String newImagePath) {
        ImageData newData = new ImageData();

        newData.artistName = old.artistName;
        newData.condition = old.condition;
        newData.documentId = old.documentId;
        newData.hexColor = old.hexColor;
        newData.imageURL = newImagePath;
        newData.location = old.location;
        newData.originalLink = old.originalLink;
        newData.siteName = old.siteName;

        return newData;
    }
}
