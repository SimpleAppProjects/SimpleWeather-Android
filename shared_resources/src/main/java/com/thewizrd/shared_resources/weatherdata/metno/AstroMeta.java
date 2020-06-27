package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class AstroMeta {

    @SerializedName("licenseurl")
    private String licenseurl;

    public void setLicenseurl(String licenseurl) {
        this.licenseurl = licenseurl;
    }

    public String getLicenseurl() {
        return licenseurl;
    }
}