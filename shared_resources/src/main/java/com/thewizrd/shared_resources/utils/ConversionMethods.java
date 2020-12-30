package com.thewizrd.shared_resources.utils;

import android.location.Location;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ConversionMethods {
    // Constants
    private static final float KM_TO_MI = 0.621371192f;
    private static final float MI_TO_KM = 1.609344f;
    private static final float INHG_TO_MB = 1013.25f / 29.92f;
    private static final float MB_TO_INHG = 29.92f / 1013.25f;
    private static final float MSEC_TO_MPH = 2.23694f;
    private static final float MSEC_TO_KPH = 3.6f;
    private static final float MM_TO_IN = 1 / 25.4f;
    private static final float IN_TO_MM = 25.4f;
    private static final float PA_TO_INHG = 0.0002952998751f;
    private static final float PA_TO_MB = 0.01f;

    public static float mbToInHg(float input) {
        return MB_TO_INHG * input;
    }

    public static float inHgToMB(float input) {
        return INHG_TO_MB * input;
    }

    public static float paToInHg(float input) {
        return PA_TO_INHG * input;
    }

    public static float paToMB(float input) {
        return PA_TO_MB * input;
    }

    public static float kmToMi(float input) {
        return KM_TO_MI * input;
    }

    public static float miToKm(float input) {
        return MI_TO_KM * input;
    }

    public static float mmToIn(float input) {
        return MM_TO_IN * input;
    }

    public static float inToMM(float input) {
        return IN_TO_MM * input;
    }

    public static float mphTokph(float input) {
        return MI_TO_KM * input;
    }

    public static float kphTomph(float input) {
        return KM_TO_MI * input;
    }

    public static float msecToMph(float input) {
        return input * MSEC_TO_MPH;
    }

    public static float msecToKph(float input) {
        return input * MSEC_TO_KPH;
    }

    public static float kphToMsec(float input) {
        return input / MSEC_TO_KPH;
    }

    public static float FtoC(float input) {
        return (input - 32) * (5f / 9);
    }

    public static float CtoF(float input) {
        return (input * (9f / 5)) + 32;
    }

    public static float KtoC(float input) {
        return input - 273.15f;
    }

    public static float KtoF(float input) {
        return (input * (9f / 5)) - 459.67f;
    }

    public static ZonedDateTime toEpochDateTime(long epochSeconds) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    public static double toRadians(double angle) {
        return Math.PI * angle / 180.0;
    }

    public static float toRadians(float angle) {
        return (float) toRadians((double) angle);
    }

    public static double toDegrees(double angle) {
        return 180.0 * angle / Math.PI;
    }

    public static float toDegrees(float angle) {
        return (float) toDegrees((double) angle);
    }

    /**
     * Calculates the distance between two coordinates in meters
     *
     * @param lat1 Latitude of first coordinate
     * @param lon1 Longitude of first coordinate
     * @param lat2 Latitude of second coordinate
     * @param lon2 Longitude of second coordinate
     * @return The distance between the two points in meters
     */
    public static double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6372.8; // In kilometers
        double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);
        lat1 = toRadians(lat1);
        lat2 = toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));

        /* Output in Meters */
        return (R * c) * 1000;
    }

    /**
     * Calculates the distance between two locations in meters
     *
     * @param position1 The first location
     * @param position2 The second location
     * @return The distance between the two locations in meters
     */
    public static double calculateGeopositionDistance(Location position1, Location position2) {
        double lat1 = position1.getLatitude();
        double lon1 = position1.getLongitude();
        double lat2 = position2.getLatitude();
        double lon2 = position2.getLongitude();

        /* Returns value in meters */
        return Math.abs(calculateHaversine(lat1, lon1, lat2, lon2));
    }
}