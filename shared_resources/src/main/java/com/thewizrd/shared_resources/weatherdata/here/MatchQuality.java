package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MatchQuality {

    @SerializedName("houseNumber")
    private double houseNumber;

    @SerializedName("state")
    private double state;

    @SerializedName("country")
    private double country;

    @SerializedName("street")
    private List<Double> street;

    @SerializedName("postalCode")
    private double postalCode;

    @SerializedName("city")
    private double city;

    @SerializedName("county")
    private double county;

    @SerializedName("district")
    private double district;

    public void setHouseNumber(double houseNumber) {
        this.houseNumber = houseNumber;
    }

    public double getHouseNumber() {
        return houseNumber;
    }

    public void setState(double state) {
        this.state = state;
    }

    public double getState() {
        return state;
    }

    public void setCountry(double country) {
        this.country = country;
    }

    public double getCountry() {
        return country;
    }

    public void setStreet(List<Double> street) {
        this.street = street;
    }

    public List<Double> getStreet() {
        return street;
    }

    public void setPostalCode(double postalCode) {
        this.postalCode = postalCode;
    }

    public double getPostalCode() {
        return postalCode;
    }

    public void setCity(double city) {
        this.city = city;
    }

    public double getCity() {
        return city;
    }

    public void setCounty(double county) {
        this.county = county;
    }

    public double getCounty() {
        return county;
    }

    public void setDistrict(double district) {
        this.district = district;
    }

    public double getDistrict() {
        return district;
    }
}