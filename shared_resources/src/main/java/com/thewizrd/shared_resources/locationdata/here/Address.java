package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Address {

    @SerializedName("additionalData")
    private List<AdditionalDataItem> additionalData;

    @SerializedName("houseNumber")
    private String houseNumber;

    @SerializedName("state")
    private String state;

    @SerializedName("label")
    private String label;

    @SerializedName("country")
    private String country;

    @SerializedName("street")
    private String street;

    @SerializedName("postalCode")
    private String postalCode;

    @SerializedName("city")
    private String city;

    @SerializedName("county")
    private String county;

    @SerializedName("district")
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