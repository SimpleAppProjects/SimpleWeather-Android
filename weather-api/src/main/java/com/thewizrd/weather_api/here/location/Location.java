package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Location {

    @Json(name = "displayPosition")
    private DisplayPosition displayPosition;

    @Json(name = "address")
    private Address address;

    @Json(name = "adminInfo")
    private AdminInfo adminInfo;

    @Json(name = "locationId")
    private String locationId;

    @Json(name = "navigationPosition")
    private List<NavigationPositionItem> navigationPosition;

    @Json(name = "locationType")
    private String locationType;

    public void setDisplayPosition(DisplayPosition displayPosition) {
        this.displayPosition = displayPosition;
    }

    public DisplayPosition getDisplayPosition() {
        return displayPosition;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }

    public void setAdminInfo(AdminInfo adminInfo) {
        this.adminInfo = adminInfo;
    }

    public AdminInfo getAdminInfo() {
        return adminInfo;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setNavigationPosition(List<NavigationPositionItem> navigationPosition) {
        this.navigationPosition = navigationPosition;
    }

    public List<NavigationPositionItem> getNavigationPosition() {
        return navigationPosition;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getLocationType() {
        return locationType;
    }
}