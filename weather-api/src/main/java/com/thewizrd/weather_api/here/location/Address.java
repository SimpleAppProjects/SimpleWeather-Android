package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Address {

    @Json(name = "additionalData")
    private List<AdditionalDataItem> additionalData;

    @Json(name = "houseNumber")
    private String houseNumber;

    @Json(name = "state")
    private String state;

    @Json(name = "label")
    private String label;

    @Json(name = "country")
    private String country;

    @Json(name = "street")
    private String street;

    @Json(name = "postalCode")
    private String postalCode;

    @Json(name = "city")
    private String city;

    @Json(name = "county")
    private String county;

    @Json(name = "district")
    private String district;

    public void setAdditionalData(List<AdditionalDataItem> additionalData) {
        this.additionalData = additionalData;
    }

    public List<AdditionalDataItem> getAdditionalData() {
        return additionalData;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet() {
        return street;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCounty() {
        return county;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDistrict() {
        return district;
    }
}