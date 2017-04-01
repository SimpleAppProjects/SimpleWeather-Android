package com.thewizrd.simpleweather.utils;

public class ConversionMethods {
    private static final double KM_TO_MI = 0.621371192;
    private static final double MI_TO_KM = 1.609344;

    public static String mbToInHg(String input) {
        double result = 29.92 * Double.valueOf(input) / 1013.25;
        return String.format("%.2f", result);
    }

    public static String kmToMi(String input) {
        double result = KM_TO_MI * Double.valueOf(input);
        return String.format("%d", Math.round(result));
    }

    public static String miToKm(String input) {
        double result = MI_TO_KM * Double.valueOf(input);
        return String.format("%d", Math.round(result));
    }

    public static String mphTokph(String input) {
        double result = MI_TO_KM * Double.valueOf(input);
        return String.format("%d", Math.round(result));
    }

    public static String kphTomph(String input) {
        double result = KM_TO_MI * Double.valueOf(input);
        return String.format("%d", Math.round(result));
    }

    public static String FtoC(String input) {
        double result = (Double.valueOf(input) - 32) * ((double) 5 / 9);
        return String.format("%d", Math.round(result));
    }

    public static String CtoF(String input) {
        double result = (Double.valueOf(input) * ((double) 9 / 5)) + 32;
        return String.format("%d", Math.round(result));
    }
}