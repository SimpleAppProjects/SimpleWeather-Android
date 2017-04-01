package com.thewizrd.simpleweather.utils;

public class WeatherException extends Exception {
    private WeatherUtils.ErrorStatus errorStatus;

    public WeatherException(WeatherUtils.ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    @Override
    public String getMessage() {
        String errorMsg;

        switch (errorStatus) {
            case NOWEATHER:
                errorMsg = "Unable to load weather data!!";
                break;
            case NETWORKERROR:
                errorMsg = "Network Connection Error!!";
                break;
            case INVALIDAPIKEY:
                errorMsg = "Invalid API Key";
                break;
            case QUERYNOTFOUND:
                errorMsg = "No cities match your search query";
                break;
            case UNKNOWN:
            default:
                errorMsg = super.getMessage();
                break;
        }

        return errorMsg;
    }
}
