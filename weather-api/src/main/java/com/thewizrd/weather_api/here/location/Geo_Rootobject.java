package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Geo_Rootobject {

    @Json(name = "response")
    private Response response;

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}