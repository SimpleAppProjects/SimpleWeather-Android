package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

public class Channel {

    @SerializedName("atmosphere")
    private Atmosphere atmosphere;

    @SerializedName("image")
    private Image image;

    @SerializedName("item")
    private Item item;

    @SerializedName("lastBuildDate")
    private String lastBuildDate;

    @SerializedName("link")
    private String link;

    @SerializedName("description")
    private String description;

    @SerializedName("language")
    private String language;

    @SerializedName("units")
    private Units units;

    @SerializedName("title")
    private String title;

    @SerializedName("astronomy")
    private Astronomy astronomy;

    @SerializedName("ttl")
    private String ttl;

    @SerializedName("location")
    private Location location;

    @SerializedName("wind")
    private Wind wind;

    public void setAtmosphere(Atmosphere atmosphere) {
        this.atmosphere = atmosphere;
    }

    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public void setLastBuildDate(String lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    public String getLastBuildDate() {
        return lastBuildDate;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public void setUnits(Units units) {
        this.units = units;
    }

    public Units getUnits() {
        return units;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setAstronomy(Astronomy astronomy) {
        this.astronomy = astronomy;
    }

    public Astronomy getAstronomy() {
        return astronomy;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public String getTtl() {
        return ttl;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public Wind getWind() {
        return wind;
    }

}