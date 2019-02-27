package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;

public class SuggestionsItem {

    @SerializedName("address")
    private Address address;

    @SerializedName("matchLevel")
    private String matchLevel;

    @SerializedName("countryCode")
    private String countryCode;

    @SerializedName("locationId")
    private String locationId;

    @SerializedName("language")
    private String language;

    @SerializedName("label")
    private String label;

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}