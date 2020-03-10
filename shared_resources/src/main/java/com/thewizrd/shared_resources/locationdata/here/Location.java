package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Location {

    @SerializedName("displayPosition")
    private DisplayPosition displayPosition;

    @SerializedName("address")
    private Address address;

    @SerializedName("adminInfo")
    private AdminInfo adminInfo;

    @SerializedName("locationId")
    private String locationId;

    @SerializedName("navigationPosition")
    private List<NavigationPositionItem> navigationPosition;

    @SerializedName("locationType")
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