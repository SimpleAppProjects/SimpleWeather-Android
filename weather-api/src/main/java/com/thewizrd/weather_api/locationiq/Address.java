package com.thewizrd.weather_api.locationiq;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Address {

    @SerializedName("country")
    private String country;

    @SerializedName("country_code")
    private String countryCode;

    @SerializedName("road")
    private String road;

    @SerializedName("city")
    private String city;

    @SerializedName("county")
    private String county;

    @SerializedName("postcode")
    private String postcode;

    @SerializedName("suburb")
    private String suburb;

    @SerializedName("house_number")
    private String houseNumber;

    @SerializedName("state")
    private String state;

    @SerializedName("city_district")
    private String cityDistrict;

    @SerializedName("house")
    private String house;

    @SerializedName("neighbourhood")
    private String neighbourhood;

    @SerializedName("hamlet")
    private String hamlet;

    @SerializedName("village")
    private String village;

    @SerializedName("town")
    private String town;

    @SerializedName("region")
    private String region;

    @SerializedName("state_district")
    private String stateDistrict;

    @SerializedName("name")
    private String name;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setRoad(String road) {
        this.road = road;
    }

    public String getRoad() {
        return road;
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

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    public String getSuburb() {
        return suburb;
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

    public void setCityDistrict(String cityDistrict) {
        this.cityDistrict = cityDistrict;
    }

    public String getCityDistrict() {
        return cityDistrict;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getHouse() {
        return house;
    }

    public String getNeighbourhood() {
        return neighbourhood;
    }

    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    public String getHamlet() {
        return hamlet;
    }

    public void setHamlet(String hamlet) {
        this.hamlet = hamlet;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStateDistrict() {
        return stateDistrict;
    }

    public void setStateDistrict(String stateDistrict) {
        this.stateDistrict = stateDistrict;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}