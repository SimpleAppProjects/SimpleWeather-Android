package com.thewizrd.shared_resources.weatherdata.nws.hourly;

import java.util.List;

public class HourlyForecastResponse {
    private String creationDate;

    private Location location;

    private List<PeriodsItem> periodsItems;

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<PeriodsItem> getPeriodsItems() {
        return periodsItems;
    }

    public void setPeriodsItems(List<PeriodsItem> periodsItems) {
        this.periodsItems = periodsItems;
    }
}