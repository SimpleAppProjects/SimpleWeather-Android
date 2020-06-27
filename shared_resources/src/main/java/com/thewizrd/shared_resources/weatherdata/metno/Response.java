package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Response {

    @SerializedName("geometry")
    private Geometry geometry;

    @SerializedName("type")
    private String type;

    @SerializedName("properties")
    private Properties properties;

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }
}