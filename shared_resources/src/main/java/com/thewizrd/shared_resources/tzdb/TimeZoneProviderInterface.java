package com.thewizrd.shared_resources.tzdb;

public interface TimeZoneProviderInterface {
    String getTimeZone(double latitude, final double longitude);
}
