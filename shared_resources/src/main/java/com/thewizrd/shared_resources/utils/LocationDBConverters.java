package com.thewizrd.shared_resources.utils;

import androidx.room.TypeConverter;

import com.thewizrd.shared_resources.weatherdata.LocationType;

public class LocationDBConverters {

    @TypeConverter
    public static LocationType locationTypeFromInt(int value) {
        return LocationType.valueOf(value);
    }

    @TypeConverter
    public static int locationTypeToInt(LocationType value) {
        return value.getValue();
    }
}
