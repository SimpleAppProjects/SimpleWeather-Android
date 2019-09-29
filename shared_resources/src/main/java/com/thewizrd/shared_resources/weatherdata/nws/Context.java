package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

public class Context {

    @SerializedName("wx")
    private String wx;

    @SerializedName("@vocab")
    private String vocab;

    @SerializedName("distance")
    private Distance distance;

    @SerializedName("city")
    private String city;

    @SerializedName("bearing")
    private Bearing bearing;

    @SerializedName("county")
    private County county;

    @SerializedName("geo")
    private String geo;

    @SerializedName("unit")
    private String unit;

    @SerializedName("forecastGridData")
    private ForecastGridData forecastGridData;

    @SerializedName("s")
    private String S;

    @SerializedName("publicZone")
    private PublicZone publicZone;

    @SerializedName("unitCode")
    private UnitCode unitCode;

    @SerializedName("forecastOffice")
    private ForecastOffice forecastOffice;

    @SerializedName("geometry")
    private Geometry geometry;

    @SerializedName("state")
    private String state;

    @SerializedName("value")
    private Value value;

    public void setWx(String wx) {
        this.wx = wx;
    }

    public String getWx() {
        return wx;
    }

    public void setVocab(String vocab) {
        this.vocab = vocab;
    }

    public String getVocab() {
        return vocab;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public Distance getDistance() {
        return distance;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setBearing(Bearing bearing) {
        this.bearing = bearing;
    }

    public Bearing getBearing() {
        return bearing;
    }

    public void setCounty(County county) {
        this.county = county;
    }

    public County getCounty() {
        return county;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getGeo() {
        return geo;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public void setForecastGridData(ForecastGridData forecastGridData) {
        this.forecastGridData = forecastGridData;
    }

    public ForecastGridData getForecastGridData() {
        return forecastGridData;
    }

    public void setS(String S) {
        this.S = S;
    }

    public String getS() {
        return S;
    }

    public void setPublicZone(PublicZone publicZone) {
        this.publicZone = publicZone;
    }

    public PublicZone getPublicZone() {
        return publicZone;
    }

    public void setUnitCode(UnitCode unitCode) {
        this.unitCode = unitCode;
    }

    public UnitCode getUnitCode() {
        return unitCode;
    }

    public void setForecastOffice(ForecastOffice forecastOffice) {
        this.forecastOffice = forecastOffice;
    }

    public ForecastOffice getForecastOffice() {
        return forecastOffice;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getValue() {
        return value;
    }
}