package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Summary {

    @SerializedName("symbol_code")
    private String symbolCode;

    public void setSymbolCode(String symbolCode) {
        this.symbolCode = symbolCode;
    }

    public String getSymbolCode() {
        return symbolCode;
    }
}