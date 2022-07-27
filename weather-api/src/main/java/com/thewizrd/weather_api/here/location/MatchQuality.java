package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class MatchQuality {

    @Json(name = "houseNumber")
    private double houseNumber;

    @Json(name = "state")
    private double state;

    @Json(name = "country")
    private double country;

    @Json(name = "street")
    private List<Double> street;

    @Json(name = "postalCode")
    private double postalCode;

    @Json(name = "city")
    private double city;

    @Json(name = "county")
    private double county;

    @Json(name = "district")
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