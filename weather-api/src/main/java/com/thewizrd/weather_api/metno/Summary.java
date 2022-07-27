package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Summary {

    @Json(name = "symbol_code")
    private String symbolCode;

    public void setSymbolCode(String symbolCode) {
        this.symbolCode = symbolCode;
    }

    public String getSymbolCode() {
        return symbolCode;
    }
}