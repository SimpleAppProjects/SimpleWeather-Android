package com.thewizrd.shared_resources.weatherdata.tomorrow;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Meta {

    @SerializedName("timesteps")
    private List<String> timesteps;

    @SerializedName("field")
    private String field;

    @SerializedName("from")
    private String from;

    @SerializedName("to")
    private String to;

    @SerializedName("timestep")
    private String timestep;

    public void setTimesteps(List<String> timesteps) {
        this.timesteps = timesteps;
    }

    public List<String> getTimesteps() {
        return timesteps;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public void setTimestep(String timestep) {
        this.timestep = timestep;
    }

    public String getTimestep() {
        return timestep;
    }
}