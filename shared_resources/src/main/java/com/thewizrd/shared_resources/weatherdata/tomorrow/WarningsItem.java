package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;

public class WarningsItem {

    @SerializedName("code")
    private int code;

    @SerializedName("meta")
    private Meta meta;

    @SerializedName("type")
    private String type;

    @SerializedName("message")
    private String message;

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}