package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;

public class ResponseItem {

    @Json(name = "instruction")
    private String instruction;

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}