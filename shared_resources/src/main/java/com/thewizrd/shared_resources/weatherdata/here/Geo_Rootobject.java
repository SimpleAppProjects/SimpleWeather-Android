package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

public class Geo_Rootobject {

    @SerializedName("response")
    private Response response;

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}