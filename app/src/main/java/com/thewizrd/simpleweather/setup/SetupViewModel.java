package com.thewizrd.simpleweather.setup;

import androidx.lifecycle.ViewModel;

import com.thewizrd.shared_resources.locationdata.LocationData;

public class SetupViewModel extends ViewModel {
    private LocationData locationData;

    public SetupViewModel() {
        locationData = null;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public void setLocationData(LocationData locationData) {
        this.locationData = locationData;
    }
}
