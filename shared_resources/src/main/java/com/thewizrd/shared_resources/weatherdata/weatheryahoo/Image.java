package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

public class Image {

    @SerializedName("width")
    private String width;

    @SerializedName("link")
    private String link;

    @SerializedName("title")
    private String title;

    @SerializedName("url")
    private String url;

    @SerializedName("height")
    private String height;

    public void setWidth(String width) {
        this.width = width;
    }

    public String getWidth() {
        return width;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }
}