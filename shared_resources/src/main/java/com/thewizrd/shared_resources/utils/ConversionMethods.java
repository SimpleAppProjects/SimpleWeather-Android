package com.thewizrd.shared_resources.utils;

import android.location.Location;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import java.text.DecimalFormat;

public class ConversionMethods {
    // Constants
    private static final double KM_TO_MI = 0.621371192;
    private static final double MI_TO_KM = 1.609344;
    private static final double INHG_TO_MB = 1013.25 / 29.92;
    private static final double MB_TO_INHG = 29.92 / 1013.25;
    private static final double MSEC_TO_MPH = 2.23694;
    private static final double MSEC_TO_KPH = 3.6;
    private static final double MM_TO_IN = 1 / 25.4;
    private static final double IN_TO_MM = 25.4;
    private static final double PA_TO_INHG = 0.0002952998751;
    private static final double PA_TO_MB = 0.01;

    private static final DecimalFormat df;

    static {
        df = new DecimalFormat("#.##");
    }

    public static String mbToInHg(String input) {
        double result = MB_TO_INHG * Double.parseDouble(input);
        return df.format(result);
    }

    public static String inHgToMB(String input) {
        double result = INHG_TO_MB * Double.parseDouble(input);
        return df.format(result);
    }

    public static String paToInHg(String input) {
        double result = PA_TO_INHG * Double.parseDouble(input);
        return df.format(result);
    }

    public static String paToMB(String input) {
        double result = PA_TO_MB * Double.parseDouble(input);
        return df.format(result);
    }

    public static String kmToMi(String input) {
        double result = KM_TO_MI * Double.parseDouble(input);
        return String.format("%d", Math.round(result));
    }

    public static String miToKm(String input) {
        double result = MI_TO_KM * Double.parseDouble(input);
        return String.format("%d", Math.round(result));
    }

    public static String mmToIn(String input) {
        double result = MM_TO_IN * Double.parseDouble(input);
        return String.format("%d", Math.round(result));
    }

    public static String inToMM(String input) {
        double result = IN_TO_MM * Double.parseDouble(input);
        return String.format("%d", Math.round(result));
    }

    public static String mphTokph(String input) {
        double result = MI_TO_KM * Double.parseDouble(input);
        return String.format("%d", Math.round(result));
    }

    public static String kphTomph(String input) {
        double result = KM_TO_MI * Double.parseDouble(input);
        return String.format("%d", Math.round(result));
    }

    public static String msecToMph(String input) {
        double result = MSEC_TO_MPH * Double.parseDouble(input);
        return df.format(Math.round(result));
    }

    public static String msecToKph(String input) {
        double result = MSEC_TO_KPH * Double.parseDouble(input);
        return df.format(Math.round(result));
    }

    public static String FtoC(String input) {
        double result = (Double.parseDouble(input) - 32) * ((double) 5 / 9);
        return String.format("%d", Math.round(result));
    }

    public static String CtoF(String input) {
        double result = (Double.parseDouble(input) * ((double) 9 / 5)) + 32;
        return String.format("%d", Math.round(result));
    }

    public static String KtoC(String input) {
        double result = Double.parseDouble(input) - 273.15;
        return String.format("%d", Math.round(result));
    }

    public static String KtoF(String input) {
        double result = (Double.parseDouble(input) * ((double) 9 / 5)) - 459.67;
        return String.format("%d", Math.round(result));
    }

    public static ZonedDateTime toEpochDateTime(String epoch_time) {
        long epoch = Long.parseLong(epoch_time);
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
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

    public static double calculateGeopositionDistance(Location position1, Location position2) {
        double lat1 = position1.getLatitude();
        double lon1 = position1.getLongitude();
        double lat2 = position2.getLatitude();
        double lon2 = position2.getLongitude();

        /* Returns value in meters */
        return Math.abs(calculateHaversine(lat1, lon1, lat2, lon2));
    }
}