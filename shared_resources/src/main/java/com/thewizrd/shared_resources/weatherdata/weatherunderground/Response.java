package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Response {

    @SerializedName("features")
    private Features features;

    @SerializedName("version")
    private String version;

    @SerializedName("termsofService")
    private String termsofService;

    @SerializedName("error")
    private _Error error;

    public void setFeatures(Features features) {
        this.features = features;
    }

    public Features getFeatures() {
        return features;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setTermsofService(String termsofService) {
        this.termsofService = termsofService;
    }

    public String getTermsofService() {
        return termsofService;
    }

    public _Error getError() {
        return error;
    }

    public void setError(_Error error) {
        this.error = error;
    }
}