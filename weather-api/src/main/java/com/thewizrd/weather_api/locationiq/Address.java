package com.thewizrd.weather_api.locationiq;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Address {

    @Json(name = "country")
    private String country;

    @Json(name = "country_code")
    private String countryCode;

    @Json(name = "road")
    private String road;

    @Json(name = "city")
    private String city;

    @Json(name = "county")
    private String county;

    @Json(name = "postcode")
    private String postcode;

    @Json(name = "suburb")
    private String suburb;

    @Json(name = "house_number")
    private String houseNumber;

    @Json(name = "state")
    private String state;

    @Json(name = "city_district")
    private String cityDistrict;

    @Json(name = "house")
    private String house;

    @Json(name = "neighbourhood")
    private String neighbourhood;

    @Json(name = "hamlet")
    private String hamlet;

    @Json(name = "village")
    private String village;

    @Json(name = "town")
    private String town;

    @Json(name = "region")
    private String region;

    @Json(name = "state_district")
    private String stateDistrict;

    @Json(name = "name")
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